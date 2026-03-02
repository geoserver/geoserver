---
inclusion: always
---

# Development Environment Configuration

## Shell Environment

**CRITICAL**: This development environment uses Git Bash, not PowerShell.

### Shell Command Requirements

1. **Use Git Bash syntax** for all shell commands
   - PowerShell commands do NOT work in this environment
   - Use Unix-style commands (ls, cat, grep, etc.)
   - Use forward slashes for paths where possible

2. **Command Prefix Workaround**
   - There is a known VSCode-fork bug that sometimes drops the first character of commands
   - **WORKAROUND**: Prefix commands with a space if the first character is lost
   - Example: If `hugo server` becomes `ugo server`, use ` hugo server` (with leading space)

### Examples

**Correct (Git Bash)**:
```bash
hugo server
npm install
ls -la
cat file.txt
```

**Incorrect (PowerShell - DO NOT USE)**:
```powershell
Get-ChildItem
Remove-Item
```


### Notes

- This configuration applies to the current workspace
- All command execution should assume Git Bash environment
- If a command fails unexpectedly, check if the first character was dropped and retry with a space prefix
