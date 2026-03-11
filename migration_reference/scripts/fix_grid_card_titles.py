#!/usr/bin/env python3
"""
Fix malformed grid card titles in index.md files.

The conversion tool creates titles like:
  [Programming GuideConfigIndex](config/index.md)
  [ExtensionsAuthkeyIndex](authkey/index.md)

These should be the actual titles from the RST source files:
  [Configuration](config/index.md)
  [Authkey](authkey/index.md)

This script:
1. Finds all index.md files with grid cards
2. For each malformed link, finds the corresponding RST file
3. Extracts the actual title from the RST file
4. Replaces malformed titles with correct ones from RST
"""

import re
from pathlib import Path
from typing import List, Tuple, Optional

def extract_title_from_rst(rst_path: Path) -> Optional[str]:
    """
    Extract the title from an RST file.
    
    RST titles are typically the first heading, marked with === or --- underlines.
    """
    try:
        content = rst_path.read_text(encoding='utf-8')
    except Exception as e:
        return None
    
    lines = content.splitlines()
    
    for i, line in enumerate(lines):
        # Check if next line is an underline (=== or ---)
        if i + 1 < len(lines):
            next_line = lines[i + 1].strip()
            # RST title underlines are === or --- (at least 3 chars)
            if (next_line and 
                (all(c == '=' for c in next_line) or all(c == '-' for c in next_line)) and
                len(next_line) >= 3):
                # This line is the title
                title = line.strip()
                if title:
                    return title
    
    return None

def find_rst_source(md_path: Path, link_target: str) -> Optional[Path]:
    """
    Find the corresponding RST source file for a markdown link target.
    
    Examples:
      MD: doc/en/developer/docs/programming-guide/index.md
      Link: config/index.md
      RST: doc/en/developer/source/programming-guide/config/index.rst
    """
    # Get the directory of the current MD file
    md_dir = md_path.parent
    
    # Resolve the target relative to the MD file
    target_md = (md_dir / link_target).resolve()
    
    # Convert MD path to RST path
    # Replace /docs/ with /source/ and .md with .rst
    md_str = str(target_md)
    rst_str = md_str.replace('/docs/', '/source/').replace('\\docs\\', '\\source\\').replace('.md', '.rst')
    rst_path = Path(rst_str)
    
    if rst_path.exists():
        return rst_path
    
    return None

def fix_grid_card_line(line: str, md_file_path: Path) -> Tuple[str, bool, Optional[str]]:
    """
    Fix a single grid card line if it has a malformed title.
    
    Returns: (fixed_line, was_changed, debug_message)
    """
    # Pattern: - [SomeMalformedTitle](path/to/file.md)
    pattern = r'^(\s*-\s+)\[([^\]]+)\]\(([^)]+)\)(.*)$'
    match = re.match(pattern, line)
    
    if not match:
        return line, False, None
    
    prefix, title, target, suffix = match.groups()
    
    # Check if title looks malformed
    # Pattern 1: CamelCase without spaces (e.g., "ExtensionsAuthkeyIndex")
    # Pattern 2: Contains known prefixes (e.g., "Programming GuideConfig Index")
    is_malformed = False
    
    if re.search(r'[a-z][A-Z]', title) and ' ' not in title.strip():
        is_malformed = True
    elif any(prefix_word in title for prefix_word in ['Programming Guide', 'Extensions', 'Community', 'Configuration', 'Policies', 'Quickstart']):
        is_malformed = True
    
    if is_malformed:
        # Find the RST source file
        rst_path = find_rst_source(md_file_path, target)
        
        if rst_path:
            # Extract title from RST
            correct_title = extract_title_from_rst(rst_path)
            
            if correct_title:
                fixed_line = f"{prefix}[{correct_title}]({target}){suffix}\n"
                debug_msg = f"RST: {rst_path.name} -> '{correct_title}'"
                return fixed_line, True, debug_msg
            else:
                debug_msg = f"Could not extract title from {rst_path}"
                return line, False, debug_msg
        else:
            debug_msg = f"Could not find RST source for {target}"
            return line, False, debug_msg
    
    return line, False, None

def fix_index_file(file_path: Path) -> Tuple[int, List[str]]:
    """
    Fix grid card titles in a single index.md file.
    
    Returns: (num_fixes, list_of_changes)
    """
    try:
        content = file_path.read_text(encoding='utf-8')
    except Exception as e:
        print(f"Error reading {file_path}: {e}")
        return 0, []
    
    lines = content.splitlines(keepends=True)
    fixed_lines = []
    changes = []
    num_fixes = 0
    in_grid_cards = False
    
    for i, line in enumerate(lines, 1):
        # Track if we're inside a grid cards block
        if '<div class="grid cards"' in line:
            in_grid_cards = True
            fixed_lines.append(line)
            continue
        
        if '</div>' in line and in_grid_cards:
            in_grid_cards = False
            fixed_lines.append(line)
            continue
        
        # Only fix lines inside grid cards blocks
        if in_grid_cards and line.strip().startswith('- ['):
            fixed_line, was_changed, debug_msg = fix_grid_card_line(line, file_path)
            fixed_lines.append(fixed_line)
            
            if was_changed:
                num_fixes += 1
                changes.append(f"  Line {i}: {line.strip()} -> {fixed_line.strip()}")
                if debug_msg:
                    changes.append(f"    ({debug_msg})")
            elif debug_msg:
                # Log issues even if not changed
                changes.append(f"  Line {i}: {line.strip()} - {debug_msg}")
        else:
            fixed_lines.append(line)
    
    if num_fixes > 0:
        # Write back the fixed content
        try:
            file_path.write_text(''.join(fixed_lines), encoding='utf-8')
        except Exception as e:
            print(f"Error writing {file_path}: {e}")
            return 0, []
    
    return num_fixes, changes

def main():
    """Main function to fix all index.md files."""
    doc_root = Path('doc/en')
    
    if not doc_root.exists():
        print(f"Error: {doc_root} does not exist")
        return
    
    # Find all index.md files
    index_files = list(doc_root.rglob('**/index.md'))
    
    print(f"Found {len(index_files)} index.md files")
    print("=" * 80)
    
    total_fixes = 0
    files_changed = 0
    
    for file_path in sorted(index_files):
        num_fixes, changes = fix_index_file(file_path)
        
        if num_fixes > 0:
            files_changed += 1
            total_fixes += num_fixes
            print(f"\n{file_path}: {num_fixes} fix(es)")
            for change in changes:
                print(change)
    
    print("\n" + "=" * 80)
    print(f"Summary: Fixed {total_fixes} grid card titles in {files_changed} files")

if __name__ == '__main__':
    main()
