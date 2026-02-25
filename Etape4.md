# Stage 4: App — Widget & Category Management

**Goal:** The app is the “settings” surface: manage multiple widget instances, configure each widget (title, appearance, category order, reorder handle), manage categories (shared across widgets), and prepare extensibility for styles/backgrounds/fonts. No routines or trash UI yet.

---

## 4.1 App entry and widget list

- **Main screen:** List of configured widget instances (widget IDs that have a row in `WidgetConfig`). Option to “Add widget” — actually means “configure a new widget”; user adds the widget from the system launcher, then opens app to configure it. So app shows: all `widgetId`s that exist in `WidgetConfig`, with title/subtitle or “Widget 1”, “Widget 2” for empty titles. Tapping a row opens that widget’s settings.
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

**Extensibility (NOTE from spec):** Program so it’s easy to add later:
- Style presets (e.g. “notebook” look).
- Custom widget background image.
- Custom font.

Approach: keep `WidgetConfig` with a reserved “options” or JSON field for future key-value or structured options; or add columns in a later migration. UI: single “Widget settings” screen with sections; new sections can be added without breaking existing ones.

---

## 4.3 Categories: global and per-widget

- **Categories are global:** One `Category` table; many widgets can reference the same category via `WidgetCategoryJoin`. Creating a category in the app makes it available to any widget.
- **Category list screen:** Accessible from app (e.g. “Manage categories” in drawer or main screen). Lists all categories; add, edit name, delete (with care: if in use by widgets, either prevent delete or cascade/hide).
- **Per-category settings (spec):**
  - Category name.
  - **Checkbox vs bullet:** “Show checkbox instead of bullet” for records in this category.
  - **Tasks with time first:** Yes = tasks with `scheduledTimeMillis` before tasks without; No = opposite.
- **Per-widget category assignment:** In widget settings (4.2), user picks which categories appear in that widget and in what order; that’s the `WidgetCategoryJoin` (sortOrder, visible). So “add category” in app = new row in `Category`; “add category to widget” = new row in `WidgetCategoryJoin` linking widgetId to categoryId.

---

## 4.4 Category settings screen (per category)

- **Entry:** From “Manage categories” list, tap a category → CategorySettingsActivity(categoryId).
- **Fields:**
  - Name.
  - Checkbox: “Show checkbox instead of bullet” (store in `Category.showCheckboxInsteadOfBullet`).
  - Checkbox: “Tasks with time first” (store in `Category.tasksWithTimeFirst`).
- Save → `SaveCategoryUseCase`. After save, refresh all widgets that use this category (notify by widgetId list from join table).

---

## 4.5 Record (task) display options (spec: “Nastavení záznamů”)

- **Spec:** “Allow enabling what is shown in record edit — e.g. plain text only, no formatting, only completed checkbox.”
- **Placement:** Can live in app settings (global) or per-widget. Easiest: global “Record edit options” in app (e.g. in Settings or a dedicated screen): toggles for “Allow rich text”, “Show time field”, “Show category dropdown”, “Show bullet color”, etc. Store in SharedPreferences or a simple `AppPreferences` table. Record edit Activity (Stage 3) reads these and shows/hides fields accordingly.
- **Implementation:** Define a small model `RecordEditOptions` (booleans) and a use case or repo to load/save it. RecordEditActivity uses it to build the form. No new UI in widget; only app settings screen + wiring in edit screen.

---

## 4.6 Navigation and flow

- **Main:** List of widgets → Widget settings (4.2).
- **Widget settings:** Sections for appearance, category order/visibility, reorder handle; link to “Manage categories” (list of all categories). From category list → Category settings (4.4).
- **Optional:** Drawer or bottom nav for “Widgets”, “Categories”, “Settings” (where “Record edit options” lives).

---

## 4.7 Widget refresh from app

- After any change that affects a widget (config or category used by that widget), call `AppWidgetManager.notifyAppWidgetViewDataChanged(widgetId, viewId)` and/or full update so the widget re-fetches state via `GetWidgetStateUseCase`.

---

## 4.8 Special category types (prepare only)

- **Spec:** “Special categories: today, tomorrow, sunday… sync with calendar later.”
- **This stage:** Add `categoryType` to Category (already in Stage 1). In UI, allow selecting type “Normal”, “Today”, “Tomorrow”, “Monday”… Store as enum or string. **Behaviour:** Treat all as “normal” for now (no auto-move, no calendar). In Stage 5 or a future extension, routines and calendar sync will use `categoryType`. So: dropdown in category settings for “Type” with values that you will implement later; no logic change in task ordering or widget display beyond what’s already there.

---

## 4.9 Deliverables

- Main Activity: list of widget instances; tap → Widget settings.
- Widget settings Activity: all config fields; category order/visibility for this widget; reorder handle dropdown; extensible structure for future style/background/font.
- Category list: all categories; add; tap → Category settings.
- Category settings Activity: name, checkbox vs bullet, tasks-with-time-first.
- Record edit options screen (global): toggles for which fields appear in record edit; persistence and use in RecordEditActivity.
- Ensure new widget from launcher gets default WidgetConfig and appears in app list.

After Stage 4, the app fully controls widget appearance and category assignment; categories are shared and configurable; record edit can be simplified via options. Stage 5 adds routines and trash UI.
