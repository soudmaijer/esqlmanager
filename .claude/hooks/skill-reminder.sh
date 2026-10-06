#!/usr/bin/env bash
# PostToolUse hook: after editing UI or layered Java code, remind Claude to apply
# the esql-ui / esql-architecture rules. Fires at most ONCE PER SESSION per
# category (ui / layers), tracked via a marker file keyed to the session id.
# Never blocks: always exits 0 and only emits an additionalContext reminder.
#
# Reads the PostToolUse hook payload (JSON) on stdin.

input=$(cat)
f=$(printf '%s' "$input" | jq -r '.tool_response.filePath // .tool_input.file_path // ""' 2>/dev/null)
sid=$(printf '%s' "$input" | jq -r '.session_id // "nosession"' 2>/dev/null)

[ -n "$f" ] || exit 0

category=""
msg=""
case "$f" in
  */target/* | */docs/*)
    exit 0
    ;;
  */src/main/java/*/ui/*.java | */src/main/java/*/ui/*/*.java | */ui/dialog/*.java)
    category="ui"
    msg="UI check: you edited ${f}. This session, verify UI changes follow the esql-ui skill (U1-U9): dialogs only through ui.dialog.Dialogs and FormDialog, Forms layouts with 12px padding (no null layouts), colours from UIManager, icons through imageLoader(), sentence case wording with the action as primary button, no success popup when the status bar shows it, context menus built on open, editors as EditorTab tabs, Swing on the event thread, disabled buttons with a tooltip saying what is needed and unsupported ones hidden (TreeMenu.missing/supported), and check the result by painting in-process. Fix violations now rather than deferring to /review."
    ;;
  */src/main/java/*/control/*.java | */src/main/java/*/dialect/*.java | */src/main/java/*/dialect/*/*.java | *Service.java | *Repository.java)
    category="layers"
    msg="Architecture check: you edited ${f}. This session, verify the change follows the esql-architecture skill (A1-A8): UI -> Controller -> Service -> Repository with no Swing outside ui and no JDBC outside repositories, no server type branches outside dialect (a dialect never runs SQL), dialect.quote and dialect.literal for SQL, services from ConnectionContext, services throw and controllers report through errors().report, virtual threads and try-with-resources, records for parameter objects, log4j2 only. New classes follow the <Subject><Role> naming (N1-N6). Fix violations now rather than deferring to /review."
    ;;
  *)
    exit 0
    ;;
esac

marker="${TMPDIR:-/tmp}/claude-esql-skill-reminder-${sid}-${category}"
if [ -f "$marker" ]; then
  exit 0
fi
touch "$marker" 2>/dev/null || true

jq -cn --arg m "$msg" '{hookSpecificOutput:{hookEventName:"PostToolUse",additionalContext:$m}}'
exit 0
