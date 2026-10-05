---
name: esql-architecture
description: Architecture, layering, SQL safety and naming rules of eSQLManager (A1-A8, N1-N6). Applied to all Java files during code review.
user_invocable: false
---

# eSQLManager Architecture Rules

Eight rules (A1-A8) and a naming standard (N1-N6), all derived from `CLAUDE.md`. Do not flag what `spotless:check` or the compiler already enforce.

---

## Rule A1: Layers

Inside each feature the layers are strictly UI -> Controller -> Service -> Repository. Dependencies point down only.

**Flag:**
- Swing components or `javax.swing` widgets outside `ui` packages. Controllers may only use `Dialogs`, `ErrorHandler` and `SwingUtilities`, never create components.
- Business logic, validation or JDBC in a UI class.
- A service that runs SQL, imports `java.sql`, or imports Swing.
- A repository or service calling upwards (controller, UI).
- JDBC (`Connection`, `Statement`, `ResultSet`) outside repositories. The only exception is a dialect mapping a `ResultSet` or metadata row to a domain object.
- A UI or controller calling a repository directly.

```java
// Good: controller asks the service
tables().drop(table);

// Bad: controller runs SQL
connection.executeUpdate("DROP TABLE " + table.getName());
```

## Rule A2: Dialect

`Dialect` is the single place that knows how servers differ.

**Flag:**
- A branch on the server type (`if (type == ServerType.MY_SQL)`, `switch (type)`) outside `dialect`.
- A dialect that runs SQL, or takes a `DatabaseConnection`. A dialect writes statements and maps rows.
- Optional functionality used without `Dialect.supports(Feature)`.
- Menu labels or messages with hardcoded "database"/"schema" words instead of `databaseTerm()` / `schemaTerm()`.

```java
// Good
String sql = dialect().limitSql(query, size, offset);

// Bad
String sql = type == ServerType.POSTGRESQL ? query + " LIMIT " + size : query + " LIMIT " + offset + "," + size;
```

## Rule A3: SQL safety

**Flag:**
- A raw identifier or value concatenated into a statement. Identifiers go through `dialect.quote(...)` (tables through `quote(TableName)`), values through `dialect.literal(...)` or `DatabaseConnection.formatFieldValue`.
- Metadata lookups without `PreparedStatement` parameters.
- An empty cell written as the text "null" instead of SQL `NULL` (`TableCell.isNull()`).
- Paging without ordering by the primary key on PostgreSQL and MySQL.

```java
// Good
String sql = "DELETE FROM " + quote(table.qualifiedName()) + " WHERE id = " + literal(id);

// Bad
String sql = "DELETE FROM " + table.getName() + " WHERE id = '" + id + "'";
```

## Rule A4: Context and injection

**Flag:**
- A controller that creates a service or repository (`new TableService(...)`). Controllers use `connectionWindowController.getContext().tables()` and the like.
- A new service or repository that is not created and wired in `ConnectionContext`.
- An application wide singleton service (services are per connection).
- `ImageLoader` or `Settings` passed through constructors or controllers instead of `ApplicationContext.get()`.

## Rule A5: Errors

**Flag:**
- A service or repository that swallows an exception instead of throwing.
- A problem the user can fix thrown as something other than `EsqlException`.
- A controller or window handling a user action without `ApplicationContext.get().errors().report(parent, "Drop table", e)`.
- `log.error` plus a dialog for the same failure.
- `JOptionPane` outside `ui.dialog.Dialogs`.
- An empty or silent `catch` without a comment saying why.
- Long running jobs not reporting through `ProgressListener.failed`.

```java
// Good
try {
	tables().drop(table);
} catch (Exception e) {
	ApplicationContext.get().errors().report(window, "Drop table", e);
}

// Bad
} catch (Exception e) {
	log.error("drop failed", e);
	JOptionPane.showMessageDialog(window, e.getMessage());
}
```

## Rule A6: Threads and resources

**Flag:**
- Background work (export, import, process list) not on a virtual thread (`Thread.ofVirtual()`).
- Swing touched off the event thread without `invokeLater`.
- An `AutoCloseable` (result set, statement, stream, `DatabaseConnection`) not opened in try-with-resources.
- Deprecated API in new code (`new Integer`, `Dialog.show()`, `Observable`).

## Rule A7: Parameter objects

**Flag:**
- A method with more than 4-5 functionally related parameters. Wrap them in a record (`ExportOptions`, `DesignedTable`).
- A result that is more than one object returned as an array, map or `Object[]` instead of a small record.

```java
// Good
void export(ExportOptions options)

// Bad
void export(Database db, File file, boolean data, boolean drop, boolean ifNotExists, String charset)
```

## Rule A8: Logging

**Flag:**
- `System.out` / `System.err` / `printStackTrace()`. Use log4j2.
- Query text logged above `debug` in `nl.errorsoft.esql.jdbc`.
- Connection start without server product, version and driver in the log.

---

## Naming (N1-N6)

This is the naming standard. New and renamed classes must follow it. Existing classes that do not follow it are reported in a separate "Rename debt" list at low severity, never as violations. Do not propose a mass rename inside a feature change.

### N1: Every class is `<Subject><Role>`

| Role suffix | Meaning |
|---|---|
| (none) | Domain object: plain data without a connection |
| `Options` | The user's choices for a job |
| `Result` | Outcome of an action |
| `Info` | Read-only summary for display |
| `Service` | Application logic, no SQL, no Swing |
| `Repository` | The only place that runs SQL |
| `Controller` | Turns a user action into service calls and shows the outcome (replaces the old `CC` suffix) |
| `Window` | Desktop window, frame or internal frame (replaces `UI`) |
| `Tab` | Content of a tab in the connection window |
| `Dialog` | Anything that is a `JDialog` (replaces `UI` and `Form` suffixes and bare names such as `Generate`, `Properties`, `Processlist`) |
| `Panel` | Composed Swing component inside a window, tab or dialog (replaces `View` / `Form`) |
| `Renderer`, `CellEditor` | Swing renderers and editors |
| `Painter` | Draws a shape on a canvas with `Graphics2D` (not a cell renderer), such as `ConnectorPainter` |
| `Card` | An object drawn on the designer canvas (`TableCard`, `DatabaseCard`, `NoteCard`, base `ModelCard`) |
| `Listener` | Swing event or callback |
| `Theme` | Colours and fonts |
| `Dialect`, `UserAdmin` | Per-server SQL writers |
| `Factory` | Creates objects |
| `Xml` | Reads and writes a file format |
| `Context` | Holds components together |
| `Exception` | Exceptions |

### N2: Static-only helpers are a plural noun

`Forms`, `Encodings`, `Validation`, `DesktopWindows`. Never `Util` / `Utils` / `Helper`.

A static class that is one algorithm or one conversion may be named after what it does instead (`AutoLayout`, `DiagramExporter`), when a plural noun would say less.

A dialog with a static entry point (`CreateDatabaseDialog.ask`, `PropertiesDialog.show`) is still a `FormDialog` subclass with a private constructor; the static method builds, shows and reads it.

### N3: Interfaces have no `I` prefix or `IF` / `Interface` suffix

Name them by role (`ProgressListener`, `EditorTab`).

### N4: Package layout

- `<feature>`: domain objects, records, `Service`, `Repository`.
- `<feature>.control`: `Controller`s.
- `<feature>.ui`: `Window`, `Tab`, `Panel`, `Renderer`, `Listener`.
- `<feature>.ui.dialog`: every `Dialog`.
- Shared Swing: `ui.dialog`, `ui.table`, `ui.icon`, `ui.component`.
- Export and import are a pair: `exporter` / `importer` (never `export`, which would not pair with `importer`, a reserved word as `import`).
- Swing-free rules used by a dialog (`designer.FieldRules`) live in the feature package, not in `ui.dialog`.

### N5: No abbreviations

No `CC`, no `UI` as a suffix, no variable names such as `cwcc`, `cwui`, `eui`. Spell out `connectionWindowController`.

### N6: Names say what, not how

A dialog is not `Generate`, a window is not `Processlist`. Name the subject and the role: `GenerateDialog`, `ProcessListDialog`.

**Flag for N1-N6:** a new class or a renamed class that breaks the table above. Existing violations: list under "Rename debt" with the proposed name.

```java
// Good
class ForeignKeyDialog extends FormDialog { }
class TableController { }

// Bad (rename debt if it already exists, a finding if it is new)
class ForeignKeyUI { }
class TableController { }
```

## Current deviations (open on purpose)

Known layer and naming problems that are not fixed yet. Do not report them again as new findings, fix them one feature at a time.

- `settings.Appearance` mixes an enum with look-and-feel (Swing/FlatLaf) code.
- `designer.model.Model` and `ModelXml` import Swing/AWT classes (they hold canvas objects such as `TableCard`).
- Controllers import `javax.swing` components directly.
- Services refer to `jdbc.DatabaseConnection`.
- `database.DatabaseListService` owns a scratch `DatabaseConnection` of its own and is created by the profile controller, not by `ConnectionContext` (it runs before a connection window exists).
- Some dialogs extend `JDialog` directly instead of `ui.dialog.FormDialog`.
- `app.ui.CreditsPanel` extends `java.awt.Canvas` (an AWT widget).
- `ui.util.DesktopWindows` also holds `openInBrowser`, which is not about the desktop.
- `error.ErrorHandler` shows the error dialog itself (Swing by design): it is the one place that turns a failure into a message, called from controllers and windows.
