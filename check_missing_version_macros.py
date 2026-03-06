#!/usr/bin/env python3
"""
Check for missing version/release macros in converted Markdown files.

This script compares RST files (from a previous commit) with their converted
Markdown equivalents to find places where |version| or |release| variables
were dropped during conversion.
"""

import subprocess
import re
import sys
from pathlib import Path

def get_rst_files_with_version_macros(commit="07fe3b2c7d"):
    """Get all RST files that contain |version| or |release| from a specific commit."""
    result = subprocess.run(
        ["git", "grep", "-l", "-E", r"\|version\||\|release\|", commit, "--", "doc/"],
        capture_output=True,
        text=True
    )
    
    if result.returncode != 0:
        return []
    
    files = result.stdout.strip().split('\n')
    # Remove commit prefix
    files = [f.split(':', 1)[1] for f in files if f]
    return files

def get_file_content_from_commit(filepath, commit="07fe3b2c7d"):
    """Get file content from a specific commit."""
    result = subprocess.run(
        ["git", "show", f"{commit}:{filepath}"],
        capture_output=True,
        text=True
    )
    
    if result.returncode != 0:
        return None
    
    return result.stdout

def convert_rst_path_to_md(rst_path):
    """Convert RST file path to expected Markdown path."""
    # doc/en/user/source/index.rst -> doc/en/user/docs/index.md
    md_path = rst_path.replace('/source/', '/docs/').replace('.rst', '.md')
    return md_path

def find_version_macro_contexts(content):
    """Find all occurrences of |version| or |release| with surrounding context."""
    lines = content.split('\n')
    contexts = []
    
    for i, line in enumerate(lines):
        if '|version|' in line or '|release|' in line:
            # Get context: 2 lines before and after
            start = max(0, i - 2)
            end = min(len(lines), i + 3)
            context_lines = lines[start:end]
            
            contexts.append({
                'line_num': i + 1,
                'line': line,
                'context': '\n'.join(context_lines),
                'has_version': '|version|' in line,
                'has_release': '|release|' in line
            })
    
    return contexts

def check_md_file_has_macro(md_path, rst_context):
    """Check if the Markdown file has the equivalent macro."""
    if not Path(md_path).exists():
        return False, "MD file doesn't exist"
    
    with open(md_path, 'r', encoding='utf-8') as f:
        md_content = f.read()
    
    # Check for {{ version }} or {{ release }}
    if rst_context['has_version'] and '{{ version }}' not in md_content:
        return False, "Missing {{ version }}"
    
    if rst_context['has_release'] and '{{ release }}' not in md_content:
        return False, "Missing {{ release }}"
    
    return True, "OK"

def main():
    print("Checking for missing version/release macros in converted Markdown files...")
    print("=" * 80)
    
    rst_files = get_rst_files_with_version_macros()
    print(f"\nFound {len(rst_files)} RST files with |version| or |release| macros\n")
    
    issues = []
    
    for rst_file in rst_files:
        rst_content = get_file_content_from_commit(rst_file)
        if not rst_content:
            continue
        
        contexts = find_version_macro_contexts(rst_content)
        md_file = convert_rst_path_to_md(rst_file)
        
        for context in contexts:
            has_macro, reason = check_md_file_has_macro(md_file, context)
            
            if not has_macro:
                issues.append({
                    'rst_file': rst_file,
                    'md_file': md_file,
                    'line_num': context['line_num'],
                    'rst_line': context['line'],
                    'reason': reason,
                    'context': context['context']
                })
    
    if issues:
        print(f"❌ Found {len(issues)} missing version/release macros:\n")
        
        for issue in issues:
            print(f"File: {issue['md_file']}")
            print(f"  Original RST: {issue['rst_file']}:{issue['line_num']}")
            print(f"  Issue: {issue['reason']}")
            print(f"  RST line: {issue['rst_line']}")
            print(f"  Context:")
            for line in issue['context'].split('\n'):
                print(f"    {line}")
            print()
        
        return 1
    else:
        print("✅ All version/release macros have been converted correctly!")
        return 0

if __name__ == "__main__":
    sys.exit(main())
