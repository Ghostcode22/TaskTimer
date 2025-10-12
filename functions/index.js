const { onCall } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const admin = require("firebase-admin");
const { GoogleGenerativeAI } = require("@google/generative-ai");

const GEMINI_API_KEY = defineSecret("GEMINI_API_KEY");
const MODEL_ID = "gemini-2.5-flash";

if (!admin.apps.length) admin.initializeApp();

function extractText(resp) {
  try {
    if (typeof resp?.text === "function") {
      const t = resp.text();
      if (t && String(t).trim()) return String(t).trim();
    }
    const parts = resp?.candidates?.[0]?.content?.parts;
    if (Array.isArray(parts) && parts.length) {
      return parts.map(p => p.text || "").join(" ").trim();
    }
  } catch (_) {}
  return "";
}

// ---------- small utils ----------
const num = (v, d = 0) => {
  const n = typeof v === "number" ? v : parseInt(String(v || ""), 10);
  return Number.isFinite(n) ? n : d;
};
const str = (v, d = "") => {
  const s = v == null ? "" : String(v);
  return s.trim() ? s.trim() : d;
};
const plural = (n, one, many) => (Math.abs(n) === 1 ? one : many);
const clamp = (s, max = 220) => (s.length > max ? s.slice(0, max - 1) + "…" : s);

// Rule-based fallback if model returns nothing
function formatFallback(event, extra = {}, ctx = {}) {
  if (event === "TASK_COMPLETED") {
    const title = str(extra.taskTitle, "task");
    const left = num(extra.tasksLeft, 0);
    const leftPart =
      left <= 0
        ? "All tasks done—nice finish!"
        : `${left} ${plural(left, "task", "tasks")} left today.`;
    return `Done with "${title}"—great work! ${leftPart}`;
  }

  if (event === "HABIT_COMPLETED") {
    const title = str(extra.habitTitle, "habit");
    const streak = Math.max(1, num(extra.streak, 1));
    const left = num(extra.habitsLeft, 0);
    const streakPart = streak <= 1 ? "Streak started!" : `${streak}-day streak 🔥`;
    const leftPart =
      left <= 0
        ? "All habits for today done."
        : `${left} ${plural(left, "habit", "habits")} left today.`;
    return `Logged "${title}" — ${streakPart}. ${leftPart}`;
  }

  if (event === "HABITS_DUE") {
    const due = num(extra.dueCount, 0);
    return due <= 0
      ? "Nothing due right now—enjoy the win."
      : `${due} ${plural(due, "habit is", "habits are")} scheduled today. You got this 💪`;
  }

  if (event === "TASK_MISSED") {
    const t = str(extra.title, "A task");
    return `Missed "${t}" yesterday—no sweat. Reschedule a tiny step and keep rolling.`;
  }

  return "You’re doing great—keep going!";
}

// Build a strict, compact prompt that *requires* including specifics when provided.
function buildPrompt({ event, question, extra, ctx }) {
  // Provide clean, typed fields the model can trust
  const canonical = {
    event,
    question: str(question, ""),
    extra: {
      taskTitle: str(extra.taskTitle),
      tasksLeft: Number.isFinite(extra.tasksLeft) ? extra.tasksLeft : undefined,
      habitTitle: str(extra.habitTitle),
      habitsLeft: Number.isFinite(extra.habitsLeft) ? extra.habitsLeft : undefined,
      streak: Number.isFinite(extra.streak) ? extra.streak : undefined,
      dueCount: Number.isFinite(extra.dueCount) ? extra.dueCount : undefined,
      title: str(extra.title)
    },
    metrics: {
      xp: num(ctx.xp, 0),
      coins: num(ctx.coins, 0),
      completedTasksRecent: num(ctx.completedTasksRecent, 0),
      habitsCount: num(ctx.habitsCount, 0),
      maxStreak: num(ctx.maxStreak, 0),
      locale: ctx.locale || "en-US",
      timeOfDay: ctx.timeOfDay || "daytime"
    }
  };

  // Role & hard requirements keep outputs grounded and specific
  const instruction =
    event === "TASK_COMPLETED"
      ? "Congratulate the user on the specific task. If tasksLeft is provided, state how many tasks remain (or that all are done). Give an encouraging message based on the exact task that was completed. Be concise and motivating."
      : event === "HABIT_COMPLETED"
      ? "Celebrate the specific habit. If streak is provided, mention it. If habitsLeft is provided, say how many remain (or all done).Give an encouraging message based on the exact habit that was completed. Be concise and motivating."
      : event === "HABITS_DUE"
      ? "Give a friendly nudge. Mention dueCount if provided. Suggest starting with the easiest habit."
      : event === "TASK_MISSED"
      ? "Be empathetic. Mention the missed task title if provided and suggest a tiny recovery action."
      : "Answer briefly about their tasks/habits momentum.";

  return [
    "You are TaskTimer Coach.",
    "STYLE RULES:",
    "- 1–4 short sentences, friendly and specific.",
    "- Use at most one emoji; none is fine.",
    "- If a specific title is provided (taskTitle, habitTitle, or title), mention it exactly once.",
    "- If counts exist (tasksLeft, habitsLeft, dueCount), include them; if they are 0, say all done.",
    "- If streak exists, mention it as “N-day streak”.",
    "- Do not invent numbers or facts. Omit fields that are missing.",
    "",
    `INSTRUCTION: ${instruction}`,
    `CONTEXT_JSON: ${JSON.stringify(canonical)}`
  ].join("\n");
}

exports.coachMessage = onCall({ secrets: [GEMINI_API_KEY], cors: true }, async (req) => {
  if (!req.auth || !req.auth.uid) throw new Error("Unauthenticated");

  const uid = req.auth.uid;
  const { event = "ASK", question = "", extra = {} } = req.data || {};

  const db = admin.firestore();
  const userRef = db.collection("users").doc(uid);

  const [userDoc, tasksDoneSnap, habitsSnap] = await Promise.all([
    userRef.get(),
    userRef.collection("tasks").where("done", "==", true).limit(50).get(),
    userRef.collection("habits").get(),
  ]);

  const user = userDoc.exists ? userDoc.data() : {};
  const streaks = habitsSnap.docs.map(d => d.get("streak") || 0);
  const maxStreak = streaks.length ? Math.max(...streaks) : 0;

  const ctx = {
    xp: user.xp || 0,
    coins: user.coins || 0,
    completedTasksRecent: tasksDoneSnap.size,
    habitsCount: habitsSnap.size,
    maxStreak,
    locale: (user.locale || "en-US"),
    timeOfDay: (() => {
      const h = new Date().getHours();
      if (h < 12) return "morning";
      if (h < 17) return "afternoon";
      return "evening";
    })()
  };

  const prompt = buildPrompt({ event, question, extra, ctx });

  let text = "";
  try {
    const genAI = new GoogleGenerativeAI(GEMINI_API_KEY.value());
    const model = genAI.getGenerativeModel({ model: MODEL_ID });
    const result = await model.generateContent(prompt);
    text = extractText(result?.response) || "";
    if (result?.response?.promptFeedback?.blockReason) {
      console.log("coachMessage blocked:", result.response.promptFeedback.blockReason);
    }
  } catch (e) {
    console.error("Gemini error", e);
  }

  if (!text) {
    // Guaranteed meaningful result (no hallucinated counts)
    text = formatFallback(event, extra, ctx);
    console.log("coachMessage using server fallback for event=", event);
  }

  // Final polish: collapse newlines and clamp length
  text = clamp(String(text).replace(/\s+/g, " ").trim(), 220);

  console.log("coachMessage model=", MODEL_ID, "event=", event, "len=", (text || "").length);
  return { message: text };
});
