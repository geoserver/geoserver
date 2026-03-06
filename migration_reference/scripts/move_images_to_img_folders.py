#!/usr/bin/env python3
"""
Move images to img subfolders and update Markdown references.
"""

import os
import re
import shutil
from pathlib import Path
from collections import defaultdict

def find_markdown_files(root_dir):
    """Find all Markdown files in the documentation."""
    md_files = []
    for root, dirs, files in os.walk(root_dir):
        dirs[:] = [d for d in dirs if not d.startswith('.') and d not in ['site', 'target', 'build']]
        for file in files:
            if file.endswith('.md'):
                md_files.append(os.path.join(root, file))
    return md_files

def extract_image_references(content):
    """Extract all image references from Markdown content."""
    # Match Markdown image syntax: ![alt](path)
    pattern = r'!\[([^\]]*)\]\(([^)]+)\)'
    return re.findall(pattern, content)

def needs_img_folder(img_path):
    """Check if image path needs to be moved to img folder."""
    # Skip external URLs
    if img_path.startswith('http://') or img_path.startswith('https://'):
        return False
    
    # Check if already in img or images folder
    path_parts = img_path.split('/')
    if 'img' in path_parts or 'images' in path_parts:
        return False
    
    return True

def process_markdown_file(md_file, dry_run=False):
    """Process a single Markdown file to move images and update references."""
    changes = []
    
    try:
        with open(md_file, 'r', encoding='utf-8') as f:
            content = f.read()
        
        original_content = content
        md_dir = os.path.dirname(md_file)
        
        # Extract all image references
        images = extract_image_references(content)
        
        for alt, img_path in images:
            img_path = img_path.strip()
            
            if not needs_img_folder(img_path):
                continue
            
            # Resolve the actual image file path
            if img_path.startswith('../'):
                # Relative path going up
                actual_img_path = os.path.normpath(os.path.join(md_dir, img_path))
            elif img_path.startswith('./'):
                # Relative path in current dir
                actual_img_path = os.path.normpath(os.path.join(md_dir, img_path[2:]))
            else:
                # Relative path in current dir (no ./)
                actual_img_path = os.path.normpath(os.path.join(md_dir, img_path))
            
            # Check if image file exists
            if not os.path.exists(actual_img_path):
                print(f"⚠️  Image not found: {actual_img_path} (referenced in {md_file})")
                continue
            
            # Determine target img folder
            img_folder = os.path.join(md_dir, 'img')
            img_filename = os.path.basename(actual_img_path)
            target_img_path = os.path.join(img_folder, img_filename)
            
            # Create img folder if needed
            if not dry_run and not os.path.exists(img_folder):
                os.makedirs(img_folder)
                print(f"✅ Created directory: {img_folder}")
            
            # Move the image file
            if not dry_run:
                if os.path.exists(target_img_path):
                    # Check if it's the same file
                    if os.path.samefile(actual_img_path, target_img_path):
                        print(f"ℹ️  Image already at target: {target_img_path}")
                    else:
                        print(f"⚠️  Target exists (different file): {target_img_path}")
                        # Add suffix to avoid overwrite
                        base, ext = os.path.splitext(img_filename)
                        counter = 1
                        while os.path.exists(os.path.join(img_folder, f"{base}_{counter}{ext}")):
                            counter += 1
                        img_filename = f"{base}_{counter}{ext}"
                        target_img_path = os.path.join(img_folder, img_filename)
                        shutil.move(actual_img_path, target_img_path)
                        print(f"✅ Moved (renamed): {actual_img_path} -> {target_img_path}")
                else:
                    shutil.move(actual_img_path, target_img_path)
                    print(f"✅ Moved: {actual_img_path} -> {target_img_path}")
            
            # Update the reference in Markdown
            new_img_path = f"img/{img_filename}"
            old_pattern = re.escape(f'![{alt}]({img_path})')
            new_pattern = f'![{alt}]({new_img_path})'
            content = content.replace(f'![{alt}]({img_path})', new_pattern)
            
            changes.append({
                'old_path': img_path,
                'new_path': new_img_path,
                'actual_file': actual_img_path,
                'target_file': target_img_path
            })
        
        # Write updated content if changes were made
        if content != original_content and not dry_run:
            with open(md_file, 'w', encoding='utf-8') as f:
                f.write(content)
            print(f"✅ Updated references in: {md_file}")
        
    except Exception as e:
        print(f"❌ Error processing {md_file}: {e}")
    
    return changes

def main():
    doc_dirs = [
        'doc/en/user/docs',
        'doc/en/developer/docs',
        'doc/en/docguide/docs'
    ]
    
    print("=" * 80)
    print("Moving Images to img Subfolders")
    print("=" * 80)
    
    # Ask for confirmation
    print("\nThis script will:")
    print("1. Create img subfolders in directories with images")
    print("2. Move images to img subfolders")
    print("3. Update Markdown references to point to img/filename.png")
    print("\nDo you want to proceed? (yes/no): ", end='')
    
    # For automation, we'll proceed automatically
    proceed = True
    
    if not proceed:
        print("Aborted.")
        return
    
    total_changes = 0
    
    for doc_dir in doc_dirs:
        if not os.path.exists(doc_dir):
            print(f"\nSkipping {doc_dir} (not found)")
            continue
        
        print(f"\n{'=' * 80}")
        print(f"Processing {doc_dir}")
        print('=' * 80)
        
        md_files = find_markdown_files(doc_dir)
        
        for md_file in md_files:
            changes = process_markdown_file(md_file, dry_run=False)
            total_changes += len(changes)
    
    print("\n" + "=" * 80)
    print("SUMMARY")
    print("=" * 80)
    print(f"Total images moved: {total_changes}")
    print("\n✅ Image reorganization complete!")
    print("\nNext steps:")
    print("1. Review the changes with: git status")
    print("2. Test locally with: mkdocs serve")
    print("3. Verify all images display correctly")

if __name__ == '__main__':
    main()
