# SQL query

[Back to the help index](index.md)

**New query** (Tools > New query, Ctrl/Cmd+T, the explorer toolbar, or the context menu of a database or schema) opens a query tab: "Query", "Query 2" and so on. The command is enabled when a database, or a schema, table or column inside one, is selected in the explorer, and the tab starts on that database. Each tab has its own editor and result, and can be closed with its close button.

The editor highlights SQL, shows line numbers and has undo; its font size is set in Settings > Preferences. The toolbar of the tab chooses the database the statements run against, and opens and saves `.sql` files. On PostgreSQL a schema picker sits next to the database: plain table names resolve to the chosen schema (it sets the `search_path`), write `schema.table` for the others. The result line shows the database and schema a statement ran on.

![A query tab with a result and the completion of the columns of products](query.png)

## Running statements

| Shortcut | Action |
|---|---|
| Cmd+Enter (Ctrl+Enter on Windows and Linux) | Run the selection, or the statement at the caret |
| Cmd+Shift+Enter (Ctrl+Shift+Enter) | Run all statements of the script in order |

Statements are separated by `;`. A semicolon inside quotes, dollar quotes or comments does not end a statement. Statements run in the background: the run buttons are disabled and the bar below the tabs says "Running..." until they are done, so the rest of the application stays usable. Running stops at the first statement that fails and names it in the error message. Every statement that returns rows (SELECT, WITH, EXPLAIN, SHOW, VALUES, ...) shows them in the result below the editor, other statements report how many rows they changed. `USE shop` (MySQL) or `\connect shop` / `\c shop` (PostgreSQL) switches the database the statements run against. On PostgreSQL `USE` is not a switch and goes to the server as written.

## Completion

| Shortcut | Action |
|---|---|
| Ctrl+Space | Open the completion |
| Cmd+Space | Open the completion (macOS, when Spotlight does not use it) |
| Cmd+Shift+Space (Ctrl+Shift+Space) | Open the completion |
| `.` after a table or alias | Show its columns |

The completion offers SQL keywords, the tables of the chosen database and their columns. It knows what fits at the caret: tables after `FROM` and `JOIN`, columns of a table or alias after a period. Aliases such as `o` in `FROM orders o` are resolved. On PostgreSQL the tables of every schema of the database are known: a plain name is a table of the chosen schema (the first of the search path), other schemas are offered after `FROM` and `sales.` lists the tables of `sales`; `sales.orders` resolves for the column completion too. Names that need it are quoted for the server.

macOS gives Cmd+Space to Spotlight. To use it for completion, turn off that shortcut in System Settings > Keyboard > Keyboard Shortcuts > Spotlight.

## Find

Cmd+F (Ctrl+F on Windows and Linux) in the editor opens a find bar below it, the same as in the output panel: every match is highlighted, "2 of 5" or "No results" says where you are, Enter and Shift+Enter go to the next and previous match, **Match case** narrows the search and Esc closes the bar and returns to the editor.
