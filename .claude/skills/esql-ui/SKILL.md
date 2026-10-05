---
name: esql-ui
description: Swing UI rules of eSQLManager (U1-U8): dialogs, layouts, colours, icons, wording, menus, editor tabs, threading, verification. Applied to files in ui packages during code review.
user_invocable: false
---

# eSQLManager UI Rules

Eight rules derived from `CLAUDE.md`. Applied to every file in a `ui` or `ui.dialog` package and to controllers that show anything.

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
- A null layout with absolute bounds outside the allowed places: the designer canvas (`DesignerCanvas`, its cards, text inside a `NoteCard`), internal frames placed by `DesktopWindows`, and the splash.
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
- A feature opening a separate top level window. Work windows are internal frames on the desktop.

## Rule U7: Event thread

**Flag:**
- Swing components touched from a virtual or background thread without `SwingUtilities.invokeLater`.
- Blocking work (JDBC, file IO) on the event thread.

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
- A UI change with no evidence that it was painted in-process to a `BufferedImage` (light and dark for the designer) and looked at. See "Verifying UI changes" in `CLAUDE.md`.
- A visible UI change without retaken `docs/screenshot.png`, `docs/designer.png` or `docs/query.png`, README and `changelog.txt` (newest entry first).
