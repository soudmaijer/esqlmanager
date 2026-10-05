# Getting started

[Back to the help index](index.md)

## Connecting

Options > Connect (or the Connect button of the toolbar) opens the connection dialog. A profile holds:

| Field | Meaning |
|---|---|
| Server type | MySQL, PostgreSQL, SQL Server or Oracle. Choosing one fills in the default port and user (MySQL 3306 / `root`, PostgreSQL 5432 / `postgres`). |
| Host and port | Where the server runs. |
| User and password | The account to log in with. |
| Database(s) | Optional, a comma separated list of the databases to show. Empty shows every database. On PostgreSQL the first name is the database to connect to, `postgres` when it is empty. |
| Auto connect | Connect with this profile when the application starts. |

Profiles are saved in `conf/profiles.xml`. Passwords are stored there in plain text, so only save one on a machine you trust.

Several connections can be open at the same time. Window > Tile horizontal, Tile vertical and Cascade arrange their windows.

## The connection window

* **Tree** on the left: the server, its databases, their tables and the columns of each table. On PostgreSQL a database holds schemas, which hold the tables. Double click a database to list its tables, double click a table to open its data. Right click any node for its context menu, which only lists what the server supports:
  * server: Create database, New query, Users, Process list, Show status, Show variables, Reload databases;
  * database: Open, Create table, Open in designer, Export, Import, Drop database, Reload tables (PostgreSQL: Create schema and Reload schemas);
  * schema (PostgreSQL): New query, Create table, Open in designer, Export, Import, Drop schema, Reload tables;
  * table: Open, Edit table, Indexes, Add field, Export, the maintenance commands, Empty table, Drop table, Reload columns;
  * column: Add field, Edit field, Drop field.
* **Tabs** on the right: the table list or the table data in the first tab, query tabs ("Query", "Query 2", ...) and this help. Every tab has a close button.
* **Toolbar**: create and drop a table, add and delete a field, insert, update and delete a row, run an SQL query, open the database in the designer, the user manager and refresh the tree. Buttons are enabled when they apply to what is selected.
* **Output panel** at the bottom: the log of the connection, with the server version and driver and every executed statement.

Closing the window asks "Disconnect from ...?" first.

## Status bars

* The bar below the tabs shows what this connection did last, for example how many rows were loaded and how long it took. While the table data is in front, the paging buttons are on its left.
* The status bar of the application shows a light for the state of the active connection and, on the right, its server and account.
