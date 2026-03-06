#!/usr/bin/env python3
"""
Run comprehensive link validation for the RST to Markdown migration.
This script validates:
- Internal links
- Anchor links
- Image references
- External links (basic validation)
"""

import sys
from pathlib import Path
from validators import LinkValidator, ImageValidator, ValidationReport, generate_validation_report

def main():
    print("=" * 70)
    print("Link Integrity Validation for RST to Markdown Migration")
    print("=" * 70)
    
    # Define paths - check both user and developer docs
    docs_paths = [
        "doc/en/user",
        "doc/en/developer", 
        "doc/en/docguide"
    ]
    
    all_broken_links = []
    all_broken_images = []
    total_files = 0
    
    for docs_path in docs_paths:
        docs_dir = Path(docs_path)
        
        # Check if docs directory exists
        if not docs_dir.exists():
            print(f"\n⚠️  Skipping {docs_path} - directory not found")
            continue
            
        # Check for built site (target/html is the configured site_dir)
        site_dir = docs_dir / "target" / "html"
        if not site_dir.exists():
            print(f"\n⚠️  Skipping {docs_path} - no built site found at {site_dir}")
            print(f"   Run 'mkdocs build' in {docs_path} first")
            continue
        
        print(f"\n{'=' * 70}")
        print(f"Validating: {docs_path}")
        print(f"{'=' * 70}")
        
        # Count HTML files
        html_files = list(site_dir.rglob("*.html"))
        total_files += len(html_files)
        print(f"Found {len(html_files)} HTML files")
        
        # Validate links
        print(f"\n--- Link Validation ---")
        link_validator = LinkValidator(str(site_dir))
        
        internal_broken = link_validator.validate_internal_links()
        anchor_broken = link_validator.validate_anchors()
        external_broken = link_validator.validate_external_links()
        
        broken_links = internal_broken + anchor_broken + external_broken
        all_broken_links.extend(broken_links)
        
        print(f"✓ Internal links checked")
        print(f"  - Broken internal links: {len(internal_broken)}")
        print(f"✓ Anchor links checked")
        print(f"  - Broken anchors: {len(anchor_broken)}")
        print(f"✓ External links checked")
        print(f"  - Malformed external URLs: {len(external_broken)}")
        
        # Validate images
        print(f"\n--- Image Validation ---")
        
        # Check for docs directory (Markdown source)
        md_docs_dir = docs_dir / "docs"
        if md_docs_dir.exists():
            img_validator = ImageValidator(str(md_docs_dir))
            images = img_validator.scan_images()
            broken_images = img_validator.validate_references(images)
            all_broken_images.extend(broken_images)
            
            print(f"✓ Image references scanned")
            print(f"  - Total images: {len(images)}")
            print(f"  - Broken image references: {len(broken_images)}")
        else:
            print(f"⚠️  No Markdown docs directory found at {md_docs_dir}")
    
    # Generate summary report
    print(f"\n{'=' * 70}")
    print("VALIDATION SUMMARY")
    print(f"{'=' * 70}")
    print(f"Total HTML files validated: {total_files}")
    print(f"Total broken links: {len(all_broken_links)}")
    print(f"Total broken images: {len(all_broken_images)}")
    
    # Create detailed report
    report = ValidationReport(
        total_files=total_files,
        successful_conversions=total_files,
        failed_conversions=0,
        broken_links=all_broken_links,
        missing_images=[img.image_path for img in all_broken_images]
    )
    
    # Write detailed reports
    output_file = "link_validation_report.md"
    generate_validation_report(report, all_broken_links, all_broken_images, output_file)
    
    # Write broken links by type
    if all_broken_links:
        print(f"\n--- Broken Links by Type ---")
        from collections import defaultdict
        by_type = defaultdict(list)
        for link in all_broken_links:
            by_type[link.error_type.value].append(link)
        
        for error_type, links in by_type.items():
            print(f"{error_type}: {len(links)}")
            
        # Write detailed broken links report
        with open("broken_links_detailed.txt", "w", encoding="utf-8") as f:
            f.write("Broken Links - Detailed Report\n")
            f.write("=" * 70 + "\n\n")
            
            for error_type, links in sorted(by_type.items()):
                f.write(f"\n{error_type.upper()} ({len(links)} issues)\n")
                f.write("-" * 70 + "\n")
                for link in links[:50]:  # Limit to first 50 per type
                    f.write(f"\nFile: {link.source_file}:{link.line_number}\n")
                    f.write(f"Link: {link.target_url}\n")
                    f.write(f"Text: {link.link_text}\n")
                    if link.suggestion:
                        f.write(f"Suggestion: {link.suggestion}\n")
                if len(links) > 50:
                    f.write(f"\n... and {len(links) - 50} more\n")
        
        print(f"\n✓ Detailed broken links report: broken_links_detailed.txt")
    
    # Write broken images report
    if all_broken_images:
        with open("broken_images_detailed.txt", "w", encoding="utf-8") as f:
            f.write("Broken Image References - Detailed Report\n")
            f.write("=" * 70 + "\n\n")
            
            for img in all_broken_images:
                f.write(f"\nFile: {img.markdown_file}:{img.line_number}\n")
                f.write(f"Image: {img.image_path}\n")
                f.write(f"Alt text: {img.alt_text}\n")
        
        print(f"✓ Detailed broken images report: broken_images_detailed.txt")
    
    print(f"\n{'=' * 70}")
    if len(all_broken_links) == 0 and len(all_broken_images) == 0:
        print("✅ VALIDATION PASSED - No broken links or images found!")
        return 0
    else:
        print("❌ VALIDATION FAILED - Issues found that need fixing")
        print(f"\nNext steps:")
        print(f"1. Review {output_file} for summary")
        print(f"2. Review broken_links_detailed.txt for link issues")
        if all_broken_images:
            print(f"3. Review broken_images_detailed.txt for image issues")
        return 1

if __name__ == "__main__":
    sys.exit(main())
