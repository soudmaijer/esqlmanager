# eSQLManager help

eSQLManager is a database manager for MySQL and PostgreSQL. SQL Server and Oracle can be browsed. Every connection opens in its own window: the database tree on the left, tabs with table data and queries on the right, and the output panel with the log at the bottom.

![The connection window with the products table of a PostgreSQL database](screenshot.png)

## Pages

* [Getting started](getting-started.md): connection profiles, the connection window and the status bars.
* [Tables and columns](tables-and-columns.md): browsing, editing data, creating and changing tables, indexes, export and import.
* [SQL query](query.md): the query tab, shortcuts and completion.
* [Database designer](designer.md): drawing a model, foreign keys, opening an existing database, PlantUML and Mermaid export.
* [Settings](settings.md): appearance, JDBC drivers and the files in `conf/`.

## What works on which server

Each server has a dialect that decides how a feature is carried out. Menus only show what the server of the connection supports.

| Feature | MySQL | PostgreSQL | SQL Server | Oracle |
|---|---|---|---|---|
| Browse databases, tables and columns, view and edit data | yes | yes | yes | yes |
| SQL query tabs with completion | yes | yes | yes | yes |
| Create and drop databases | yes | yes | not verified | no |
| Create and change tables, columns and indexes | yes | yes | no | no |
| Export and import as SQL (PostgreSQL: per schema) | yes | yes | no | no |
| Database designer, foreign keys, open in designer | yes | yes | no | no |
| Users and privileges | yes | yes (roles) | no | no |
| Process list, server status and variables | yes | yes | no | no |
| Table maintenance | optimize, analyze, check, repair | VACUUM, ANALYZE | no | no |

A PostgreSQL connection is made to one database. Opening another database in the tree reconnects and shows its schemas (`public` and any others); each schema holds its tables. Known gaps with schemas: the user manager grants table privileges without a schema, and Open in designer on a database node reads its current schema (use the schema node for another schema).
