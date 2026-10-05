#!/usr/bin/env bash
# Stop hook: advisory only. When Java sources changed but changelog.txt did not,
# remind Claude to finish the change (changelog, README/docs, screenshots).
# Never blocks, always exits 0. Does not run Maven.

cd "${CLAUDE_PROJECT_DIR:-.}" 2>/dev/null || exit 0
git rev-parse --git-dir >/dev/null 2>&1 || exit 0

changed=$( { git diff --name-only HEAD 2>/dev/null; git status --porcelain -uall 2>/dev/null | sed 's/^...//'; } | sort -u)

printf '%s\n' "$changed" | grep -Eq '^src/.*\.java$' || exit 0
printf '%s\n' "$changed" | grep -Fxq 'changelog.txt' && exit 0

echo "Reminder: Java sources changed but changelog.txt did not. Add a newest-first entry to changelog.txt, update README.md and docs/ if behaviour changed, and for UI changes retake docs/screenshot.png, docs/designer.png, docs/query.png and docs/connect.png (unless this is an internal change with nothing user visible)."
exit 0
