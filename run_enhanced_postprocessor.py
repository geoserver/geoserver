#!/usr/bin/env python3
"""
Run enhanced interpreted text role postprocessor on already-converted Markdown files
"""

import re
import glob
from pathlib import Path

def run_enhanced_postprocessor(docs_dir):
    """Run enhanced interpreted text role conversion"""
    print(f"Running enhanced postprocessor on: {docs_dir}")
    
    # Role mappings for GeoServer documentation
    role_mappings = {
        'website': 'https://geoserver.org/',
        'developer': 'https://docs.geoserver.org/latest/en/developer/',
        'user': '../user/',
        'api': 'api/',
        'geotools': 'https://docs.geotools.org/latest/userguide/',
        'geot': 'https://osgeo-org.atlassian.net/browse/GEOT-',  # GeoTools JIRA issues
        'wiki': 'https://github.com/geoserver/geoserver/wiki/',
        'geos': 'https://osgeo-org.atlassian.net/browse/GEOS-',
        'docguide': '../docguide/',
        'download_community': 'https://build.geoserver.org/geoserver/main/community-latest/',
        'download_extension': 'https://build.geoserver.org/geoserver/main/ext-latest/',
        ':download_community': 'https://build.geoserver.org/geoserver/main/community-latest/',
        'download_release': 'https://geoserver.org/release/stable/',
    }
    
    # Pattern 1: `text <url>`{.interpreted-text role="rolename"} (with space)
    pattern1 = r'`([^`]+) <([^>]+)>`\{\.interpreted-text role="([^"]+)"\}'
    
    # Pattern 2: `text<url>`{.interpreted-text role="rolename"} (no space - for ref/doc roles)
    pattern2 = r'`([^`<]+)<([^>]+)>`\{\.interpreted-text role="([^"]+)"\}'
    
    # Pattern 3: `text`{.interpreted-text role="rolename"} (simple, no URL)
    pattern3 = r'`([^`]+)`\{\.interpreted-text role="([^"]+)"\}'
    
    converted_count = 0
    file_count = 0
    unknown_roles = {}
    
    for md_file in glob.glob(str(Path(docs_dir) / '**/*.md'), recursive=True):
        try:
            with open(md_file, 'r', encoding='utf-8') as f:
                content = f.read()
            
            original_content = content
            file_converted = [0]
            
            def replace_role_with_space(match):
                text = match.group(1)
                url = match.group(2)
                role = match.group(3)
                
                if role in role_mappings:
                    full_url = role_mappings[role] + url
                    file_converted[0] += 1
                    return f'[{text}]({full_url})'
                else:
                    if role == 'ref':
                        if '#' in url:
                            file_converted[0] += 1
                            return f'[{text}]({url})'
                        else:
                            file_converted[0] += 1
                            return f'[{text}](#{url})'
                    unknown_roles[role] = unknown_roles.get(role, 0) + 1
                    return match.group(0)
            
            def replace_role_no_space(match):
                text = match.group(1)
                url = match.group(2)
                role = match.group(3)
                
                # ref and doc roles are internal references
                if role in ['ref', 'doc']:
                    # doc role points to other documentation pages
                    if role == 'doc':
                        file_converted[0] += 1
                        return f'[{text}]({url})'
                    # ref role is for anchors/sections
                    if '#' in url:
                        file_converted[0] += 1
                        return f'[{text}]({url})'
                    else:
                        file_converted[0] += 1
                        return f'[{text}](#{url})'
                
                if role in role_mappings:
                    full_url = role_mappings[role] + url
                    file_converted[0] += 1
                    return f'[{text}]({full_url})'
                
                unknown_roles[role] = unknown_roles.get(role, 0) + 1
                return match.group(0)
            
            def replace_simple_role(match):
                text = match.group(1)
                role = match.group(2)
                
                # For download roles, just use the text as filename
                if role in ['download_community', 'download_extension', ':download_community', 'download_release']:
                    full_url = role_mappings.get(role, role_mappings.get(':' + role, '')) + text
                    file_converted[0] += 1
                    return f'[{text}]({full_url})'
                
                # For geos role (GitHub issues), use text as issue number
                if role == 'geos':
                    full_url = role_mappings['geos'] + text
                    file_converted[0] += 1
                    return f'[{text}]({full_url})'
                
                # For geot role (GeoTools JIRA), use text as issue number
                if role == 'geot':
                    full_url = role_mappings['geot'] + text
                    file_converted[0] += 1
                    return f'[{text}]({full_url})'
                
                # doc role with just path
                if role == 'doc':
                    file_converted[0] += 1
                    return f'[{text}]({text})'
                
                # abbr role - just keep as plain text
                if role == 'abbr':
                    file_converted[0] += 1
                    return text
                
                unknown_roles[role] = unknown_roles.get(role, 0) + 1
                return match.group(0)
            
            # Apply patterns in order (most specific first)
            content = re.sub(pattern1, replace_role_with_space, content)
            content = re.sub(pattern2, replace_role_no_space, content)
            content = re.sub(pattern3, replace_simple_role, content)
            
            if content != original_content:
                with open(md_file, 'w', encoding='utf-8') as f:
                    f.write(content)
                file_count += 1
                converted_count += file_converted[0]
        
        except Exception as e:
            print(f"WARNING: Failed to process {md_file}: {e}")
    
    # Report results
    print(f"✓ Converted {converted_count} interpreted text roles in {file_count} files")
    
    if unknown_roles:
        print(f"\nRemaining unknown roles:")
        for role, count in sorted(unknown_roles.items(), key=lambda x: x[1], reverse=True):
            print(f"  - {role}: {count} occurrences")
    
    return converted_count, file_count, unknown_roles

if __name__ == "__main__":
    import sys
    
    # Process all three documentation directories
    doc_dirs = [
        "doc/en/docguide/docs",
        "doc/en/developer/docs",
        "doc/en/user/docs",
    ]
    
    total_converted = 0
    total_files = 0
    all_unknown = {}
    
    print("=" * 60)
    print("Enhanced Interpreted Text Role Postprocessor")
    print("=" * 60)
    print()
    
    for doc_dir in doc_dirs:
        if Path(doc_dir).exists():
            print(f"\nProcessing: {doc_dir}")
            print("-" * 60)
            converted, files, unknown = run_enhanced_postprocessor(doc_dir)
            total_converted += converted
            total_files += files
            
            # Merge unknown roles
            for role, count in unknown.items():
                all_unknown[role] = all_unknown.get(role, 0) + count
        else:
            print(f"\nSkipping: {doc_dir} (not found)")
    
    print()
    print("=" * 60)
    print(f"Total: Converted {total_converted} roles in {total_files} files")
    
    if all_unknown:
        print(f"\nAll remaining unknown roles:")
        for role, count in sorted(all_unknown.items(), key=lambda x: x[1], reverse=True):
            print(f"  - {role}: {count} occurrences")
    
    print("=" * 60)
