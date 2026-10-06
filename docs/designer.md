# Database designer

[Back to the help index](index.md)

Tools > Database designer draws a model of databases, tables and notes, and generates it on a MySQL or PostgreSQL server. The designer opens as a window on the desktop of eSQLManager, with a tab of its own next to the tabs of the connections, so you can switch between a connection and its models. Each model has its own designer window. Closing it asks to save the model first. Disconnecting keeps the designer open without a connection; Generate then asks which open connection to the same kind of server to use.

![The model designer with the shop database and its tables](designer.png)

## Drawing a model

* Right click the canvas to add a database, a table or a note at that spot, to select all, to arrange the model or to show the grid.
* Right click a card for what applies to it: properties, add a foreign key, attach, remove.
* Double click a table to edit its name, columns and foreign keys. The properties of a table, a database and the model (Model properties) refuse an empty name, a database name that another database of the model has, and a table name that another table in the same database has (tables that are linked to no database count as one group). The author of the model can be edited at any time.
* Shift-drag from a table or a note to a database to link them.
* Tables are cards with an icon per column: a key for the primary key, a link for a foreign key column. The colours follow the light or dark appearance.

## Menus and shortcuts

| Menu | Items |
|---|---|
| File | New model (Cmd+N, Ctrl+N elsewhere), Open model (Cmd+O), Save model (Cmd+S), Save model as, Export as PlantUML, Export as Mermaid, Close |
| Edit | Delete selected (Delete, asks first), Select all (Cmd+A), Deselect all (Cmd+D) |
| View | Show grid, Arrange automatically |
| Model | Add database (F1), Add table (F2), Add note (F3), Attach table (F4), Attach note (F5), Show object properties, Show model properties |

Delete removes the selected connector, or else the selected cards, after asking for confirmation. Attach and Show object properties work on the selection however it was made, also after Select all. The generate button of the toolbar ("Generate model in database") opens the generate dialog.

## Foreign keys

* Drag from the icon of a column onto a column of another table, use "Add foreign key..." in a table's context menu, or the Foreign keys tab of its properties. Changes in that tab reach the model when the properties are saved; Cancel discards them.
* The dialog takes one or more column pairs, a name (default `fk_<table>_<column>`) and the ON DELETE and ON UPDATE actions. It checks that the columns exist and their types fit, and refuses a key between tables of two different database cards: "A foreign key can only link tables of the same database." Tables linked to no database card count as one database.
* A foreign key is drawn from column to column, with a crow's foot at the many side and a double bar at the referenced table. Double click a line to edit it, select it and press Delete to remove it.
* On MySQL foreign keys need InnoDB tables.

## Opening an existing database

"Open in designer" in the context menu of a database (or the designer button of the toolbar) reads every table of that database, views left out, with its columns, primary key, indexes and the foreign keys between them, into a new model. On PostgreSQL the database reads its current schema (normally `public`) and a schema node reads that schema; generating the model creates the tables in the current schema. The tables are placed automatically: a referenced table left of the tables that refer to it, tables without relations in a grid below. View > Arrange automatically does the same for any model.

## Saving and generating

* File > Save model writes the model to an `.edm` file, Open model reads it back.
* A model remembers the kind of server it is designed for (MySQL, PostgreSQL): a new model takes the server of the connection the designer was opened from, "Open in designer" that of the database it reads. It is saved in the `.edm` file; a model saved by an older version has none and takes the server of the first connection it is generated on.
* Generating a model creates its databases, tables and foreign keys on the server. Tables and keys that exist are skipped, so a model can be generated again after adding to it. A model is only generated on a connection to its own kind of server: the designer's connection when it matches, otherwise Generate asks which open connection of that kind to use, and says so when there is none.
* Each database card is a database on the server, also on PostgreSQL: generating creates the database when it is missing and the tables in its current schema (normally `public`). The menus and messages take the server's word for it from the model.

## PlantUML and Mermaid

File > Export as PlantUML... and Export as Mermaid... write the model as an ER diagram text, for documentation or a README. For example in Mermaid:

```
erDiagram
    customers {
        int id PK
        varchar(100) name
    }
    orders {
        int id PK
        int customer_id FK
    }
    customers ||--o{ orders : "fk_orders_customer_id"
```
