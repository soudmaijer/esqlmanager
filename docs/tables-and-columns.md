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

* **Create table** (toolbar, or the context menu of a database) asks for a name and the columns: type, length, default, not null, auto increment, a comment per column (MySQL and PostgreSQL), and primary key, unique and index flags. **Show SQL** at the bottom of the tab shows the statements that Save will run and keeps them up to date while you type.
* **Edit table** renames a table and changes its comment and, on MySQL, its storage engine. Show SQL lists the statements for the changes made so far.
* **Rename table** and **Duplicate table** are in the context menu of a table. Duplicate copies the structure (columns, defaults, indexes) to a new table next to it and, when you tick "Copy the data too", the rows; the auto numbering of the copy continues after the copied rows. The new name must not exist yet.
* **Properties** (context menu of a table or a database) shows a read-only summary: for a table its name, database, schema, engine or type, comment, number of rows and columns and its size on disk; for a database its character set and collation (MySQL) or owner and encoding (PostgreSQL) and its number of tables.
* **Create database** (context menu of the server) checks the name against the databases of the server, with the exact case as the server compares it (on PostgreSQL `Shop` can be created next to `shop`), and offers the options of the server: character set and collation on MySQL, owner and encoding on PostgreSQL. A choice left on "(server default)" is not written.
* **Rename schema** and **Drop schema** (PostgreSQL) are in the context menu of a schema, **Create schema** in the one of a database.
* **Indexes** manages the primary key, unique indexes, plain indexes and (MySQL) fulltext indexes.

These three open as a tab of the connection window, one per table; opening one again shows the open tab. Save applies the change (Create table and Edit table then close their tab, the indexes tab stays open), Cancel or the close button of the tab asks before discarding unsaved changes.
* **Empty table** deletes every row, **Drop table** removes the table. Both ask first.
* Maintenance: Optimize and Analyze table, and on MySQL Check and Repair table. On PostgreSQL optimize runs `VACUUM`.

## Columns

Add field, Edit field and Drop field are in the toolbar and in the context menu of a table or column.

## Export and import

* **Export** (context menu of a database, schema or table) writes the structure, the data or both as an SQL script. The export window opens at the node that was selected; on PostgreSQL its tree shows the schemas of a database and the tables of a schema. Exporting a PostgreSQL database takes every schema in it. The script names each table as `schema.table` and creates its schema when it is missing, so importing it restores the tables into the schema they came from. On servers that cannot switch database in SQL the script uses `\connect`. Options:
  * Content: structure, data, and the views of the selected databases or schemas (written after their tables).
  * Statements: create database, drop table, use database, `DROP ... IF EXISTS` (on by default) and `CREATE ... IF NOT EXISTS` (off by default).
  * Script: wrap the script of each database in a transaction (`BEGIN`/`COMMIT`, `START TRANSACTION` on MySQL), and disable foreign key checks while the script runs (MySQL only: PostgreSQL has no way to do that without superuser rights, so the box is disabled there).
  * Format: the number of rows per `INSERT` (1 writes an `INSERT` per row) and the file encoding (UTF-8 by default, also UTF-16, ISO-8859-1, windows-1252 and US-ASCII). A file name that ends in `.gz` is written gzip compressed.
* **Import** (context menu of the server, a database or a schema) runs an SQL script (also a `.gz` file) against the selected database. On PostgreSQL a schema can be selected: table names the script does not qualify go into that schema, and the connection is set back to its previous schema afterwards. Options: stop on the first error or continue and list the failed statements when the import ends, run in a single transaction (everything is rolled back when a statement fails or you cancel; MySQL commits implicitly at every `CREATE` and `DROP`, so only data statements are undone there; a transaction always stops at the first error), and the file encoding. An error names the statement that failed.
* The progress window of an export or import shows the table or the number of statements done and has a **Cancel** button (which becomes Close). Cancelling an export deletes the partial file, cancelling an import says how many statements ran and whether they were rolled back. The upload and download window for a binary cell can be cancelled as well; a partial download is removed.
* The windows start in the default folder of Settings > Preferences and use its default encoding.

## Users and server

* **Users** (server context menu or toolbar) manages accounts, passwords and privileges per server, database and table. On PostgreSQL accounts are roles.
* **Process list** shows the connections to the server (its own included) with their running queries, and can end one. **Pause** stops the refreshing, **Hide idle** leaves out the connections that run nothing, and the interval can be 1, 2, 5 or 10 seconds. Double click a row (or Show query) opens the full statement with SQL colouring and a Copy button. **Show status** and **Show variables** list the server's status values and settings.
