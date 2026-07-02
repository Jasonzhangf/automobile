package proto

// Command name constants — single source of truth for all daemon ↔ device commands.
// Using constants prevents typos, enables IDE autocomplete, and allows
// centralized validation via ValidateCommandName.
//
// Layout: Core ops | Navigate | Observe | Collect | Admin | Device
const (
	// Core operations
	CmdPing             = "ping"
	CmdTap              = "tap"
	CmdScroll           = "scroll"
	CmdBack             = "press-key" // back = press-key with keyCode=4
	CmdPressKey         = "press-key"
	CmdInputText        = "input-text"

	// Observe
	CmdDumpUiTreeRoot   = "dump-ui-tree-root"
	CmdCaptureScreenshot = "capture-screenshot"

	// Navigation
	CmdOpenDeepLink     = "open-deep-link"

	// Collect / analysis
	CmdToggleAction     = "toggle"
	CmdCommentLike      = "comment-like"

	// Admin / device control
	CmdStartDaemon      = "start-daemon"
	CmdStopDaemon       = "stop-daemon"
	CmdAppendLog        = "append-log"
	CmdFetchLogs        = "fetch-logs"
	CmdReadLogTail      = "read-log-tail"
)

// AllCommands is the canonical list of valid command names.
// Used for validation in proto/validate.go.
var AllCommands = []string{
	CmdPing,
	CmdTap,
	CmdScroll,
	CmdPressKey,
	CmdInputText,
	CmdDumpUiTreeRoot,
	CmdCaptureScreenshot,
	CmdOpenDeepLink,
	CmdToggleAction,
	CmdCommentLike,
	CmdStartDaemon,
	CmdStopDaemon,
	CmdAppendLog,
	CmdFetchLogs,
	CmdReadLogTail,
}
