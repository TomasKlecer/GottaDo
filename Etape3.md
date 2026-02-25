# Stage 3: Widget Interactions & Record Editing

**Goal:** Add all widget interactions (click to edit, click category to add record, long-press to toggle completed, edge reorder popup, overlay reorder) and the full record edit UI. All actions go through use cases; widget only dispatches intents.

---

## 3.1 Click on record → Edit

- **Widget:** Set `PendingIntent` on record row. Intent must identify: `widgetId`, `taskId`, and action type (e.g. `ACTION_EDIT_TASK`). Target: an **Activity** (or Fragment host) that shows the record edit screen.
- **Flow:** Activity is started from widget; it loads task via `GetTaskUseCase`, shows edit UI; on Save → `SaveTaskUseCase`; on Delete → `DeleteTaskUseCase` (moves to trash); on Cancel → finish. Then trigger widget refresh for `widgetId`.
- **Record edit UI (same for “add” and “edit”):**
  - Text: rich text (highlight, color, italic per span). Store as HTML string; use EditText with Spanned or WebView/Editor that produces HTML.
  - Completed: checkbox.
  - Bullet color: color picker / preset chips.
  - Time: optional time picker; if set, task is ordered by time in category.
  - Category: dropdown to move task to another category (call `MoveTaskToCategoryUseCase` on save if changed).
  - Buttons: Save, Cancel, Delete. For “add new record” (opened from category click): no Cancel (or hide it), only Save and Delete.
- **Architecture:** One Activity (e.g. `RecordEditActivity`) with `taskId: Long?` and `categoryId: Long` (for new task). If `taskId == null`, create new task in given category on Save; otherwise update. ViewModel calls use cases; Activity observes and refreshes widget when done. Structure the form so that visibility of fields (rich text, time, category, bullet color, etc.) can be driven by app preferences in Stage 4 — e.g. a single “record edit options” model loaded at startup that shows/hides sections.

---

## 3.2 Click on category row → Add record

- **Widget:** PendingIntent on category row with `ACTION_ADD_TASK`, `widgetId`, `categoryId`. Same Activity as above: start with `taskId = null`, `categoryId = x`. Activity creates empty task (or no task until Save) and shows edit UI without Cancel button, only Save and Delete.
- **Implementation detail:** Either create a draft task in DB on category click and open edit with that taskId, or create task only on Save from edit screen (preferred: create on Save so “Cancel” is not needed — user just closes; if you create draft, Delete in edit would remove it).

---

## 3.3 Long-press on record → Toggle completed

- **Widget:** Long-click PendingIntent on record row. Intent targets a **BroadcastReceiver** or a small **Service** that: receives `taskId`, calls `ToggleTaskCompletedUseCase(taskId)`, then triggers widget refresh for the widget that sent the intent (pass `widgetId` in intent).
- No UI; just flip `completed` and refresh widget. Visual: completed = strikethrough + gray (already in Stage 2 layout).

---

## 3.4 Edge tap → Reorder popup (and reorder handle position)

- **Widget config:** `reorderHandlePosition` = LEFT, RIGHT, or NONE. If NONE, no edge tap; reorder is only via the button at the bottom of the list (when user scrolls down).
- **Edge tap:** When user taps left or right edge (narrow strip), open a **popup/overlay** that allows reordering. Popup can be a transparent Activity or a small overlay window. Content: same list of categories + tasks, but in “reorder mode” — e.g. tap up/down to swap, or simulated drag (see 3.5). Dismiss on tap outside.
- **Intent:** Widget sets PendingIntent for the edge view (or for a dedicated “reorder” area). Intent carries `widgetId`. Overlay reads widget state from `GetWidgetStateUseCase(widgetId)` and shows reorder UI. After each move, call `ReorderTasksUseCase` / move-between-categories use case and refresh widget.

---

## 3.5 Simulated drag & drop (RemoteViews limitation)

- **Spec:** “Simulate moving tasks via overlay activity that visually represents drag and then updates order in database.”
- **Overlay Activity:** Full-screen or large overlay showing the same list (categories + tasks). User “drags” a row (e.g. tap row then tap target position, or up/down buttons). On confirm: compute new order and possibly new category; call use cases to move task and update sortOrder; then refresh widget and finish.
- **Flow:** Either the “edge popup” is this overlay, or edge opens a small popup that launches this overlay. One reorder UI that: (1) displays current order, (2) lets user choose a task and new position (and optionally new category), (3) applies `ReorderTasksUseCase` / `MoveTaskToCategoryUseCase`, (4) refreshes widget.

---

## 3.6 Bottom buttons (scroll to end)

- **Open app:** Last item(s) in the list (or footer) — “Open app” button. PendingIntent to launch main Activity of the app (e.g. widget list / settings).
- **Reorder (if reorder handle = NONE):** Another button at bottom that opens the same reorder overlay as in 3.4/3.5, so user can still reorder without edge tap.
- These buttons are only visible when user scrolls to the end (they are part of the scrollable list from Stage 2).

---

## 3.7 Delete → Trash and Redo

- **Delete in edit:** Calls `DeleteTaskUseCase` (task removed from category and inserted into TrashEntry). Show a transient “Undo” (e.g. Snackbar) that calls restore use case and re-inserts task, then refreshes widget. If no Undo, task stays in trash (restorable from app trash screen in Stage 5).

---

## 3.8 Use cases to add (if not in Stage 1)

- `ReorderTasksUseCase` — reorder within category or move task to another category and set order.
- Ensure `SaveTaskUseCase` handles: create new task in category, update existing, and optional category change (move).

---

## 3.9 Deliverables

- Record edit Activity + ViewModel: full form (text/HTML, completed, bullet color, time, category dropdown), Save/Cancel/Delete, launched from widget with taskId or categoryId.
- Widget: PendingIntents on record (edit), category (add), record long-press (toggle completed), edge (reorder popup), bottom “Open app” and “Reorder” buttons.
- Reorder overlay Activity: list of categories/tasks, simple “move” interaction (tap to select, tap target to place), persist via use cases, refresh widget.
- Delete with optional in-screen Undo (restore from trash).
- All actions go through domain use cases; widget and activities only dispatch and refresh.

After Stage 3, the widget is fully interactive and record editing is complete from the widget side. App-side settings (which categories, font sizes, etc.) are Stage 4.
