# Tables and columns

[Back to the help index](index.md)

## Viewing data

Double click a table in the tree (or choose Open in its context menu) to show its rows. The data is shown a page at a time; the paging buttons are in the bar below the tabs. Click a column header to sort. Paging orders by the primary key, so rows keep their place after an update.

## Editing data

* Click a cell and type to change it, then press **Update changes** in the toolbar. An empty cell is stored as `NULL`.
* **Insert new row** adds an empty row to fill in, **Delete row** removes the selected rows after asking.
* Binary columns (blobs) can be uploaded from a file and saved to a file.

A row can only be changed or deleted when it can be identified, by its primary key or else by all its values.

## Tables

* **Create table** (toolbar, or the context menu of a database) asks for a name and the columns: type, length, default, not null, auto increment, and primary key, unique and index flags.
* **Edit table** renames a table and changes its comment and, on MySQL, its storage engine.
* **Indexes** manages the primary key, unique indexes, plain indexes and (MySQL) fulltext indexes.

These three open as a tab of the connection window, one per table; opening one again shows the open tab. Save applies the change (Create table and Edit table then close their tab, the indexes tab stays open), Cancel or the close button of the tab asks before discarding unsaved changes.
* **Empty table** deletes every row, **Drop table** removes the table. Both ask first.
* Maintenance: Optimize and Analyze table, and on MySQL Check and Repair table. On PostgreSQL optimize runs `VACUUM`.

## Columns

Add field, Edit field and Drop field are in the toolbar and in the context menu of a table or column.

## Export and import

* **Export** (context menu of a database, schema or table) writes the structure, the data or both as an SQL script. The export window opens at the node that was selected; on PostgreSQL its tree shows the schemas of a database and the tables of a schema. Exporting a PostgreSQL database takes every schema in it. The script names each table as `schema.table` and creates its schema when it is missing, so importing it restores the tables into the schema they came from. On servers that cannot switch database in SQL the script uses `\connect`.
* **Import** (context menu of the server, a database or a schema) runs an SQL script against the selected database. On PostgreSQL a schema can be selected: table names the script does not qualify go into that schema, and the connection is set back to its previous schema afterwards. A progress window shows how far it is.

## Users and server

* **Users** (server context menu or toolbar) manages accounts, passwords and privileges per server, database and table. On PostgreSQL accounts are roles.
* **Process list** shows the running queries and can end one. **Show status** and **Show variables** list the server's status values and settings.
