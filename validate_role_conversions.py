#!/usr/bin/env python3
"""
Validate that all interpreted text roles have been converted.
"""

import re
from pathlib import Path


def count_remaining_roles(docs_dir: Path) -> dict:
    """Count remaining interpreted text roles in a directory."""
    pattern = re.compile(r'`([^`]+?)`\{\.interpreted-text role="([^"]+?)"\}')
    
    roles = {}
    total = 0
    
    for md_file in docs_dir.rglob('*.md'):
        content = md_file.read_text(encoding='utf-8')
        matches = pattern.findall(content)
        
        for text, role in matches:
            if role not in roles:
                roles[role] = []
            roles[role].append(str(md_file))
            total += 1
    
    return roles, total


def main():
    """Main entry point."""
    manuals = {
        'User Manual': Path('doc/en/user/docs'),
        'Developer Manual': Path('doc/en/developer/docs'),
        'Documentation Guide': Path('doc/en/docguide/docs'),
    }
    
    print("=" * 70)
    print("ROLE CONVERSION VALIDATION")
    print("=" * 70)
    
    grand_total = 0
    all_roles = {}
    
    for name, path in manuals.items():
        if not path.exists():
            print(f"\n{name}: Directory not found")
            continue
        
        roles, total = count_remaining_roles(path)
        grand_total += total
        
        print(f"\n{name}:")
        if total == 0:
            print("  ✓ No remaining interpreted text roles")
        else:
            print(f"  ⚠️  {total} remaining interpreted text roles:")
            for role, files in sorted(roles.items()):
                print(f"    - {role}: {len(files)} occurrences")
                all_roles[role] = all_roles.get(role, 0) + len(files)
    
    print("\n" + "=" * 70)
    print(f"TOTAL REMAINING: {grand_total} occurrences")
    
    if grand_total == 0:
        print("✓ SUCCESS: All interpreted text roles have been converted!")
        return 0
    else:
        print("\n⚠️  The following roles still need to be fixed:")
        for role, count in sorted(all_roles.items()):
            print(f"  - {role}: {count} occurrences")
        return 1


if __name__ == '__main__':
    exit(main())
