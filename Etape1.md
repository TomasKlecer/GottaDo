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

**Task** (the “record” in the spec)
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
- Widget: `GetWidgetStateUseCase(widgetId)` — returns all data needed to render one widget: config + ordered list of “category blocks”. Each block: category id, name, display options (`showCheckboxInsteadOfBullet`, `tasksWithTimeFirst`), and ordered list of tasks (id, contentHtml, completed, bulletColor, scheduledTimeMillis). Ordering: by widget’s category order/visibility; within category by `tasksWithTimeFirst` then time/sortOrder. No business logic in widget; this use case computes ordering and filters.
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
