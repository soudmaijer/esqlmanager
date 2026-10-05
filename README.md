# eSQLManager

A graphical database manager written in Java Swing. It started in 2002 as a graduation project and has since been brought up to date: it builds with Maven, runs on Java 25 and talks to current database servers.

![eSQLManager showing the products table of a PostgreSQL database](docs/screenshot.png)

![The model designer with a shop database and its tables](docs/designer.png)

## Features

Each database has its own dialect (`nl.errorsoft.esql.domain.dialect`) that decides how a feature is carried out, so the same feature works on MySQL and PostgreSQL. SQL Server and Oracle only have the features that work through plain JDBC; they have not been tested against a live server recently.

| Feature | MySQL | PostgreSQL | SQL Server | Oracle |
|---|---|---|---|---|
| Saved connection profiles with auto-connect | yes | yes | yes | yes |
| Several connections open at once, tiled or cascaded | yes | yes | yes | yes |
| JDBC driver configuration | yes | yes | yes | yes |
| Browse databases, tables, views and columns in a tree | yes | yes | yes | yes |
| Row count per table | yes | yes | yes | yes |
| View table data, paged | yes | yes | yes | yes |
| Sort table data | yes | yes | yes | yes |
| Edit cell values | yes | yes | yes | yes |
| Insert rows | yes | yes | yes | yes |
| Delete rows | yes | yes | yes | yes |
| Upload and download binary data (blobs) | yes | yes | yes | yes |
| Run your own SQL with syntax highlighting | yes | yes | yes | yes |
| Create a database | yes | yes | yes | no |
| Drop a database | yes | yes | yes | no |
| Create a table | yes | yes | no | no |
| Modify a table (rename, comment, storage engine) | yes | yes | no | no |
| Drop a table | yes | yes | yes | yes |
| Empty a table | yes | yes | yes | yes |
| Add, modify and drop columns | yes | yes | no | no |
| Index manager (primary key, unique, index, fulltext) | yes | yes | no | no |
| Table maintenance: optimize, analyze | yes | yes (VACUUM, ANALYZE) | no | no |
| Table maintenance: check, repair | yes | no | no | no |
| Export as SQL (structure and data) | yes | yes | no | no |
| Import an SQL script | yes | yes | no | no |
| Database designer: draw a model and generate it | yes | yes | no | no |
| User manager: accounts and passwords | yes | yes (roles) | no | no |
| User manager: privileges per server, database and table | yes | yes | no | no |
| Process list, with ending a process | yes | yes | no | no |
| Server status | yes | yes | no | no |
| Server variables | yes | yes | no | no |
| Output panel with connection details and executed queries | yes | yes | yes | yes |
| Status bar with server, account and the last action | yes | yes | yes | yes |

PostgreSQL shows the tables of the connection's current schema (normally `public`), and has no check and repair commands. A PostgreSQL connection is made to one database; opening another database in the tree reconnects.

## Requirements

- Java 25
- Docker is only needed if you want a throwaway database to try it out

Maven is not required, the project includes the Maven wrapper.

## Run

```sh
./mvnw compile exec:exec
```

The application runs from the `runtime/` directory, which holds its configuration.

## Configuration

Everything the application reads and writes at runtime lives in `runtime/`:

| Path | Contents |
|---|---|
| `runtime/conf/profiles.xml` | Saved connection profiles |
| `runtime/conf/settings.xml` | Application settings |
| `runtime/conf/driver.xml` | JDBC driver class and URL per database type |
| `runtime/conf/datatypes.xml` | Column types |

Profiles are normally created in the connection dialog. When you choose a server type the default port and user name are filled in (MySQL 3306 / `root`, PostgreSQL 5432 / `postgres`, SQL Server 1433 / `sa`, Oracle 1521 / `system`).

The **Database(s)** field of a profile is an optional comma separated filter. Leave it empty to list every database on the server. For PostgreSQL the first name is also the database the connection is made to, with `postgres` as the default.

Note that `profiles.xml` stores passwords in plain text. Keep local changes out of version control, for example with `git update-index --skip-worktree runtime/conf/profiles.xml`.

### Appearance

Settings > Preferences > Appearance chooses the look and feel: follow the system (light or dark, on macOS), light, dark or the native look of the operating system. The choice is stored in `settings.xml` and applies at once. On macOS the menu is in the screen menu bar. The icons are vector icons from [Lucide](https://lucide.dev) that follow the theme.

## Logging

The application logs with Log4j 2. The configuration is `src/main/resources/log4j2.xml`: messages go to the console and to the output panel. Executed queries are logged at `debug` level, set the `nl.errorsoft.esql.data` logger to `info` to hide them.

## Try it with a local PostgreSQL

```sh
./start.sh            # PostgreSQL 17 in Docker with a sample database "shop", then the application
./start.sh --db-only  # only the database
./stop.sh             # stop and remove the database container
```

The script prints what to fill in: server type **PostgreSQL**, host `localhost`, port `5432`, user `postgres`, password `test`.

## Development

```sh
./mvnw test
```

The tests start PostgreSQL 17 and MySQL 8 with Testcontainers, so they need Docker and are skipped without it. They run the same scenarios against every dialect: tables, columns, indexes, export and import, editing data, users and privileges, databases, server status and the process list.

Design decisions and the architecture are described in `CLAUDE.md`. Format the code with `./mvnw spotless:apply`.

## Project layout

```
src/main/java/nl/errorsoft/esql
  Main.java  starts the application
  table/ database/ importexport/ blob/ user/ designer/ query/ connection/
             one package per feature: data, service and repository, with
             control/ and ui/ below it for the controllers and Swing windows
  app/       main window, settings, the ApplicationContext
  ui/        Swing parts shared by features
  data/      the database connection and the repository base class
  domain/    shared types, including dialect/ with the per-database behaviour
src/main/resources   icons, images, help pages and log4j2.xml
src/test/java        dialect tests against real servers
runtime/             configuration the application reads and writes
```

## Credits

Programming and design by S. Oudmaijer and J. Walgemoed (Errorsoft), 2002 to 2003.
