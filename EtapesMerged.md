# GottaDo — Development Stages (Merged)

All five architectural stages in one document. Stages are ordered so each builds on the previous; implement in sequence (Etape1 → Etape5).

---

# Stage 1: Data Layer & Domain Foundation

**Goal:** Establish Room database, domain models, repositories, and core use cases. No UI, no widget. Everything else will build on this.

---

## 1.1 Database schema (Room)

**WidgetConfig**
- `widgetId: Int` (primary key) — AppWidgetManager widget ID
- `title: String?` — widget title (empty = hidden)
- `subtitle: String?` — widget subtitle (empty = hidden)
- `note: String?` — internal note, not shown on widget
- `backgroundColor: Int` — ARGB
- `backgroundAlpha: Float` — 0..1
- `categoryFontSizeSp: Float`
- `recordFontSizeSp: Float`
- `defaultTextColor: Int` — ARGB
- `reorderHandlePosition: Enum` — LEFT, RIGHT, NONE
- *(Reserve columns or use JSON/type converter for future: style preset, custom background, font — so extensions can add without schema migration)*

**Category**
- `id: Long` (primary key, autoGenerate)
- `name: String`
- `sortOrder: Int` — global order of categories
- `showCheckboxInsteadOfBullet: Boolean`
- `tasksWithTimeFirst: Boolean` — time-based tasks before or after tasks without time
- `categoryType: String` — e.g. "normal", "today", "tomorrow", "monday"… for future special types and calendar sync; for now only "normal" is used

**WidgetCategoryJoin** (many-to-many: widget ↔ categories)
- `widgetId: Int`
- `categoryId: Long`
- `sortOrder: Int` — order of this category within this widget
- `visible: Boolean` — whether category is shown in this widget
- Primary key: (widgetId, categoryId)

**Task** (the "record" in the spec)
- `id: Long` (primary key, autoGenerate)
- `categoryId: Long` (foreign key)
- `contentHtml: String` — formatted text as HTML
- `completed: Boolean`
- `bulletColor: Int` — ARGB
- `scheduledTimeMillis: Long?` — null = no time; used for ordering within category
- `sortOrder: Int` — order within category (for same time or no-time)
- `createdAtMillis: Long`
- `updatedAtMillis: Long`

**Routine** (table only; behaviour in Stage 5)
- `id: Long`, `categoryId: Long`, `frequency: Enum` (DAILY, WEEKLY, MONTHLY, YEARLY)
- Schedule: `scheduleTimeHour`, `scheduleTimeMinute`, `scheduleDayOfWeek` (weekly), `scheduleDayOfMonth` (monthly), `scheduleMonth`/`scheduleDay` (yearly) — use nulls where not applicable
- Visibility: `visibilityMode` (HIDDEN/VISIBLE), `visibilityFrom`, `visibilityTo` (flexible: time/day/date depending on frequency; JSON or typed columns)
- Incomplete tasks: `incompleteAction` (DELETE/MOVE/COMPLETE/UNCOMPLETE), `incompleteMoveToCategoryId: Long?`
- Completed tasks: `completedAction`, `completedMoveToCategoryId: Long?`
- Design so multiple routines per category are allowed (e.g. different weekly days). No schema change needed in Stage 5.

**TrashEntry** (soft-delete / restore)
- `id: Long`, `originalCategoryId: Long` (for restore), same payload as Task (contentHtml, completed, bulletColor, scheduledTimeMillis, sortOrder, etc.), `deletedAtMillis: Long`, `categoryName: String` (denormalized for display). Structure so trash can list by day and restore single or clear all.

Use appropriate indexes (widgetId, categoryId, categoryId+sortOrder, etc.) and foreign keys. Use TypeConverters for enums and optional JSON for extensible widget/category options.

---

## 1.2 Domain layer

**Models (domain):** Mirror entities where needed but use clear domain names: e.g. `WidgetConfig`, `Category`, `Task`, `Routine`, `TrashEntry`. No Room annotations in domain models if you keep them separate; otherwise use the same data classes and keep dependency direction: UI → ViewModel → UseCase → Repository → Data Source (Room).

**Repositories (interfaces in domain, implementations in data):**
- `WidgetConfigRepository` — get by widgetId, getAll, insert, update, delete
- `CategoryRepository` — CRUD, list all, get categories for widget (via join)
- `WidgetCategoryRepository` or combined — set category order/visibility per widget, add/remove category from widget
- `TaskRepository` — CRUD, list by category (ordered by time then sortOrder), reorder (update sortOrder/scheduledTimeMillis), move to category
- `RoutineRepository` — CRUD (used from Stage 5)
- `TrashRepository` — add to trash (delete from Task, insert into TrashEntry), list by day, restore, delete single, clear all

**Use cases (domain):**
- Widget: `GetWidgetStateUseCase(widgetId)` — returns all data needed to render one widget: config + ordered list of "category blocks". Each block: category id, name, display options (`showCheckboxInsteadOfBullet`, `tasksWithTimeFirst`), and ordered list of tasks (id, contentHtml, completed, bulletColor, scheduledTimeMillis). Ordering: by widget's category order/visibility; within category by `tasksWithTimeFirst` then time/sortOrder. No business logic in widget; this use case computes ordering and filters.
- Task: `GetTaskUseCase`, `SaveTaskUseCase`, `DeleteTaskUseCase` (move to trash), `ToggleTaskCompletedUseCase`, `ReorderTasksUseCase`, `MoveTaskToCategoryUseCase`
- Category: `GetCategoriesUseCase`, `GetCategoriesForWidgetUseCase`, `SaveCategoryUseCase`, etc.
- Widget config: `GetWidgetConfigUseCase`, `SaveWidgetConfigUseCase`, `GetAllWidgetConfigsUseCase` (for app widget list in Stage 4)
- Trash: `GetTrashEntriesByDayUseCase`, `RestoreFromTrashUseCase`, `DeleteFromTrashUseCase`, `ClearTrashUseCase`

Every user action must go through a use case. Repositories only do persistence; use cases orchestrate (e.g. delete task = delete from tasks + insert into trash).

---

## 1.3 Dependency direction & DI

- **Data:** Room Database, DAOs, Repository implementations. Single source of truth is Room.
- **Domain:** Use case classes, repository interfaces (if split module) or at least clear boundaries.
- **App/UI:** Will depend on domain/data. Use dependency injection (Hilt recommended) so ViewModels get UseCases and widget gets a way to obtain widget state via UseCase (no logic in widget).

Do not implement routines logic or WorkManager in this stage; only the Routine entity/table and repository contract so Stage 5 can add behaviour without touching schema.

---

## 1.4 Deliverables (no UI)

- Room database with migrations (version 1).
- All entities, DAOs, repository implementations.
- Domain use cases listed above (routine execution use case deferred to Stage 5).
- DI setup so that UseCases can be resolved by widget and app.
- Optional: minimal unit tests for repositories or use cases (no UI tests yet).

After Stage 1, any new feature (widget, app screens, routines) only consumes use cases and repository interfaces.

---

# Stage 2: Widget — Display & Basic Structure

**Goal:** Implement the widget as a semi-transparent, scrollable list that shows categories and records. No interactions yet (no click-to-edit, no long-press, no reorder). Widget only renders state provided by a use case; no business logic inside the widget.

---

## 2.1 Widget state (from Stage 1)

**GetWidgetStateUseCase(widgetId)** (defined in Stage 1) must return a single DTO containing:
- Widget config: colors, alpha, font sizes, default text color, reorder handle position.
- Ordered list of "category blocks": for each category visible in this widget, category id, name, and display options (`showCheckboxInsteadOfBullet`, `tasksWithTimeFirst`), plus ordered list of tasks (id, contentHtml, completed, bulletColor, scheduledTimeMillis). Order: by widget's category order; within category by `tasksWithTimeFirst` then time/sortOrder.

Widget process: call this use case (e.g. from a service or via AppWidgetManager update), get state, pass to rendering. No logic in widget beyond "take state → build RemoteViews".

---

## 2.2 Widget layout (RemoteViews)

- **Root:** Semi-transparent background (use config background color + alpha). Shape or 9-patch if needed for "square" look.
- **Scrollable list:** Use `ListView` (or `RecyclerView` via AppWidgetHostView if not available in RemoteViews — on Android 7+ we use standard widget APIs). RemoteViews typically use `ListView` with `RemoteViewsService` + `RemoteViewsFactory`.
- **Item types:**
  - **Category row:** Single layout — bold, slightly larger font; text = category name.
  - **Record row:** Per-category display option: show either a **bullet** (colored dot) or a **checkbox** (from `showCheckboxInsteadOfBullet` in widget state). Text can be multi-line. When `completed == true`, render as strikethrough + gray; otherwise default text color from config.
- **Font sizes:** From widget config (category font size, record font size). Apply to the corresponding item layouts.
- **Scrolling:** List must scroll so that many categories/records are accessible. Bottom area (buttons: "Open app", "Reorder") is part of the same scroll content so they appear only when user scrolls to the end — implemented as last items in the list or a footer in the same scroll.

Do not add click listeners or reorder UI in this stage; only visual layout and data binding.

---

## 2.3 RemoteViewsService + RemoteViewsFactory

- **Service:** Registered in manifest; returns a `RemoteViewsFactory`.
- **Factory:** Given `widgetId` (and optionally `intent` extras), obtain `GetWidgetStateUseCase` (via DI or application context), call `GetWidgetStateUseCase(widgetId)`, get the list of "row" items (flattened: category row, record row, category row, … plus footer rows for "Open app" / "Reorder" if reorder not disabled).
- Map state to list items: each item is either category or record or footer. Use different `layoutId` per type so the factory returns the appropriate RemoteViews for each position.
- Apply colors/font sizes from config to each row. For HTML content, RemoteViews do not support Spanned; store and display plain text in the widget list (or a stripped version); full HTML is for the edit UI. So either: show plain text in widget and keep HTML for edit, or use a single-line/simplified representation in widget (spec: "řádek může zobrazovat i víceřádkový text" — use setCharSequence and allow multi-line in the layout if RemoteViews support it).

---

## 2.4 Widget update trigger

- **AppWidgetProvider:** On `onUpdate`, for each widgetId ensure a default `WidgetConfig` exists (insert if not present), so `GetWidgetStateUseCase(widgetId)` always has data and the app's widget list (Stage 4) can show all instances. Then request widget state from use case and refresh the list (set adapter intent, set background, etc.). On `onEnabled`/`onDisabled` handle first/last widget instance if needed.
- **Refresh:** Whenever data can change (task/category/config), call `AppWidgetManager.notifyAppWidgetViewDataChanged(widgetId, viewId)` and/or update the widget. Data changes will be triggered from app or from future stages (edit, reorder, routines); for this stage, a manual refresh or one-time load is enough.

---

## 2.5 Multi-instance support

- Each `widgetId` is passed to `GetWidgetStateUseCase`. Config and visible categories are stored per `widgetId` (Stage 1). Ensure the factory and provider use the correct `widgetId` for each instance.

---

## 2.6 Deliverables

- Widget layout(s): root (background + list), item layouts for category row, record row, and footer row(s) for "Open app" / "Reorder".
- `RemoteViewsService` + `RemoteViewsFactory` that use `GetWidgetStateUseCase(widgetId)` and map result to list items.
- AppWidgetProvider that builds RemoteViews (background, list with service intent) and triggers updates.
- No click/long-press/reorder behaviour yet — Stage 3 will add those.

After Stage 2, the widget is visible and shows live data from the database for each instance, with correct styling and scrolling.

---

# Stage 3: Widget Interactions & Record Editing

**Goal:** Add all widget interactions (click to edit, click category to add record, long-press to toggle completed, edge reorder popup, overlay reorder) and the full record edit UI. All actions go through use cases; widget only dispatches intents.

---

## 3.1 Click on record → Edit

- **Widget:** Set `PendingIntent` on record row. Intent must identify: `widgetId`, `taskId`, and action type (e.g. `ACTION_EDIT_TASK`). Target: an **Activity** (or Fragment host) that shows the record edit screen.
- **Flow:** Activity is started from widget; it loads task via `GetTaskUseCase`, shows edit UI; on Save → `SaveTaskUseCase`; on Delete → `DeleteTaskUseCase` (moves to trash); on Cancel → finish. Then trigger widget refresh for `widgetId`.
- **Record edit UI (same for "add" and "edit"):**
  - Text: rich text (highlight, color, italic per span). Store as HTML string; use EditText with Spanned or WebView/Editor that produces HTML.
  - Completed: checkbox.
  - Bullet color: color picker / preset chips.
  - Time: optional time picker; if set, task is ordered by time in category.
  - Category: dropdown to move task to another category (call `MoveTaskToCategoryUseCase` on save if changed).
  - Buttons: Save, Cancel, Delete. For "add new record" (opened from category click): no Cancel (or hide it), only Save and Delete.
- **Architecture:** One Activity (e.g. `RecordEditActivity`) with `taskId: Long?` and `categoryId: Long` (for new task). If `taskId == null`, create new task in given category on Save; otherwise update. ViewModel calls use cases; Activity observes and refreshes widget when done. Structure the form so that visibility of fields (rich text, time, category, bullet color, etc.) can be driven by app preferences in Stage 4 — e.g. a single "record edit options" model loaded at startup that shows/hides sections.

---

## 3.2 Click on category row → Add record

- **Widget:** PendingIntent on category row with `ACTION_ADD_TASK`, `widgetId`, `categoryId`. Same Activity as above: start with `taskId = null`, `categoryId = x`. Activity creates empty task (or no task until Save) and shows edit UI without Cancel button, only Save and Delete.
- **Implementation detail:** Either create a draft task in DB on category click and open edit with that taskId, or create task only on Save from edit screen (preferred: create on Save so "Cancel" is not needed — user just closes; if you create draft, Delete in edit would remove it).

---

## 3.3 Long-press on record → Toggle completed

- **Widget:** Long-click PendingIntent on record row. Intent targets a **BroadcastReceiver** or a small **Service** that: receives `taskId`, calls `ToggleTaskCompletedUseCase(taskId)`, then triggers widget refresh for the widget that sent the intent (pass `widgetId` in intent).
- No UI; just flip `completed` and refresh widget. Visual: completed = strikethrough + gray (already in Stage 2 layout).

---

## 3.4 Edge tap → Reorder popup (and reorder handle position)

- **Widget config:** `reorderHandlePosition` = LEFT, RIGHT, or NONE. If NONE, no edge tap; reorder is only via the button at the bottom of the list (when user scrolls down).
- **Edge tap:** When user taps left or right edge (narrow strip), open a **popup/overlay** that allows reordering. Popup can be a transparent Activity or a small overlay window. Content: same list of categories + tasks, but in "reorder mode" — e.g. tap up/down to swap, or simulated drag (see 3.5). Dismiss on tap outside.
- **Intent:** Widget sets PendingIntent for the edge view (or for a dedicated "reorder" area). Intent carries `widgetId`. Overlay reads widget state from `GetWidgetStateUseCase(widgetId)` and shows reorder UI. After each move, call `ReorderTasksUseCase` / move-between-categories use case and refresh widget.

---

## 3.5 Simulated drag & drop (RemoteViews limitation)

- **Spec:** "Simulate moving tasks via overlay activity that visually represents drag and then updates order in database."
- **Overlay Activity:** Full-screen or large overlay showing the same list (categories + tasks). User "drags" a row (e.g. tap row then tap target position, or up/down buttons). On confirm: compute new order and possibly new category; call use cases to move task and update sortOrder; then refresh widget and finish.
- **Flow:** Either the "edge popup" is this overlay, or edge opens a small popup that launches this overlay. One reorder UI that: (1) displays current order, (2) lets user choose a task and new position (and optionally new category), (3) applies `ReorderTasksUseCase` / `MoveTaskToCategoryUseCase`, (4) refreshes widget.

---

## 3.6 Bottom buttons (scroll to end)

- **Open app:** Last item(s) in the list (or footer) — "Open app" button. PendingIntent to launch main Activity of the app (e.g. widget list / settings).
- **Reorder (if reorder handle = NONE):** Another button at bottom that opens the same reorder overlay as in 3.4/3.5, so user can still reorder without edge tap.
- These buttons are only visible when user scrolls to the end (they are part of the scrollable list from Stage 2).

---

## 3.7 Delete → Trash and Redo

- **Delete in edit:** Calls `DeleteTaskUseCase` (task removed from category and inserted into TrashEntry). Show a transient "Undo" (e.g. Snackbar) that calls restore use case and re-inserts task, then refreshes widget. If no Undo, task stays in trash (restorable from app trash screen in Stage 5).

---

## 3.8 Use cases to add (if not in Stage 1)

- `ReorderTasksUseCase` — reorder within category or move task to another category and set order.
- Ensure `SaveTaskUseCase` handles: create new task in category, update existing, and optional category change (move).

---

## 3.9 Deliverables

- Record edit Activity + ViewModel: full form (text/HTML, completed, bullet color, time, category dropdown), Save/Cancel/Delete, launched from widget with taskId or categoryId.
- Widget: PendingIntents on record (edit), category (add), record long-press (toggle completed), edge (reorder popup), bottom "Open app" and "Reorder" buttons.
- Reorder overlay Activity: list of categories/tasks, simple "move" interaction (tap to select, tap target to place), persist via use cases, refresh widget.
- Delete with optional in-screen Undo (restore from trash).
- All actions go through domain use cases; widget and activities only dispatch and refresh.

After Stage 3, the widget is fully interactive and record editing is complete from the widget side. App-side settings (which categories, font sizes, etc.) are Stage 4.

---

# Stage 4: App — Widget & Category Management

**Goal:** The app is the "settings" surface: manage multiple widget instances, configure each widget (title, appearance, category order, reorder handle), manage categories (shared across widgets), and prepare extensibility for styles/backgrounds/fonts. No routines or trash UI yet.

---

## 4.1 App entry and widget list

- **Main screen:** List of configured widget instances (widget IDs that have a row in `WidgetConfig`). Option to "Add widget" — actually means "configure a new widget"; user adds the widget from the system launcher, then opens app to configure it. So app shows: all `widgetId`s that exist in `WidgetConfig`, with title/subtitle or "Widget 1", "Widget 2" for empty titles. Tapping a row opens that widget's settings.
- **When user adds widget from launcher:** Default `WidgetConfig` for new `widgetId` is created in Stage 2 (AppWidgetProvider inserts if not present on update). App list here simply uses `GetAllWidgetConfigsUseCase`; every configured widget from Stage 2 will appear.
- **Architecture:** ViewModel uses `GetAllWidgetConfigsUseCase` (or iterate widget IDs from config repo). List adapter → open WidgetSettingsActivity(widgetId).

---

## 4.2 Widget settings screen (per widget)

**Data:** One `WidgetConfig` per `widgetId`. Load via `GetWidgetConfigUseCase(widgetId)`; save via `SaveWidgetConfigUseCase`.

**Fields (from spec):**
- **Title** — optional; when empty, hidden on widget.
- **Subtitle** — optional; when empty, hidden on widget.
- **Note** — internal only, not shown on widget.
- **Background:** color picker (e.g. color wheel/palette) + alpha slider. Persist as `backgroundColor` + `backgroundAlpha`.
- **Category font size** (sp).
- **Record font size** (sp).
- **Default text color** (for records).
- **Category order and visibility:** List of categories that can appear on this widget. For each: category name, sort order in widget, visibility toggle (show/hide in this widget). Reorder list (drag or up/down). Data: `WidgetCategoryJoin` rows; use `GetCategoriesForWidgetUseCase`, update via repository (set order, visible).
- **Reorder handle:** Dropdown — Left, Right, None.

**Extensibility (NOTE from spec):** Program so it's easy to add later:
- Style presets (e.g. "notebook" look).
- Custom widget background image.
- Custom font.

Approach: keep `WidgetConfig` with a reserved "options" or JSON field for future key-value or structured options; or add columns in a later migration. UI: single "Widget settings" screen with sections; new sections can be added without breaking existing ones.

---

## 4.3 Categories: global and per-widget

- **Categories are global:** One `Category` table; many widgets can reference the same category via `WidgetCategoryJoin`. Creating a category in the app makes it available to any widget.
- **Category list screen:** Accessible from app (e.g. "Manage categories" in drawer or main screen). Lists all categories; add, edit name, delete (with care: if in use by widgets, either prevent delete or cascade/hide).
- **Per-category settings (spec):**
  - Category name.
  - **Checkbox vs bullet:** "Show checkbox instead of bullet" for records in this category.
  - **Tasks with time first:** Yes = tasks with `scheduledTimeMillis` before tasks without; No = opposite.
- **Per-widget category assignment:** In widget settings (4.2), user picks which categories appear in that widget and in what order; that's the `WidgetCategoryJoin` (sortOrder, visible). So "add category" in app = new row in `Category`; "add category to widget" = new row in `WidgetCategoryJoin` linking widgetId to categoryId.

---

## 4.4 Category settings screen (per category)

- **Entry:** From "Manage categories" list, tap a category → CategorySettingsActivity(categoryId).
- **Fields:**
  - Name.
  - Checkbox: "Show checkbox instead of bullet" (store in `Category.showCheckboxInsteadOfBullet`).
  - Checkbox: "Tasks with time first" (store in `Category.tasksWithTimeFirst`).
- Save → `SaveCategoryUseCase`. After save, refresh all widgets that use this category (notify by widgetId list from join table).

---

## 4.5 Record (task) display options (spec: "Nastavení záznamů")

- **Spec:** "Allow enabling what is shown in record edit — e.g. plain text only, no formatting, only completed checkbox."
- **Placement:** Can live in app settings (global) or per-widget. Easiest: global "Record edit options" in app (e.g. in Settings or a dedicated screen): toggles for "Allow rich text", "Show time field", "Show category dropdown", "Show bullet color", etc. Store in SharedPreferences or a simple `AppPreferences` table. Record edit Activity (Stage 3) reads these and shows/hides fields accordingly.
- **Implementation:** Define a small model `RecordEditOptions` (booleans) and a use case or repo to load/save it. RecordEditActivity uses it to build the form. No new UI in widget; only app settings screen + wiring in edit screen.

---

## 4.6 Navigation and flow

- **Main:** List of widgets → Widget settings (4.2).
- **Widget settings:** Sections for appearance, category order/visibility, reorder handle; link to "Manage categories" (list of all categories). From category list → Category settings (4.4).
- **Optional:** Drawer or bottom nav for "Widgets", "Categories", "Settings" (where "Record edit options" lives).

---

## 4.7 Widget refresh from app

- After any change that affects a widget (config or category used by that widget), call `AppWidgetManager.notifyAppWidgetViewDataChanged(widgetId, viewId)` and/or full update so the widget re-fetches state via `GetWidgetStateUseCase`.

---

## 4.8 Special category types (prepare only)

- **Spec:** "Special categories: today, tomorrow, sunday… sync with calendar later."
- **This stage:** Add `categoryType` to Category (already in Stage 1). In UI, allow selecting type "Normal", "Today", "Tomorrow", "Monday"… Store as enum or string. **Behaviour:** Treat all as "normal" for now (no auto-move, no calendar). In Stage 5 or a future extension, routines and calendar sync will use `categoryType`. So: dropdown in category settings for "Type" with values that you will implement later; no logic change in task ordering or widget display beyond what's already there.

---

## 4.9 Deliverables

- Main Activity: list of widget instances; tap → Widget settings.
- Widget settings Activity: all config fields; category order/visibility for this widget; reorder handle dropdown; extensible structure for future style/background/font.
- Category list: all categories; add; tap → Category settings.
- Category settings Activity: name, checkbox vs bullet, tasks-with-time-first.
- Record edit options screen (global): toggles for which fields appear in record edit; persistence and use in RecordEditActivity.
- Ensure new widget from launcher gets default WidgetConfig and appears in app list.

After Stage 4, the app fully controls widget appearance and category assignment; categories are shared and configurable; record edit can be simplified via options. Stage 5 adds routines and trash UI.

---

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
  - YEARLY: month + day of month + time (or stored as "date")
- **Visibility:** 
  - Mode: HIDDEN / VISIBLE
  - Range: for "visible", optional "from" and "to" (e.g. daily: time range; weekly: day range; monthly: day range). Store in a flexible way (e.g. JSON or columns `visibilityFrom`, `visibilityTo` with type-dependent meaning).
- **Incomplete tasks:** 
  - Action: DELETE / MOVE / COMPLETE / UNCOMPLETE / etc. If not DELETE: optional "move to category" (categoryId). Checkbox "delete" vs "complete/uncomplete" + "move to category" dropdown (all categories + "don't move").
- **Completed tasks:** 
  - Same structure: DELETE or MOVE or UNCOMPLETE, etc., with optional target category.
- **Unique constraint:** One routine per (categoryId, frequency) or allow multiple per category (e.g. two weekly routines on different days). Spec says "rutiny nad tasky kategorií" — so one or more routines per category; design so each routine has its own schedule and actions.

Routines operate on **categories**, not on widgets. When a routine runs, it affects tasks in that category; widgets that display that category will show updated state after refresh.

---

## 5.2 Routine use cases

- **Crud:** `GetRoutinesForCategoryUseCase`, `SaveRoutineUseCase`, `DeleteRoutineUseCase`.
- **Execution (domain):** `ExecuteRoutineUseCase(routineId)` or `ExecuteRoutinesDueUseCase()`. Logic:
  - Load routine; determine "due" (current time matches schedule).
  - Query tasks in category (incomplete and/or completed according to routine config).
  - Apply actions: delete (move to trash), move to category, set completed/uncompleted.
  - Persist changes; return list of affected widgetIds (all widgets that show this category) for refresh.

WorkManager worker will call this execution use case; it does not contain business rules, only scheduling and invocation.

---

## 5.3 WorkManager: routine worker

- **Unique periodic work:** e.g. `RoutineWorker`, enqueued with `UniqueWorkPolicy.KEEP`, period at least 15 minutes (or as allowed). Worker runs periodically; each run:
  - Query all routines (or routines that are "due" in current window). Due = schedule matches (e.g. daily at 8:00 → run when current time is around 8:00).
  - For each due routine, call `ExecuteRoutineUseCase` (or batch use case that runs all due routines).
  - After execution, collect all affected widgetIds and call `AppWidgetManager.notifyAppWidgetViewDataChanged` for each so widgets refresh.
- **Scheduling nuance:** Exact time (e.g. "every day at 8:00") can be implemented with a single run at period boundary and "due" check in code, or with OneTimeWorkRequest rescheduling (run at next 8:00, then schedule next 8:00). Prefer simple periodic + due check for Android 7+ compatibility.
- **No business logic in worker:** Only "get due routines → call use case → refresh widgets".

---

## 5.4 Routine UI in app

- **Entry:** From Category settings (Stage 4), add section "Routines" or link "Manage routines" → list of routines for this category.
- **Routine list:** For a category, show routines (e.g. "Daily 8:00", "Weekly Monday 9:00"). Add routine → Routine edit screen.
- **Routine edit screen:**
  - Frequency: Daily / Weekly / Monthly / Yearly.
  - Schedule: time picker; for Weekly add day picker; for Monthly add day-of-month; for Yearly add date picker.
  - Visibility: dropdown Hidden / Visible; if Visible, optional "from" and "to" (UI depends on frequency: time range for daily, day range for weekly, etc.).
  - Incomplete tasks: action (Delete / Move / Complete / Uncomplete…) + if Move: category dropdown; checkboxes as per spec (delete vs complete/uncomplete + move).
  - Completed tasks: same structure.
  - Save / Cancel / Delete routine.
- ViewModel uses routine use cases; on save, no need to reschedule WorkManager if worker runs periodically and only checks "due" in code. If you use exact-time scheduling, reschedule on save.

---

## 5.5 Trash screen (app)

- **Spec:** "History of deleted records, grouped by day; restore, clear all, or delete single."
- **Screen:** New Activity or Fragment: "Trash". Load via `GetTrashEntriesByDayUseCase` — returns entries grouped by day (e.g. `Map<Day, List<TrashEntry>>` or list of day headers + entries).
- **UI:** List: for each day, header (date); under it, list of trashed tasks (show content, category name, deleted time). Per entry: Restore, Delete permanently. Bottom or menu: "Empty trash" → `ClearTrashUseCase`.
- **Restore:** `RestoreFromTrashUseCase(trashEntryId)` — re-insert task into original category (or default category if category was deleted), remove from trash. Refresh widgets that show that category.
- **Delete permanently:** `DeleteFromTrashUseCase(trashEntryId)` — remove from trash table only.
- **Empty trash:** `ClearTrashUseCase()` — delete all trash entries. Refresh all widgets if needed (optional).

---

## 5.6 Extensions: architecture hooks (no implementation)

Design so the following can be added later without rewriting core:

- **Google Calendar sync:** 
  - New module or package: sync adapter / use case that reads calendar events and creates/updates tasks in selected categories. Categories with type "today" / "tomorrow" / weekday can have sync rules (e.g. "sync with: today"). 
  - Store sync settings (e.g. per category: sync enabled, calendar id, sync interval). Routine-like "sync" schedule (hourly/daily/weekly) or manual "Sync" button. 
  - Domain: `SyncCalendarUseCase`; widget and app only consume task list as today.

- **Precreated widgets:** 
  - Seed data: on first launch or "Add preset", insert default `WidgetConfig` + default categories + sample tasks. No new features; only default data and maybe a simple "Try these presets" screen.

- **Trash as journal:** 
  - Same trash table; add filtering/sorting and richer day view. No schema change; only UI and optional export.

- **Notifications:** 
  - New use case: "Notify if tasks not completed by X time." Schedule with WorkManager or AlarmManager; show notification. Config in app (per category or global).

- **Lepší posuvník (ActivateOnTap):** 
  - If widget constraints allow: add setting "Open reorder on edge tap (false = direct drag; true = popup then drag)." Store in WidgetConfig; widget and reorder overlay read it. Omit if not feasible with RemoteViews.

Ensure:
- Category has `categoryType` for "today", "tomorrow", "monday"…
- Routine execution and sync are use-case driven so calendar sync can call similar "apply changes to tasks" logic.
- Widget never contains business logic; it only renders state and sends intents.

---

## 5.7 Deliverables

- Routine CRUD in data/domain layer; `ExecuteRoutineUseCase` (or equivalent) with full business rules; Worker that queries due routines, runs use case, refreshes affected widgets.
- Routine UI: list per category, add/edit routine with all spec fields (schedule, visibility, incomplete/completed actions).
- Trash screen: list by day, restore, delete single, empty trash; use cases already in Stage 1, only UI and wiring.
- Documentation or short comments in code where extension points are (e.g. "Calendar sync will implement SyncProvider here").

After Stage 5, the app is feature-complete per spec: widget, app settings, categories, routines, and trash. Extensions can be added incrementally on top of this architecture.
