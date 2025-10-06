# TaskTimer

> **Development Stage:** **Beta** 

TaskTimer is a gamified focus and habit‑tracking app that uses the **Pomodoro technique** and a **reward economy** (XP + coins) to keep users consistent. Users level up, unlock avatar items, and track streaks and stats — all synced with Firebase.

---

##  Core Features (Implemented)
- **Pomodoro Timer:** Custom session and break lengths with session controls.
- **Gamification:** Earn **XP** and **coins** for completed sessions and habits.
- **Avatar Customization + Shop:** Spend coins on hairstyles, clothing, and backgrounds.
- **Progress Tracking:** Session history, streaks, and stats views.
- **Firebase Integration:** Firestore for real‑time data; Firebase Auth for secure login.
- **Notifications:** Session start/break reminders and alerts.

---

##  Current Status
- Core timer, XP/coin rewards, Auth, Firestore, task creation, and basic stats are **working**.
- UI/UX theme work and habit tracking are **in progress**.
- AI suggestion logic is **prototyped** and undergoing testing.

---

##  **This Month’s Work (to prepare for Software Integration user testing next month)**
1. **Finish Habit Tracker MVP**
    - Create/complete habits, streak logic, and daily/weekly summaries.
2. **Economy & Shop Polish**
    - Balance XP/coin rewards, add starter inventory, persist purchases, and guard against duplicate grants.
3. **AI Suggestions (MVP)**
    - Simple rules model for suggesting focus durations and break ratios based on recent history.
4. **Notifications & Scheduling**
    - Reliable daily reminders; do‑not‑disturb windows; edge‑case handling across device restarts.
5. **UI/UX & Accessibility**
    - Apply final color theme, larger‑tap targets, content descriptions, and TalkBack labels.

---

##  **User Testing (Next Month)**
- Goal: Validate session flow, reward clarity, and habit tracker usability.
- Success Criteria: task completion in tests.

---

##  Tech Stack
- **Android (Java + XML)** for UI and app logic.
- **Firebase Auth** and **Firestore** for auth and real‑time data.
- **Android Studio**; **GitHub** with **Gitflow**; **Jira** for planning.

---

##  Setup
1. Clone the repo:
   ```bash
   git clone https://github.com/<your-username>/TaskTimer.git
   ```
2. Open in **Android Studio**.
3. Add your Firebase `google-services.json` (Auth + Firestore enabled).
4. **Gradle Sync** → Build & Run on device/emulator.

---

##  Branching Workflow
| Branch | Purpose |
| --- | --- |
| `main` | Production-ready releases |
| `develop` | Active development |
| `feature/<name>` | Feature work merged into `develop` |

> Uses **Gitflow** (feature branches → `develop` → release → `main`).

---

##  License
MIT
