# eSQLManager

A graphical database manager written in Java Swing. It started in 2002 as a graduation project and has since been brought up to date: it builds with Maven, runs on Java 25 and talks to current database servers.

![eSQLManager showing the customers table of a PostgreSQL database](docs/screenshot.png)

## Features

- Connect to MySQL, PostgreSQL, Microsoft SQL Server and Oracle, with saved connection profiles and auto-connect.
- Browse databases, tables, views and columns in a tree, with row counts per table.
- View, edit, sort and page through table data, and run your own SQL with syntax highlighting.
- An output panel that follows what the application does: connecting, server version, the queries it runs.

Not every feature is available on every database yet. Each database has its own dialect (`nl.errorsoft.esql.domain.dialect`) that decides what is supported:

| | MySQL | PostgreSQL | SQL Server | Oracle |
|---|---|---|---|---|
| Browse databases, tables and data | yes | yes | yes | yes |
| Run SQL | yes | yes | yes | yes |
| User manager, process list | yes | no | no | no |
| Create and modify tables, indexes | yes | no | no | no |
| Import, export, database designer | yes | no | no | no |

PostgreSQL shows the tables of the connection's current schema (normally `public`). SQL Server and Oracle work through plain JDBC metadata and have not been tested against a live server recently.

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
| `runtime/conf/datatypes.xml`, `syntax.xml` | Column types and SQL syntax highlighting |

Profiles are normally created in the connection dialog. When you choose a server type the default port and user name are filled in (MySQL 3306 / `root`, PostgreSQL 5432 / `postgres`, SQL Server 1433 / `sa`, Oracle 1521 / `system`).

The **Database(s)** field of a profile is an optional comma separated filter. Leave it empty to list every database on the server. For PostgreSQL the first name is also the database the connection is made to, with `postgres` as the default.

Note that `profiles.xml` stores passwords in plain text. Keep local changes out of version control, for example with `git update-index --skip-worktree runtime/conf/profiles.xml`.

## Logging

The application logs with Log4j 2. The configuration is `src/main/resources/log4j2.xml`: messages go to the console and to the output panel. Executed queries are logged at `debug` level, set the `nl.errorsoft.esql.data` logger to `info` to hide them.

## Try it with a local PostgreSQL

```sh
docker run -d --name esql-pg -e POSTGRES_PASSWORD=test -p 5432:5432 postgres:17
```

Start the application, choose **PostgreSQL** as server type, and connect with user `postgres`, password `test` and port `5432`.

## Project layout

```
src/main/java/nl/errorsoft/esql
  control/   use-case controllers
  data/      the database connection
  domain/    model classes, including dialect/ with the per-database behaviour
  gui/       Swing windows and components
  dbcreator/ the database model designer
src/main/resources   icons, images, help pages and log4j2.xml
runtime/             configuration the application reads and writes
```

## Credits

Programming and design by S. Oudmaijer and J. Walgemoed (Errorsoft), 2002 to 2003.
