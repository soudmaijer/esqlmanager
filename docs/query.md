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

## Explain

**Explain** (the list button next to Run, Cmd+E or Ctrl+E) shows the plan the server chose for the selection, or the statement at the caret, without running it. **Explain analyze** (the gauge button) runs the statement and shows the plan with what was measured: actual rows, loops and times. Because it runs the statement, its changes stay: for anything other than a SELECT (or a WITH of selects) it asks "Analyze runs this statement and its changes stay. Run it?" first, with **Run and analyze** next to Cancel. MySQL measures queries only; its analyze of an INSERT, UPDATE or DELETE shows the plan without times. Explain works on PostgreSQL and MySQL 8; on other servers the buttons are not shown. Every result tab also has Explain and Explain analyze buttons next to the pencil: they explain the statement that produced that result, in the schema it ran in, and the plan replaces the plan that result showed before (they are disabled while another statement runs, or when the tab is on another database than the result).

The plan opens as a result tab, "Explain: ..." or "Analyze: ...", with two tabs:

* **Plan**: the steps as a tree, the step that produces the result at the top and the steps it reads from below it. Click the arrow (or double click a row) to fold a step. The columns are the **Rows** (estimated, and after analyze "estimated / actual" per loop), the **Loops**, the **Cost** the planner estimated (in the server's units, the step and the steps below it), the **Time** (all loops together, the steps below included) and **% of total**, the part of the plan's time (after analyze) or cost (otherwise) spent in that step itself, as a bar. The steps that take the most, at most three each with a fifth or more of the total, have their time and bar in yellow. The panel on the right shows the selected step: table, index, filter and join conditions, buffers found in the cache (hit) and read from disk, how far the estimate was off, and everything else the server said about it. The line below the plan shows the planning time, the execution time and the total cost.
* **Text**: the plan as the server gave it (JSON, or MySQL's tree after analyze), with **Copy plan**.

A yellow triangle marks a step with a hint; hover over it, or select the step, to read it. The hints state a fact from the plan that often explains a slow statement:

| Hint | Meaning |
|---|---|
| Sequential scan of orders reading about 65,000 rows, with a filter | PostgreSQL reads the whole table (more than 10,000 rows) and drops the rows that do not match. |
| Full table scan of orders, about 2,000 rows | MySQL reads every row of the table (access type ALL). |
| Estimated 120 rows, got 98,000 | The planner's estimate was off by more than 10 times, so it may have chosen the wrong plan. Statistics that are out of date are a common reason. |
| Sort used the disk (external merge, 12,000 kB) | A sort did not fit in memory (`work_mem` on PostgreSQL). |
| Hash used the disk in 4 batches | A hash join or aggregate did not fit in memory. |
| Nested loop runs Index Scan 5,000 times | The inner side of a nested loop ran more than 1,000 times. |

![The plan of a join with a filter, a grouping and a sort, analyzed on PostgreSQL](explain.png)

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
