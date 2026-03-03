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


## Python Environment

**CRITICAL**: This workspace has a Python virtual environment at `.venv/`

### Virtual Environment Usage

1. **Always use the existing .venv**
   - Virtual environment is located at `.venv/` in the workspace root
   - All Python dependencies are installed in this venv
   - DO NOT create a new virtual environment

2. **Activating the venv in Git Bash**
   ```bash
   source .venv/Scripts/activate
   ```

3. **Running Python scripts**
   ```bash
   # Activate venv first
   source .venv/Scripts/activate
   
   # Then run scripts
   python script_name.py
   ```

4. **Installing new dependencies**
   ```bash
   source .venv/Scripts/activate
   pip install package_name
   ```

### Notes

- This configuration applies to the current workspace
- All command execution should assume Git Bash environment
- If a command fails unexpectedly, check if the first character was dropped and retry with a space prefix
- Always use `.venv/` for Python operations - it contains all required dependencies
