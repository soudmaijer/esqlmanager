# eSQLManager

Java Swing database manager (originally an Errorsoft graduation project, 2002-2003), modernised to run on Java 25 with Maven. It manages MySQL and PostgreSQL; SQL Server and Oracle have a dialect for browsing only.

## Build, run, test

* Maven wrapper only (`./mvnw`), never Gradle. Java 25 (`maven.compiler.release`), sources are UTF-8.
* Run: `./mvnw compile exec:exec` (starts `nl.errorsoft.esql.Main` with `runtime/` as working directory, see Configuration below).
* Validate locally: `./start.sh` starts a PostgreSQL 17 container (`esql-pg`, user `postgres`, password `test`, sample database `shop`, on the first free port from 5432, the script prints it) and the application, `./start.sh --db-only` only the database, `./stop.sh` removes the container.
* Test: `./mvnw test`. The tests start Postgres 17 and MySQL 8 with Testcontainers and are skipped without Docker.
* A GUI cannot be started inside the Claude sandbox (no display). Run harnesses with the sandbox disabled, and verify UI work by painting the root pane to a `BufferedImage` in-process.

## Finishing a change

* Update `README.md` and `changelog.txt`, and retake `docs/screenshot.png` and `docs/designer.png` when the UI changed, before reporting a change as done.

## Design decisions

### Database differences live in a Dialect

* `dialect/Dialect` is the single place that knows how a server differs: quoting, string literals, DDL for tables, columns and indexes, paging, switching database, listing databases and tables, table maintenance, and exporting a table definition. `AbstractDialect` holds ANSI/JDBC-metadata behaviour, `dialect.mysql.MySqlDialect` and `dialect.postgres.PostgresDialect` override what differs.
* Callers ask the dialect. Never branch on the server type (`if type == MY_SQL`) outside `dialect`. Adding a database means adding a `Dialect`, a `<driver>` section in `runtime/conf/datatypes.xml`, and a case in `Dialects.forType`.
* Optional functionality is declared with `Dialect.supports(Feature)`; the controllers show a message when a feature is missing.
* `UserAdmin` (per dialect, from `Dialect.getUserAdmin()`) holds account and privilege management. MySQL accounts are `user@host` and privileges are GRANT/REVOKE; PostgreSQL accounts are roles, global privileges are role attributes, database and table privileges come from the object ACLs.
* PostgreSQL has one database per connection: `useDatabase` reconnects. The profile's "Database(s)" field is a filter, and its first entry is the database to connect to (default `postgres`). Tables are looked up in `current_schema()`.
* The primary key index is always presented as `PRIMARY`, whatever name the server gave it. `Dialect` methods take the quoted/unquoted name and quote it themselves.

### SQL safety

* Identifiers go through `dialect.quote(...)`, values through `dialect.literal(...)` (or `DatabaseConnection.formatFieldValue`, which delegates to it). Never concatenate a raw name or value into a statement. MySQL escapes backslashes in literals, PostgreSQL does not, which is why this is per dialect.
* Metadata lookups use `PreparedStatement` parameters.
* An empty cell is SQL `NULL`, not the text "null" (`TableData.isNull()`).
* Paging on PostgreSQL and MySQL orders by the primary key so that rows do not move after an update.
* A script written by Export switches database with `\connect` on servers that cannot do it in SQL; Import understands it.

### UI

* Look and feel is FlatLaf, chosen through `app.Appearance` (Follow the system, Light, Dark, Native) and stored in `conf/settings.xml` (`<appearance>`), changed in Settings > Preferences and applied at once. On macOS FlatLaf uses its Mac themes, `Main` sets the screen menu bar and system appearance properties before the first window (`Appearance.prepareDesktop`). The native macOS look was far too slow when resizing, so it is only used when the user picks Native.
* Use Swing only, no AWT widgets (`Label`, `Button`, ...). New dialogs use layout managers, not null layouts with absolute bounds.
* Do not hardcode `Color.white` or `Color.gray`. Take colours from `UIManager`. A read-only `JTextPane` is painted grey by FlatLaf, set its background explicitly.
* Swing is touched on the event thread. `ESQLManagerUI.print` and `setStatusInfo` marshal themselves with `invokeLater`.
* The query editor is an `RSyntaxTextArea` (com.fifesoft) in an `RTextScrollPane` with SQL highlighting, line numbers and the library's undo. `ui.EditorTheme.install` gives it the RSyntaxTextArea theme `idea.xml` or `dark.xml` matching `FlatLaf.isLafDark()` and applies it again when the look and feel changes.
* The output panel is a read-only `RSyntaxTextArea` with SQL colouring (`ui.editor.EditorTheme`, same theme as the query editor): it shows the log exactly as written.
* The output panel does not wrap lines (re-wrapping a long log made resizing slow), keeps at most 200000 characters, and the split pane uses continuous layout with `resizeWeight` 1.0.
* The status bar of the application shows the state with its light on the left and the server and account of the active connection on the right (`ConnectionWindowCC.showStatusInfo`). What a connection did last ("shop: 4 table(s) opened in the designer") is shown in the status bar at the bottom of that connection's own window (`ConnectionWindowUI.setStatus`).

### Logging and output

* log4j2 only, never `System.out`/`System.err`. `OutputPanelAppender` shows log lines in the output panel. Query text is logged at `debug` in `nl.errorsoft.esql.jdbc`.
* Log connection start, server product/version and driver, so the output explains what happened.

### Errors

* Services and repositories do not swallow exceptions, they throw. A problem the user can fix (a row that can't be identified, a feature the database does not have) is an `EsqlException`, anything else is unexpected.
* The controller or window that handles a user action catches and calls `ApplicationContext.get().errors().report(parentWindow, "Drop table", e)`. That logs the error once (a stack trace only for unexpected ones) and shows one message, "Drop table failed: <cause>". Do not call `log.error` and `showErrorMessage` yourself.
* Long running jobs (export, import) report through `ProgressListener.failed`. `Main` installs `report` as the uncaught exception handler.
* A catch that stays silent has a comment saying why (for example a table that can't be counted is still listed).

### Resources and threads

* Everything `AutoCloseable` is opened in try-with-resources: result sets, statements, streams, and `DatabaseConnection` itself. `DatabaseConnection.executeQuery` closes its statement when the result set is closed.
* Background work (export, import, the process list) runs on virtual threads (`Thread.ofVirtual()`). Swing is still only touched on the event thread.

### Java language level

* Java 25 idioms: records for plain data and parameter objects, pattern matching for `instanceof` and `switch`, switch expressions, text blocks for SQL, lambdas instead of anonymous classes, diamond operator. `java.util.Observable` is gone, progress goes through `ProgressListener`.

### Configuration and resources

* The version in the title bar and splash comes from the pom, the commit from `git-commit-id-maven-plugin`; both are written to `build.properties` by resource filtering (`app.ESQLManager`). A build with uncommitted changes shows `-dirty` after the hash, without git the commit is `unknown`. There is no build counter.
* `runtime/` is the working directory: `conf/` (profiles, drivers, datatypes, settings), `credits.txt`. Code reads `conf/...` relative to the working directory, also in tests (surefire `workingDirectory`).
* `runtime/conf/profiles.xml` must not contain passwords or local test profiles when committed. Running the application rewrites it (`lastUsed`), so `git checkout runtime/conf/profiles.xml` before committing.
* JDBC drivers come from Maven Central, no jars in the repository.
* Images, HTML and `log4j2.xml` are in `src/main/resources`. Images are loaded through `ImageLoader` (cached, paths relative to the classpath root).
* Icons are Lucide SVGs in `src/main/resources/icons/svg` (ISC license, `NOTICE.txt`), drawn with FlatLaf's `FlatSVGIcon` so they follow the theme. A new icon is a stroke `#6e6e6e` SVG plus an `images.addIcon(name, file, size, selected)` line in `ApplicationContext.imageLoader()`; callers use `imageLoader().getIcon(name)`, never `new ImageIcon(...)`. The tree uses a `...sel` variant (selection colour) for selected rows. GIFs remain only for the window icon and the splash.
* There is no licensing, registration or auto-update any more; do not reintroduce them.

### Code style

* Formatting is done by Spotless with the Eclipse formatter profile in `.eclipse-formatter.xml` (Java conventions: braces at the end of the line, tabs, 160 columns, LF line endings; if, else, for and while always have braces). Run `./mvnw spotless:apply` before committing, `./mvnw verify` runs `spotless:check` and fails on unformatted code. `.editorconfig` holds the same basics for editors. IntelliJ's own formatter does not follow it.
* Resources are closed with try-with-resources. No deprecated API in new code (`new Integer`, `Dialog.show()`, ...).
* No em-dashes in prose or documentation.

## Verifying UI changes

* Retake `docs/screenshot.png` and `docs/designer.png` with a harness that paints the windows in-process against a Postgres container (use a free host port, 5432 is often taken), preferably in a subagent so the images stay out of the main context. Look at the result, check icons, alignment and the status bar.
* The designer can be checked without a database: build a `Model` in a harness, add it to a `ModelViewer` (`new ModelViewerControl(null)` is enough), paint it to a `BufferedImage` with `FlatLightLaf` and `FlatDarkLaf` and look at both.

## Tests

* `DialectContractTest` runs the same scenarios against every dialect: create and alter tables, indexes, export and import, editing data with hostile text, user management, creating databases. `PostgresDialectTest` and `MySqlDialectTest` supply the container. A new dialect gets a subclass.
* Prefer a real database over mocks for anything that produces SQL.

## Architecture

The code is moving from layers by technical type (`gui`, `control`, `domain`, `data`) to packaging by feature. The target:

* Package by feature (functional packaging), for example `connection`, `database`, `table`, `export`, `importer`, `server`, `designer`, `user`, `query`.
* Inside each feature the layers are strictly UI -> Controller -> Service -> Repository. Dependencies point downwards only.
  * UI (Swing): no business logic, no JDBC. Swing classes only in UI packages.
  * Controller: translates UI events into service calls and shows the outcome. It gets its service from `cwcc.getContext()`.
  * Service: application logic (order of steps, validation, updating the domain objects after a change). Calls repositories, never SQL.
  * Repository: the only place that runs SQL. It extends `jdbc.AbstractRepository`, which holds the connection and gives `dialect()`, `quote()`, `literal()`, `useDatabase()`, `executeUpdate()` and `executeAll()`. A repository asks the `Dialect` for everything that differs per server and never branches on the server type.
* Domain types (`Table`, `TableColumn`, `TableIndex`, `TableData`, `DatabaseUser`, ...) are plain data without a connection.
* A result that is more than one object is a small record in the feature package (`table.QueryResult`).
* Do this one feature at a time and keep the contract tests green.

Features and their packages (all under `nl.errorsoft.esql`; each has `control` and `ui` subpackages where it has windows):

* `table`: tables, columns, indexes, rows (`Table`, `TableColumn`, `TableIndex`, `TableData`, `TableService`, `TableRepository`, `QueryResult`).
* `database`: databases and their table lists (`Database`, `DatabaseService`, `DatabaseRepository`), including the tree view.
* `export` and `importer`: SQL export (`ExportService`, `ExportRepository`, `ExportOptions`) and import (`ImportService`, `ImportRepository`). Services run on their own thread and report progress to a `job.ProgressListener`; `job.ui.ImportExportProgressUI` shows it for both.
* `blob`: uploading and saving binary cells (`BlobService`, `BlobRepository`).
* `user`: accounts and privileges (`UserService`, `UserRepository`). The SQL itself is in the dialect's `UserAdmin`, which the repository wraps.
* `designer`: the model designer. `DesignerService` creates the designed databases and tables from `DesignedDatabase` and `DesignedTable`, then adds each table's `DesignedForeignKey`s in a second pass (skipping keys that exist, checking that the columns exist) through `TableService.addForeignKey`.
  * The model links a table to its database with a generic reference (`Model.addReference`, never table-to-table). Foreign keys are separate: `designer.model.ForeignKey` records in `Model` (`addForeignKey`, `foreignKeysOf`), removed with their table and kept in step with renamed or removed fields (`Model.fieldsEdited`, called by `TableProperties`).
  * Model files (.edm) are read and written by `designer.model.ModelXml` with JDOM. New files are version 0.2 (with `<foreignkeys>`), 0.1 files still load, missing sections mean empty. `ModelPersistenceTest` has a 0.1 fixture in `src/test/resources/designer`.
  * Foreign key SQL comes from `Dialect.addForeignKeySql`/`dropForeignKeySql`; actions are checked against the whitelist `Dialect.REFERENTIAL_ACTIONS`. `Dialect.checkForeignKeyTable` refuses MySQL tables that are not InnoDB.
  * Look: objects are cards painted by themselves (`TableObject`, `DatabaseObject` as a pill, `CommentObject` as a note) with colours and fonts from `designer.ui.diagram.DesignerTheme` (UIManager keys, so light and dark both work). A component is larger than its card by `DesignerTheme.SHADOW` for the shadow; `ModelObject.cardBounds()` is the card in viewer coordinates, `contains` only accepts the card, and model files store the card position (`setCardLocation`). The table header shows the storage engine only when the server's dialect has table types (`ModelViewer.setShowTableTypes`). `ModelViewer` paints the canvas with an optional dotted grid (View > Show Grid).
  * Relations are drawn by `designer.ui.diagram.ConnectorRenderer` below the cards: a foreign key is an orthogonal connector with rounded corners from the child's column row (`TableObject.rowAnchorY`) to the parent's, a crow's foot at the child and a double bar at the parent, around the right side when the cards overlap horizontally or for a self reference. Hit testing uses an 8px stroked shape. Database and note links are dashed curves.
  * Foreign key UI: drag from the field icon of a row onto a column of another table (a ghost connector follows the mouse; `Model.mouseDragged` does not move a table when the drag starts on a field icon), a table's context menu "Add Foreign Key...", or the Foreign Keys tab of `TableProperties`. All open `ForeignKeyDialog`, which checks the key with `ForeignKey.validate()` (columns exist, types of the same kind, allowed actions) and reports problems through the `ErrorHandler`. Double click on a connector edits it, Delete or its context menu removes it. Shift-drag still links a database or a note.
  * Reverse engineering: "Open in designer" (database context menu and toolbar of the connection window, enabled for a database node) calls `ConnectionWindowCC.openDatabaseInDesigner`. `DesignerService.reverseEngineer(Database)` returns a `DesignedDatabase` with every table (views left out, `DesignerRepository.loadTableNames` asks the metadata for type TABLE), its columns, primary key (composite too), single column indexes as the column's index/unique flags, and the foreign keys between tables of that database (`getImportedKeys`, NO ACTION as empty). `Dialect.readColumn` turns a `getColumns` row into a `CreateColumn` with the type names of `datatypes.xml` (PostgreSQL `int4` becomes `integer`, serial and identity columns become auto increment, `'x'::character varying` becomes `x`; MySQL strips `UNSIGNED` into the flag). `designer.ui.diagram.ModelFactory.fromDatabase` turns the records into a `Model`, `DBCreator(eui, cwui, model)` shows and arranges it. Generating it again skips what exists. Contract test: `designerReadsAnExistingDatabaseBack`.
  * Layout: `designer.layout.AutoLayout` (no Swing, sizes and edges in, positions out) uses the layered algorithm of the Eclipse Layout Kernel (`org.eclipse.elk.alg.layered`, direction right), so a referenced table is left of the tables that refer to it; tables without relations (or only a self reference) go in a grid below. ELK needs `LayoutMetaDataService.registerLayoutMetaDataProviders(new LayeredMetaDataProvider())` outside Eclipse and the undeclared `org.eclipse.xtext.xbase.lib`; Guava is pinned because ELK asks for any version. `ModelArranger.arrange(Model)` puts the databases in a row at the top and the tables below with it (View > Arrange Automatically). `AutoLayoutTest` checks overlap and order.
  * Exports: `designer.export.DiagramExporter` writes a `DiagramModel` (plain records made from a `Model` with `DiagramModel.of`) as PlantUML or Mermaid text, without Swing (File > Export as PlantUML/Mermaid).
* `designer.ui.diagram` holds the canvas (`ModelViewer`, the model objects, `ConnectorRenderer`, `ModelBrowser`), `designer.ui.dialog` the property, foreign key, history and generate dialogs, `designer.ui` the window (`DBCreator`).
* `query`: statements typed by the user (`QueryService`, `QueryRepository`) and the editor with syntax highlighting.
* `connection`: profiles, drivers, server types, the connection window and `ConnectionContext`.
* `server`: process list, server status and variables (`ServerService`, `ServerRepository`, `ServerProcess`, `server.ui.Processlist`).
* `app`: main window, credits, splash, start up. `settings`: `Settings`, `Appearance` and `settings.ui.SettingsUI`.
* `ui`: Swing parts shared by several features: `ui.icon` (`ImageLoader`, `StatusLight`), `ui.table` (`ColumnWidths`, sortable headers, `MultiLineCellEditor`), `ui.editor` (`EditorTheme`), `ui.util` (`DesktopUtils`, file filter, hyperlinks).
* `jdbc`: `DatabaseConnection` and `AbstractRepository`. `dialect`: `Dialect`, `AbstractDialect`, `Dialects`, `UserAdmin`, with `mysql`, `postgres`, `sqlserver` and `oracle` sub packages. `error`: `EsqlException` and `ErrorHandler`. `job`: `ProgressListener`. `CreateColumn` and `DataType` are in `table`.
* `app.ApplicationContext` (singleton, `ApplicationContext.get()`) is where components are resolved from. It holds what exists once per application (`imageLoader()`, `settings()`) and one `connection.ConnectionContext` per open connection (`connection(databaseConnection)`, released when the window closes). Do not pass an `ImageLoader` or `Settings` through controllers.
* `ConnectionContext` creates every repository and service of one connection once and wires them with constructor injection. Services are per connection because several connections are open at the same time, so they are never application singletons. Controllers get a service from `cwcc.getContext()` (`getContext().tables()`, `.databases()`, `.users()`, `.servers()`, `.queries()`, `.designer()`) and never create services or repositories themselves. A service receives its repository and the services it needs in its constructor (`BlobService` uses `TableService.rowFilter`). Jobs that report progress to observers (export, import, blob transfer) are created per run with `newExport`, `newImport` and `newBlobTransfer`. A new feature adds its repository and service to `ConnectionContext`.
* Parameters that belong together are a record (`ExportOptions`, `DesignedTable`), not a long argument list.

## Known technical debt

* Raw `Vector` and other raw types (about 100 lint warnings).
* Many dialogs still use null layouts (`ConnectionWindowUI`, `CreateTable`, `IndexesUI`, ...).
* `Dialect` and `UserAdmin` methods such as `listTables`, `maintain`, `dropIndexSql` still take a `DatabaseConnection` and run SQL themselves, repositories only wrap them.
* Some windows (`Processlist`, `DatabaseTreeView`) hold more logic than a UI should.
* SQL Server and Oracle dialects only browse; their DDL, user management and maintenance are not implemented.
