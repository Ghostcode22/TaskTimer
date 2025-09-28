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
    event,
    extra,
  };

  const instruction =
    event === "TASK_COMPLETED" ? "Congratulate concisely; give an motivational quote."
    : event === "HABIT_COMPLETED" ? "Celebrate streak; reinforce consistency; give an motivational quote."
    : event === "HABITS_DUE" ? "Nudge positively; propose the easiest habit to start now."
    : event === "TASK_MISSED" ? "Empathetic reset; propose a tiny recovery action."
    : "Answer the user's question about their habits/tasks and momentum, concisely.";

    const prompt = [
      "You are TaskTimer Coach. Be positive, concise (1–2 short sentences). Avoid emojis unless helpful.",
      `Context: ${JSON.stringify(ctx)}`,
      `Instruction: ${instruction}`,
      `UserQuestion: ${question || "(none)"}`
    ].join("\n");

    let text = "";
    try {
      const genAI = new GoogleGenerativeAI(GEMINI_API_KEY.value());
      const model = genAI.getGenerativeModel({ model: MODEL_ID });

      const result = await model.generateContent(prompt);
      const resp = result?.response;

      if (resp?.promptFeedback?.blockReason) {
        console.log("coachMessage blocked:", resp.promptFeedback.blockReason);
      }

      if (resp && typeof resp.text === "function") {
        text = (resp.text() || "").trim();
      }

      if (!text) {
        const parts = resp?.candidates?.[0]?.content?.parts;
        if (Array.isArray(parts) && parts.length) {
          text = parts.map(p => p.text || "").join(" ").trim();
        }
      }

      console.log("coachMessage model=", MODEL_ID, "event=", event, "len=", (text || "").length);
    } catch (e) {
      console.error("Gemini error", e);
    }

    if (!text) {
      const serverFallback = {
        TASK_COMPLETED: "Nice work—stack one tiny task while the engine’s warm.",
        HABIT_COMPLETED: `Consistency wins—streak at ${maxStreak || 1}! Keep it light and repeat.`,
        HABITS_DUE: "A few habits are due—start with the easiest 2-minute one.",
        TASK_MISSED: "No sweat—do a 2-minute version now to reset the streak.",
        ASK: "I’m here—ask me about your tasks, habits, or how to regain momentum."
      }[event] || "You’ve got this.";
      console.log("coachMessage using server fallback for event=", event);
      text = serverFallback;
    }

    return { message: text };
});
