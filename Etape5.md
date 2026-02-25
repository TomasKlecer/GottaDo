# Stage 5: Routines, Trash UI & Extensibility Hooks

**Goal:** Implement category routines (daily/weekly/monthly/yearly) with WorkManager, full trash screen in the app, and ensure architecture is ready for future extensions (calendar sync, precreated widgets, notifications, etc.) without redesign.

---

## 5.1 Routine model (database already in Stage 1)

**Routine** table (refine if needed):
- `id: Long`, `categoryId: Long`
- `frequency: Enum` — DAILY, WEEKLY, MONTHLY, YEARLY
- **Schedule:** 
  - DAILY: time of day (hour, minute)
  - WEEKLY: day of week + time
  - MONTHLY: day of month (1–31) + time
  - YEARLY: month + day of month + time (or stored as “date”)
- **Visibility:** 
  - Mode: HIDDEN / VISIBLE
  - Range: for “visible”, optional “from” and “to” (e.g. daily: time range; weekly: day range; monthly: day range). Store in a flexible way (e.g. JSON or columns `visibilityFrom`, `visibilityTo` with type-dependent meaning).
- **Incomplete tasks:** 
  - Action: DELETE / MOVE / COMPLETE / UNCOMPLETE / etc. If not DELETE: optional “move to category” (categoryId). Checkbox “delete” vs “complete/uncomplete” + “move to category” dropdown (all categories + “don’t move”).
- **Completed tasks:** 
  - Same structure: DELETE or MOVE or UNCOMPLETE, etc., with optional target category.
- **Unique constraint:** One routine per (categoryId, frequency) or allow multiple per category (e.g. two weekly routines on different days). Spec says “rutiny nad tasky kategorií” — so one or more routines per category; design so each routine has its own schedule and actions.

Routines operate on **categories**, not on widgets. When a routine runs, it affects tasks in that category; widgets that display that category will show updated state after refresh.

---

## 5.2 Routine use cases

- **Crud:** `GetRoutinesForCategoryUseCase`, `SaveRoutineUseCase`, `DeleteRoutineUseCase`.
- **Execution (domain):** `ExecuteRoutineUseCase(routineId)` or `ExecuteRoutinesDueUseCase()`. Logic:
  - Load routine; determine “due” (current time matches schedule).
  - Query tasks in category (incomplete and/or completed according to routine config).
  - Apply actions: delete (move to trash), move to category, set completed/uncompleted.
  - Persist changes; return list of affected widgetIds (all widgets that show this category) for refresh.

WorkManager worker will call this execution use case; it does not contain business rules, only scheduling and invocation.

---

## 5.3 WorkManager: routine worker

- **Unique periodic work:** e.g. `RoutineWorker`, enqueued with `UniqueWorkPolicy.KEEP`, period at least 15 minutes (or as allowed). Worker runs periodically; each run:
  - Query all routines (or routines that are “due” in current window). Due = schedule matches (e.g. daily at 8:00 → run when current time is around 8:00).
  - For each due routine, call `ExecuteRoutineUseCase` (or batch use case that runs all due routines).
  - After execution, collect all affected widgetIds and call `AppWidgetManager.notifyAppWidgetViewDataChanged` for each so widgets refresh.
- **Scheduling nuance:** Exact time (e.g. “every day at 8:00”) can be implemented with a single run at period boundary and “due” check in code, or with OneTimeWorkRequest rescheduling (run at next 8:00, then schedule next 8:00). Prefer simple periodic + due check for Android 7+ compatibility.
- **No business logic in worker:** Only “get due routines → call use case → refresh widgets”.

---

## 5.4 Routine UI in app

- **Entry:** From Category settings (Stage 4), add section “Routines” or link “Manage routines” → list of routines for this category.
- **Routine list:** For a category, show routines (e.g. “Daily 8:00”, “Weekly Monday 9:00”). Add routine → Routine edit screen.
- **Routine edit screen:**
  - Frequency: Daily / Weekly / Monthly / Yearly.
  - Schedule: time picker; for Weekly add day picker; for Monthly add day-of-month; for Yearly add date picker.
  - Visibility: dropdown Hidden / Visible; if Visible, optional “from” and “to” (UI depends on frequency: time range for daily, day range for weekly, etc.).
  - Incomplete tasks: action (Delete / Move / Complete / Uncomplete…) + if Move: category dropdown; checkboxes as per spec (delete vs complete/uncomplete + move).
  - Completed tasks: same structure.
  - Save / Cancel / Delete routine.
- ViewModel uses routine use cases; on save, no need to reschedule WorkManager if worker runs periodically and only checks “due” in code. If you use exact-time scheduling, reschedule on save.

---

## 5.5 Trash screen (app)

- **Spec:** “History of deleted records, grouped by day; restore, clear all, or delete single.”
- **Screen:** New Activity or Fragment: “Trash”. Load via `GetTrashEntriesByDayUseCase` — returns entries grouped by day (e.g. `Map<Day, List<TrashEntry>>` or list of day headers + entries).
- **UI:** List: for each day, header (date); under it, list of trashed tasks (show content, category name, deleted time). Per entry: Restore, Delete permanently. Bottom or menu: “Empty trash” → `ClearTrashUseCase`.
- **Restore:** `RestoreFromTrashUseCase(trashEntryId)` — re-insert task into original category (or default category if category was deleted), remove from trash. Refresh widgets that show that category.
- **Delete permanently:** `DeleteFromTrashUseCase(trashEntryId)` — remove from trash table only.
- **Empty trash:** `ClearTrashUseCase()` — delete all trash entries. Refresh all widgets if needed (optional).

---

## 5.6 Extensions: architecture hooks (no implementation)

Design so the following can be added later without rewriting core:

- **Google Calendar sync:** 
  - New module or package: sync adapter / use case that reads calendar events and creates/updates tasks in selected categories. Categories with type “today” / “tomorrow” / weekday can have sync rules (e.g. “sync with: today”). 
  - Store sync settings (e.g. per category: sync enabled, calendar id, sync interval). Routine-like “sync” schedule (hourly/daily/weekly) or manual “Sync” button. 
  - Domain: `SyncCalendarUseCase`; widget and app only consume task list as today.

- **Precreated widgets:** 
  - Seed data: on first launch or “Add preset”, insert default `WidgetConfig` + default categories + sample tasks. No new features; only default data and maybe a simple “Try these presets” screen.

- **Trash as journal:** 
  - Same trash table; add filtering/sorting and richer day view. No schema change; only UI and optional export.

- **Notifications:** 
  - New use case: “Notify if tasks not completed by X time.” Schedule with WorkManager or AlarmManager; show notification. Config in app (per category or global).

- **Lepší posuvník (ActivateOnTap):** 
  - If widget constraints allow: add setting “Open reorder on edge tap (false = direct drag; true = popup then drag).” Store in WidgetConfig; widget and reorder overlay read it. Omit if not feasible with RemoteViews.

Ensure:
- Category has `categoryType` for “today”, “tomorrow”, “monday”…
- Routine execution and sync are use-case driven so calendar sync can call similar “apply changes to tasks” logic.
- Widget never contains business logic; it only renders state and sends intents.

---

## 5.7 Deliverables

- Routine CRUD in data/domain layer; `ExecuteRoutineUseCase` (or equivalent) with full business rules; Worker that queries due routines, runs use case, refreshes affected widgets.
- Routine UI: list per category, add/edit routine with all spec fields (schedule, visibility, incomplete/completed actions).
- Trash screen: list by day, restore, delete single, empty trash; use cases already in Stage 1, only UI and wiring.
- Documentation or short comments in code where extension points are (e.g. “Calendar sync will implement SyncProvider here”).

After Stage 5, the app is feature-complete per spec: widget, app settings, categories, routines, and trash. Extensions can be added incrementally on top of this architecture.
