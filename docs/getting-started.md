# Getting started

[Back to the help index](index.md)

## Connecting

Options > Connect (or the Connect button at the left of the explorer toolbar, with no saved profile selected) opens the connection dialog. At the left is the list of saved profiles with three buttons above it: **+** makes a new profile (it is in the list as "New profile" until you save it), **-** removes the selected profile after asking, and the copy button duplicates it as "<name> copy" (no auto connect). Select a profile to edit it; when it has unsaved changes you are asked to save or discard them before another profile is shown. At the right are the **Name** of the profile and two tabs. The **Connection** tab holds the settings of a profile:

![The connection dialog with a saved profile](connect.png)

| Field | Meaning |
|---|---|
| Server type | MySQL, PostgreSQL, SQL Server or Oracle. Choosing one fills in the default port and user (MySQL 3306 / `root`, PostgreSQL 5432 / `postgres`). |
| Host and port | Where the server runs. |
| User and password | The account to log in with. |
| Save password | On by default. When it is off the password is not written to `conf/profiles.xml` and is asked for when you connect (also for auto connect). |
| Auto connect | Connect with this profile when the application starts. |

**Test connection** connects with the values in the form (the saved profile is not used) and shows "Connected to <server> <version>" in green or the error in red next to the button, without opening a window. A long message is cut off; hover over it to read all of it.

The first time you test or connect to a MySQL or Oracle server, eSQLManager asks to download its JDBC driver: these drivers are not included because of their licences (see [JDBC drivers](settings.md#jdbc-drivers)). PostgreSQL and SQL Server work without a download.

The **Databases and schemas** tab is available after a successful test. It lists the databases of the server with a checkbox (click the box or press the space bar); on PostgreSQL a database opens to show its schemas, which are loaded when you open it. Tick what the profile should show: nothing ticked shows everything, a ticked database without ticked schemas shows all its schemas, ticking a schema ticks its database, and unticking a database forgets its schemas. The first ticked database is the one the connection is made to (`postgres` when nothing is ticked). **Select none** clears the ticks, **Reload** lists the databases again. Changing the host, port, user, password or server type disables the tab until you test again; the ticks are kept. A ticked database that no longer exists is dropped when you save. Schemas that are not ticked are left out of the tree, the export and import windows, the query tab and the designer, but exporting a whole database still includes them.

**Save** keeps the profile in the list (also when you changed its name), **Connect** opens a connection with the values in the form and **Close** closes the dialog.

Profiles are saved in `conf/profiles.xml`. A saved password is stored there in plain text, so only save one on a machine you trust, or turn Save password off.

Several connections can be open at the same time. They all appear in the explorer on the left, each under the name of its profile, so two connections to the same server as the same user are told apart. Saved profiles that are not connected are listed below them in grey: expand one with its arrow, double click it, press the Connect button of the explorer toolbar or choose Connect in its context menu to connect, Edit connection opens the profile dialog on it. When there are no connections and no saved profiles yet, the explorer shows a **New connection...** button that opens the connection dialog.

## The main window

* **Explorer** on the left: every connection with its databases, their tables and the columns of each table. On PostgreSQL a database holds schemas, which hold the tables. Selecting a node only selects it: what is below a database, schema or table is loaded in the background when you expand it (its arrow, a double click, or the right arrow key), with "Loading..." in the meantime; on PostgreSQL expanding a database connects to it. A load that fails is reported and the node folds up again. A column shows its type in grey after its name (`id  integer`, `name  varchar(100)`), a key icon for the primary key and a link icon for a foreign key; hover over it for not null, default and auto increment. Double click a table (or press Enter) to open its data (its columns expand with the arrow); a double click on a database, schema or connection folds it open or closed. **Show tables** in the context menu of a database or schema (or Enter on it) opens the list of its tables. Disconnect is in the context menu of a connection. Right click any node for its context menu, which only lists what the server supports:
  * connection: Create database, Users, Process list, Show status, Show variables, Reload databases, Disconnect;
  * saved profile: Connect, Edit connection;
  * database: Show tables, New query, Create table, Open in designer, Export, Import, Drop database, Properties, Reload tables (PostgreSQL: Create schema and Reload schemas);
  * schema (PostgreSQL): Show tables, New query, Create table, Open in designer, Export, Import, Rename schema, Drop schema, Reload tables;
  * table: Open, Edit table, Indexes, Add field, Rename table, Duplicate table, Export, the maintenance commands, Empty table, Drop table, Properties, Reload columns;
  * column: Add field, Edit field, Drop field.
* The **explorer toolbar** reloads the databases, opens a query (also Tools > New query, Ctrl/Cmd+T), the user manager and the designer for the connection of the selected node. Buttons are enabled when they apply to what is selected. Creating and dropping tables and columns is in the context menus of the tree.
* **Buttons and menus** follow what is selected. A button or menu item that cannot run yet is greyed out and its tooltip says what is needed ("New query: select a database first."); one that the server never offers (Users on SQL Server, Check table on PostgreSQL) is not shown at all.
* **Work tabs** on the right: every table list, table data, query ("Query", "Query 2", ...), table editor, indexes, designer and the help is a tab of its own with the logo of its server. When two tabs have the same title, the profile name of the connection is added: "orders (local pg)". Click a tab to bring it to the front, or use Window > Next window and Previous window (Cmd/Ctrl+Shift+] and [); the Window menu lists the open tabs grouped by connection. The cross on a tab (or a middle click) closes it; an editor with changes asks first, a designer asks to save its model.
* The table data has a **toolbar at its top** to insert, update and delete rows.
* **Output panel** at the bottom, across the whole width, with a tab per log: **Application** for what belongs to no connection (start up, settings, drivers, a connection that failed to open) and one tab per open connection, with its profile name and server logo, for its server version and driver, every executed statement and its errors. A connection's tab appears when it connects and goes when it disconnects; selecting a node or tab of a connection brings its log to the front. Press Cmd+F (Ctrl+F on Windows and Linux) in a log to find text: the find bar at the bottom highlights every match and shows "2 of 5" or "No results"; Enter finds the next match, Shift+Enter the previous one, **Match case** narrows it and Esc closes the bar.

**Disconnect** (toolbar, Options menu, or the context menu of a connection) first asks each editor of the connection with unsaved changes, then "Disconnect from <profile>? N tabs will close.". The tabs of the connection close; its designers stay open, and Generate then asks for another open connection to the same kind of server.

## Status bars

* The bar at the bottom of a tab shows its last message: on the table data how many rows were loaded and how long it took, on a query tab the outcome of its last run. A tab without a message of its own, such as a fresh query tab, leaves it empty. On the table data the paging buttons are in the same bar. What happens in the explorer (tables listed, a model opened in the designer) is in the output panel.
* The status bar of the application shows a light for the state and, on the right, the server and account of the connection of the tab in front or of the node selected in the explorer.
