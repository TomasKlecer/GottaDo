# Stage 2: Widget — Display & Basic Structure

**Goal:** Implement the widget as a semi-transparent, scrollable list that shows categories and records. No interactions yet (no click-to-edit, no long-press, no reorder). Widget only renders state provided by a use case; no business logic inside the widget.

---

## 2.1 Widget state (from Stage 1)

**GetWidgetStateUseCase(widgetId)** (defined in Stage 1) must return a single DTO containing:
- Widget config: colors, alpha, font sizes, default text color, reorder handle position.
- Ordered list of “category blocks”: for each category visible in this widget, category id, name, and display options (`showCheckboxInsteadOfBullet`, `tasksWithTimeFirst`), plus ordered list of tasks (id, contentHtml, completed, bulletColor, scheduledTimeMillis). Order: by widget’s category order; within category by `tasksWithTimeFirst` then time/sortOrder.

Widget process: call this use case (e.g. from a service or via AppWidgetManager update), get state, pass to rendering. No logic in widget beyond “take state → build RemoteViews”.

---

## 2.2 Widget layout (RemoteViews)

- **Root:** Semi-transparent background (use config background color + alpha). Shape or 9-patch if needed for “square” look.
- **Scrollable list:** Use `ListView` (or `RecyclerView` via AppWidgetHostView if not available in RemoteViews — on Android 7+ we use standard widget APIs). RemoteViews typically use `ListView` with `RemoteViewsService` + `RemoteViewsFactory`.
- **Item types:**
  - **Category row:** Single layout — bold, slightly larger font; text = category name.
  - **Record row:** Per-category display option: show either a **bullet** (colored dot) or a **checkbox** (from `showCheckboxInsteadOfBullet` in widget state). Text can be multi-line. When `completed == true`, render as strikethrough + gray; otherwise default text color from config.
- **Font sizes:** From widget config (category font size, record font size). Apply to the corresponding item layouts.
- **Scrolling:** List must scroll so that many categories/records are accessible. Bottom area (buttons: “Open app”, “Reorder”) is part of the same scroll content so they appear only when user scrolls to the end — implemented as last items in the list or a footer in the same scroll.

Do not add click listeners or reorder UI in this stage; only visual layout and data binding.

---

## 2.3 RemoteViewsService + RemoteViewsFactory

- **Service:** Registered in manifest; returns a `RemoteViewsFactory`.
- **Factory:** Given `widgetId` (and optionally `intent` extras), obtain `GetWidgetStateUseCase` (via DI or application context), call `GetWidgetStateUseCase(widgetId)`, get the list of “row” items (flattened: category row, record row, category row, … plus footer rows for “Open app” / “Reorder” if reorder not disabled).
- Map state to list items: each item is either category or record or footer. Use different `layoutId` per type so the factory returns the appropriate RemoteViews for each position.
- Apply colors/font sizes from config to each row. For HTML content, RemoteViews do not support Spanned; store and display plain text in the widget list (or a stripped version); full HTML is for the edit UI. So either: show plain text in widget and keep HTML for edit, or use a single-line/simplified representation in widget (spec: “řádek může zobrazovat i víceřádkový text” — use setCharSequence and allow multi-line in the layout if RemoteViews support it).

---

## 2.4 Widget update trigger

- **AppWidgetProvider:** On `onUpdate`, for each widgetId ensure a default `WidgetConfig` exists (insert if not present), so `GetWidgetStateUseCase(widgetId)` always has data and the app’s widget list (Stage 4) can show all instances. Then request widget state from use case and refresh the list (set adapter intent, set background, etc.). On `onEnabled`/`onDisabled` handle first/last widget instance if needed.
- **Refresh:** Whenever data can change (task/category/config), call `AppWidgetManager.notifyAppWidgetViewDataChanged(widgetId, viewId)` and/or update the widget. Data changes will be triggered from app or from future stages (edit, reorder, routines); for this stage, a manual refresh or one-time load is enough.

---

## 2.5 Multi-instance support

- Each `widgetId` is passed to `GetWidgetStateUseCase`. Config and visible categories are stored per `widgetId` (Stage 1). Ensure the factory and provider use the correct `widgetId` for each instance.

---

## 2.6 Deliverables

- Widget layout(s): root (background + list), item layouts for category row, record row, and footer row(s) for “Open app” / “Reorder”.
- `RemoteViewsService` + `RemoteViewsFactory` that use `GetWidgetStateUseCase(widgetId)` and map result to list items.
- AppWidgetProvider that builds RemoteViews (background, list with service intent) and triggers updates.
- No click/long-press/reorder behaviour yet — Stage 3 will add those.

After Stage 2, the widget is visible and shows live data from the database for each instance, with correct styling and scrolling.
