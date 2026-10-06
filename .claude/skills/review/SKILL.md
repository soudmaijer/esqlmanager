---
name: review
description: Review eSQLManager changes against the esql-architecture and esql-ui rules. Delegates to parallel code-reviewer agents. Accepts a path or --all. Makes no edits.
---

# Review

Orchestrator that delegates review to `code-reviewer` agents. It never edits files.

**Input**: `$ARGUMENTS`, an optional path or `--all`.

**Steps**

1. **Determine scope**
   - `--all`: every `.java` file under `src/main/java`
   - `<path>`: `.java` files under the path
   - no arguments: the files changed since the last pushed ref. Find the base with `git rev-parse --abbrev-ref @{upstream}` (fall back to `origin/master`), then `git diff --name-only <base>...HEAD -- '*.java'`, plus uncommitted changes from `git status --porcelain`.

   Skip `target/`. If no files are found, report "No files in scope" and stop.

2. **Group by feature package**

   Group files by the feature package below `nl/errorsoft/esql/` (`table`, `database`, `designer`, `query`, `connection`, `exporter`/`importer`, `user`, `server`, `app`/`settings`, shared `ui`/`jdbc`/`dialect`/`error`/`job`). Split a group with more than about 15 files. Fewer than 15 files in total: one agent.

3. **Spawn `code-reviewer` agents in parallel** (several Agent calls in one message), each with:

   > Review these files. Apply `esql-architecture` (A1-A8, N1-N6) to all, and `esql-ui` (U1-U9) to ui, dialog and controller files. Report findings with file:line, rule id and failure scenario, and list naming problems in untouched classes under "Rename debt".
   >
   > Files: <list>

4. **Merge**

   Combine the reports, dedupe by root cause (across agents too), group repeated patterns, rank by severity (violation, warning, note). Present the findings, then the separate "Rename debt" list, then a summary table.

   Do not fix anything and do not run Maven, tests or the formatter. If the user asks for fixes afterwards, that is a separate step.
