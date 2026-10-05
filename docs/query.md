# SQL query

[Back to the help index](index.md)

New query (in the context menu of the server or a database) or **Run SQL query** in the toolbar opens a query tab: "Query", "Query 2" and so on. Each tab has its own editor and result, and can be closed with its close button.

The editor highlights SQL, shows line numbers and has undo. The toolbar of the tab chooses the database the statements run against, and opens and saves `.sql` files.

![A query tab with a result and the completion of the columns of products](query.png)

## Running statements

| Shortcut | Action |
|---|---|
| Cmd+Enter (Ctrl+Enter on Windows and Linux) | Run the selection, or the statement at the caret |
| Cmd+Shift+Enter (Ctrl+Shift+Enter) | Run all statements of the script in order |

Statements are separated by `;`. A semicolon inside quotes, dollar quotes or comments does not end a statement. Running stops at the first statement that fails and names it in the error message. A query shows its rows in the result below the editor, other statements report how many rows they changed.

## Completion

| Shortcut | Action |
|---|---|
| Ctrl+Space | Open the completion |
| Cmd+Space | Open the completion (macOS, when Spotlight does not use it) |
| Cmd+Shift+Space (Ctrl+Shift+Space) | Open the completion |
| `.` after a table or alias | Show its columns |

The completion offers SQL keywords, the tables of the chosen database and their columns. It knows what fits at the caret: tables after `FROM` and `JOIN`, columns of a table or alias after a period. Aliases such as `o` in `FROM orders o` are resolved. Names that need it are quoted for the server.

macOS gives Cmd+Space to Spotlight. To use it for completion, turn off that shortcut in System Settings > Keyboard > Keyboard Shortcuts > Spotlight.
