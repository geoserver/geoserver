#!/usr/bin/env python3
"""
Scan Markdown files for image references and identify images not in img subfolders.
"""

import os
import re
from pathlib import Path
from collections import defaultdict

def find_markdown_files(root_dir):
    """Find all Markdown files in the documentation."""
    md_files = []
    for root, dirs, files in os.walk(root_dir):
        # Skip hidden directories and build outputs
        dirs[:] = [d for d in dirs if not d.startswith('.') and d not in ['site', 'target', 'build']]
        for file in files:
            if file.endswith('.md'):
                md_files.append(os.path.join(root, file))
    return md_files

def extract_image_references(md_file):
    """Extract all image references from a Markdown file."""
    images = []
    try:
        with open(md_file, 'r', encoding='utf-8') as f:
            content = f.read()
            
        # Match Markdown image syntax: ![alt](path)
        pattern = r'!\[([^\]]*)\]\(([^)]+)\)'
        matches = re.findall(pattern, content)
        
        for alt, path in matches:
            # Skip external URLs
            if path.startswith('http://') or path.startswith('https://'):
                continue
            images.append(path.strip())
            
    except Exception as e:
        print(f"Error reading {md_file}: {e}")
    
    return images

def analyze_image_structure(doc_dirs):
    """Analyze image references and identify those not in img subfolders."""
    results = {
        'total_md_files': 0,
        'total_images': 0,
        'images_in_img_folder': 0,
        'images_not_in_img_folder': 0,
        'issues': []
    }
    
    for doc_dir in doc_dirs:
        if not os.path.exists(doc_dir):
            print(f"Skipping {doc_dir} (not found)")
            continue
            
        print(f"\nScanning {doc_dir}...")
        md_files = find_markdown_files(doc_dir)
        results['total_md_files'] += len(md_files)
        
        for md_file in md_files:
            images = extract_image_references(md_file)
            results['total_images'] += len(images)
            
            for img_path in images:
                # Check if image is in an img subfolder
                path_parts = img_path.split('/')
                
                # Check if 'img' appears in the path
                if 'img' in path_parts or 'images' in path_parts:
                    results['images_in_img_folder'] += 1
                else:
                    results['images_not_in_img_folder'] += 1
                    results['issues'].append({
                        'md_file': md_file,
                        'image_path': img_path
                    })
    
    return results

def main():
    # Documentation directories to scan
    doc_dirs = [
        'doc/en/user/docs',
        'doc/en/developer/docs',
        'doc/en/docguide/docs',
        'doc/zhCN/docs'
    ]
    
    print("=" * 80)
    print("Image Structure Analysis")
    print("=" * 80)
    
    results = analyze_image_structure(doc_dirs)
    
    print("\n" + "=" * 80)
    print("SUMMARY")
    print("=" * 80)
    print(f"Total Markdown files: {results['total_md_files']}")
    print(f"Total image references: {results['total_images']}")
    print(f"Images in img/images folders: {results['images_in_img_folder']}")
    print(f"Images NOT in img/images folders: {results['images_not_in_img_folder']}")
    
    if results['images_not_in_img_folder'] > 0:
        print(f"\n⚠️  Found {results['images_not_in_img_folder']} images that need to be moved!")
        
        # Group by directory
        by_dir = defaultdict(list)
        for issue in results['issues']:
            md_dir = os.path.dirname(issue['md_file'])
            by_dir[md_dir].append(issue)
        
        print(f"\nAffected directories: {len(by_dir)}")
        
        # Show sample issues
        print("\nSample issues (first 20):")
        for i, issue in enumerate(results['issues'][:20]):
            print(f"  {i+1}. {issue['md_file']}")
            print(f"     Image: {issue['image_path']}")
    else:
        print("\n✅ All images are already in img/images subfolders!")
    
    # Save detailed report
    report_file = 'image_structure_report.txt'
    with open(report_file, 'w', encoding='utf-8') as f:
        f.write("Image Structure Analysis Report\n")
        f.write("=" * 80 + "\n\n")
        f.write(f"Total Markdown files: {results['total_md_files']}\n")
        f.write(f"Total image references: {results['total_images']}\n")
        f.write(f"Images in img/images folders: {results['images_in_img_folder']}\n")
        f.write(f"Images NOT in img/images folders: {results['images_not_in_img_folder']}\n\n")
        
        if results['images_not_in_img_folder'] > 0:
            f.write("Issues Found:\n")
            f.write("-" * 80 + "\n")
            for issue in results['issues']:
                f.write(f"\nFile: {issue['md_file']}\n")
                f.write(f"Image: {issue['image_path']}\n")
        
    print(f"\nDetailed report saved to: {report_file}")

if __name__ == '__main__':
    main()
