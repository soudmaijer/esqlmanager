---
name: esql-ui
description: Swing UI rules of eSQLManager (U1-U9): dialogs, layouts, colours, icons, wording, menus, editor tabs, threading, verification. Applied to files in ui packages during code review.
user_invocable: false
---

# eSQLManager UI Rules

Nine rules derived from `CLAUDE.md`. Applied to every file in a `ui` or `ui.dialog` package and to controllers that show anything.

---

## Rule U1: Dialogs

**Flag:**
- `new JOptionPane(...)` or `JOptionPane.show...` anywhere except `ui.dialog.Dialogs`. Use `Dialogs.info/warn/error/confirm/confirmDestructive/input/form/askSave`.
- A dialog with fields that does not use `FormDialog`.
- A `showMessage` / `showErrorMessage` copy in a window.
- A destructive confirmation using `confirm` instead of `confirmDestructive`.
- A confirmation that does not name the object and what happens, or whose button is not the action ("Drop") next to Cancel.

```java
// Good
Dialogs.confirmDestructive(window, "Drop database 'shop' and all its tables? This cannot be undone.", "Drop");

// Bad
JOptionPane.showConfirmDialog(window, "Are you sure?");
```

## Rule U2: Layout

**Flag:**
- A null layout with absolute bounds outside the allowed places: the designer canvas (`DesignerCanvas`, its cards, text inside a `NoteCard`), and the splash.
- A dialog or panel not built with `ui.util.Forms` (12px padding, button row bottom right, titled groups, label/field grid).
- AWT widgets (`Label`, `Button`, `TextField`) instead of Swing.

## Rule U3: Colours

**Flag:**
- `Color.white`, `Color.gray`, `Color.BLACK` or a hex literal for UI chrome. Take colours from `UIManager` (grid lines `Table.gridColor`, designer colours through `DesignerTheme`).
- A read-only `JTextPane` without an explicit background (FlatLaf paints it grey).
- Colours cached at construction that must follow a look and feel change.

## Rule U4: Icons

**Flag:**
- `new ImageIcon(...)` or loading an image path directly. Use `ApplicationContext.get().imageLoader().getIcon(name)`.
- A new icon without the `images.addIcon(...)` line in `ApplicationContext.imageLoader()`, or a Lucide SVG whose stroke is not `#6e6e6e`.
- A server type mapped to an icon anywhere but `ServerType.iconName()`.

## Rule U5: Wording

**Flag:**
- Title case for titles or labels. Use sentence case; labels end with a colon.
- A primary button that is not the action ("Save", "Drop", "Create"); using "OK" where a verb fits.
- Cancel where nothing can be discarded (use Close), or Close where changes would be lost (use Cancel).
- Missing mnemonics on dialog buttons and menu items.
- A success popup when the status bar or the window already shows the outcome.
- Em-dashes in any user visible text, docs or comments.

## Rule U6: Menus and editors

**Flag:**
- A context menu built once at start up instead of when it opens, or showing items that do not apply.
- A popup opened on only one of `mousePressed` / `mouseReleased`, or without `isPopupTrigger()`, or before selecting what is under the cursor.
- A tree menu item not coming from `TreeMenu.itemsFor(node, dialect)`.
- An editor (create or edit table, indexes, a similar work area) that is a dialog or separate window instead of a tab implementing `ui.component.EditorTab` (`confirmClose()`, `removeTab` after a successful save).
- Esc closing a tab. Esc (`EscapeToClose`) is for dialogs only.
- A feature opening a separate top level window. Work views of a connection are `connection.ui.WorkFrame`s opened through its `ConnectionView` (`WorkFrames`), shown as tabs above the desktop of the main window; designers and help are internal frames there too.
- A tree action that is not wired in `ExplorerPanel.perform` to a `ConnectionWindowController` method of the connection the node belongs to (`TreeSelection.connection`).

## Rule U7: Event thread

**Flag:**
- Swing components touched from a virtual or background thread without `SwingUtilities.invokeLater`.
- Blocking work (JDBC, file IO) on the event thread. A controller runs it through `ConnectionWindowController.inBackground`, also the loads triggered by expanding a node in the explorer (selecting a node never loads anything).
- A thread doing a connection's work without `ConnectionWindowController.logContext()` (its log lines lose the connection name in the output panel).

```java
// Good
Thread.ofVirtual().start(() -> {
	var rows = service.load(table);
	SwingUtilities.invokeLater(() -> grid.show(rows));
});

// Bad
grid.show(service.load(table)); // JDBC on the EDT
```

## Rule U8: Verification

**Flag (as a note on the change, not on a line):**
- A UI change with no evidence that it was painted in-process to a `BufferedImage` and looked at. Light always; dark too only when the change paints or colours something new (a component, icon, colour, renderer or painter), not for logic, behaviour or enablement changes. Docs images are light only. See "Verifying UI changes" in `CLAUDE.md`.
- A visible UI change without retaken `docs/screenshot.png`, `docs/designer.png`, `docs/query.png` or `docs/connect.png`, README and `changelog.txt` (newest entry first).

## Rule U9: Contextual buttons

**Flag:**
- A toolbar button, menu bar item or view button that stays enabled when its action cannot run in the current context (nothing or the wrong node selected, no rows selected, nothing changed). It is disabled.
- A disabled button without a tooltip saying what is needed. Use `ToolbarButtons.setAvailable(component, missing)` with a sentence such as "Select a database first.".
- A button for an action the server never offers (`Dialect.supports(Feature)`, `maintenanceCommands()`) that is greyed out instead of hidden; the same for actions a view can never do (row buttons on a query result).
- Enablement of a tree action computed in a toolbar or menu by its own checks instead of `TreeMenu.missing(item, path, dialect)` and `TreeMenu.supported(item, dialect)`, the decisions the context menus use; for a list of tables `TreeMenu.missingForTables`.
- Labels that hardcode "database" or "schema" instead of `Dialect.databaseTerm()`/`schemaTerm()`.
