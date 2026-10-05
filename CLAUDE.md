# eSQLManager

Java Swing database manager (originally an Errorsoft graduation project, 2002-2003), modernised to run on Java 25 with Maven. It manages MySQL and PostgreSQL; SQL Server and Oracle have a dialect for browsing only.

## Build, run, test

* Maven wrapper only (`./mvnw`), never Gradle. Java 25 (`maven.compiler.release`), sources are UTF-8.
* Run: `./mvnw exec:java`. The working directory is `runtime/` (see Configuration below).
* Test: `./mvnw test`. The tests start Postgres 17 and MySQL 8 with Testcontainers and are skipped without Docker.
* A GUI cannot be started inside the Claude sandbox (no display). Run harnesses with the sandbox disabled, and verify UI work by painting the root pane to a `BufferedImage` in-process.

## Design decisions

### Database differences live in a Dialect

* `domain/dialect/Dialect` is the single place that knows how a server differs: quoting, string literals, DDL for tables, columns and indexes, paging, switching database, listing databases and tables, table maintenance, and exporting a table definition. `AbstractDialect` holds ANSI/JDBC-metadata behaviour, `MySqlDialect` and `PostgresDialect` override what differs.
* Callers ask the dialect. Never branch on the server type (`if type == MY_SQL`) outside `domain/dialect`. Adding a database means adding a `Dialect`, a `<driver>` section in `runtime/conf/datatypes.xml`, and a case in `Dialects.forType`.
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

* Look and feel is FlatLaf (`FlatLightLaf`), with the system look and feel as fallback. The native macOS look was far too slow when resizing.
* Use Swing only, no AWT widgets (`Label`, `Button`, ...). New dialogs use layout managers, not null layouts with absolute bounds.
* Do not hardcode `Color.white` or `Color.gray`. Take colours from `UIManager`. A read-only `JTextPane` is painted grey by FlatLaf, set its background explicitly.
* Swing is touched on the event thread. `ESQLManagerUI.print` and `setStatusInfo` marshal themselves with `invokeLater`.
* The output panel does not wrap lines (re-wrapping a long log made resizing slow), keeps at most 200000 characters, and the split pane uses continuous layout with `resizeWeight` 1.0.
* The status bar shows the action on the left and, next to it, the server, account and the last thing the active connection did (`ConnectionWindowCC.showStatusInfo`).

### Logging and output

* log4j2 only, never `System.out`/`System.err`. `OutputPanelAppender` shows log lines in the output panel. Query text is logged at `debug` in `nl.errorsoft.esql.data`.
* Log connection start, server product/version and driver, so the output explains what happened.

### Configuration and resources

* `runtime/` is the working directory: `conf/` (profiles, drivers, datatypes, settings, syntax), `credits.txt`. Code reads `conf/...` relative to the working directory, also in tests (surefire `workingDirectory`).
* `runtime/conf/profiles.xml` must not contain passwords or local test profiles when committed.
* JDBC drivers come from Maven Central, no jars in the repository.
* Images, HTML and `log4j2.xml` are in `src/main/resources`. Images are loaded through `ImageLoader` (cached, paths relative to the classpath root).
* There is no licensing, registration or auto-update any more; do not reintroduce them.

### Code style

* Formatting is done by Spotless with the Eclipse formatter profile in `.eclipse-formatter.xml` (Java conventions: braces at the end of the line, tabs, 160 columns, LF line endings; if, else, for and while always have braces). Run `./mvnw spotless:apply` before committing, `./mvnw verify` runs `spotless:check` and fails on unformatted code. `.editorconfig` holds the same basics for editors. IntelliJ's own formatter does not follow it.
* Resources are closed with try-with-resources. No deprecated API in new code (`new Integer`, `Dialog.show()`, ...).
* No em-dashes in prose or documentation.

## Tests

* `DialectContractTest` runs the same scenarios against every dialect: create and alter tables, indexes, export and import, editing data with hostile text, user management, creating databases. `PostgresDialectTest` and `MySqlDialectTest` supply the container. A new dialect gets a subclass.
* Prefer a real database over mocks for anything that produces SQL.

## Architecture

The code is moving from layers by technical type (`gui`, `control`, `domain`, `data`) to packaging by feature. The target:

* Package by feature (functional packaging), for example `connection`, `database`, `table`, `data` (row editing), `index`, `importexport`, `designer`, `user`, `query`.
* Inside each feature the layers are strictly UI -> Controller -> Service -> Repository. Dependencies point downwards only.
  * UI (Swing): no business logic, no JDBC. Swing classes only in UI packages.
  * Controller: translates UI events into service calls and shows the outcome. It creates a service per call from the connection of the window.
  * Service: application logic (order of steps, validation, updating the domain objects after a change). Calls repositories, never SQL.
  * Repository: the only place that runs SQL. It extends `data.AbstractRepository`, which holds the connection and gives `dialect()`, `quote()`, `literal()`, `useDatabase()`, `executeUpdate()` and `executeAll()`. A repository asks the `Dialect` for everything that differs per server and never branches on the server type.
* Domain types (`Table`, `TableColumn`, `TableIndex`, `TableData`, `DatabaseUser`, ...) are plain data without a connection.
* A result that is more than one object is a small record in the feature package (`table.QueryResult`).
* Do this one feature at a time and keep the contract tests green.

Features and their packages (all under `nl.errorsoft.esql`; each has `control` and `ui` subpackages where it has windows):

* `table`: tables, columns, indexes, rows (`Table`, `TableColumn`, `TableIndex`, `TableData`, `TableService`, `TableRepository`, `QueryResult`).
* `database`: databases and their table lists (`Database`, `DatabaseService`, `DatabaseRepository`), including the tree view.
* `importexport`: SQL export and import (`ExportService`, `ImportService`, with an `ExportRepository` and `ImportRepository`). Services run on their own thread and report progress to `Observer`s.
* `blob`: uploading and saving binary cells (`BlobService`, `BlobRepository`).
* `user`: accounts and privileges (`UserService`, `UserRepository`). The SQL itself is in the dialect's `UserAdmin`, which the repository wraps.
* `designer`: the model designer. `DesignerService` creates the designed databases and tables from `DesignedDatabase` and `DesignedTable`.
* `query`: statements typed by the user (`QueryService`, `QueryRepository`) and the editor with syntax highlighting.
* `connection`: profiles, drivers, the connection window, process list, server status and variables (`ServerService`).
* `app`: main window, settings, start up. `ui`: Swing parts shared by several features (`ImageLoader`, `ColumnWidths`, ...). `data`: `DatabaseConnection` and `AbstractRepository`. `domain`: types shared by features (`CreateColumn`, `DataType`) and `domain.dialect`.
* `app.ApplicationContext` (singleton, `ApplicationContext.get()`) is where components are resolved from. It holds what exists once per application (`imageLoader()`, `settings()`) and one `connection.ConnectionContext` per open connection (`connection(databaseConnection)`, released when the window closes). Do not pass an `ImageLoader` or `Settings` through controllers.
* `ConnectionContext` creates every repository and service of one connection once and wires them with constructor injection. Services are per connection because several connections are open at the same time, so they are never application singletons. Controllers get a service from `cwcc.getContext()` (`getContext().tables()`, `.databases()`, `.users()`, `.servers()`, `.queries()`, `.designer()`) and never create services or repositories themselves. A service receives its repository and the services it needs in its constructor (`BlobService` uses `TableService.rowFilter`). Jobs that report progress to observers (export, import, blob transfer) are created per run with `newExport`, `newImport` and `newBlobTransfer`. A new feature adds its repository and service to `ConnectionContext`.
* Parameters that belong together are a record (`ExportOptions`, `DesignedTable`), not a long argument list.

## Known technical debt

* Raw `Vector` and other raw types (about 100 lint warnings), `java.util.Observable`/`Observer` for progress reporting.
* Many dialogs still use null layouts (`ConnectionWindowUI`, `CreateTable`, `IndexesUI`, ...).
* `Dialect` and `UserAdmin` methods such as `listTables`, `maintain`, `dropIndexSql` still take a `DatabaseConnection` and run SQL themselves, repositories only wrap them.
* Controllers still create a service per call and some windows (`Processlist`, `DatabaseTreeView`) hold more logic than a UI should.
* SQL Server and Oracle dialects only browse; their DDL, user management and maintenance are not implemented.
