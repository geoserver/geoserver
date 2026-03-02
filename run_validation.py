#!/usr/bin/env python3
"""
Run validation suite for RST to Markdown migration.

This script orchestrates all validation checks:
- Round-trip HTML comparison
- Link integrity validation
- Image reference validation
- Screenshot QA report generation
"""

import argparse
import sys
from pathlib import Path
from validators import (
    RoundTripValidator,
    LinkValidator,
    ImageValidator,
    generate_validation_report,
    ValidationStatus
)


def main():
    parser = argparse.ArgumentParser(
        description="Validate RST to Markdown conversion"
    )
    parser.add_argument(
        "--rst-dir",
        required=True,
        help="Directory containing RST source files (e.g., doc/en/user/source)"
    )
    parser.add_argument(
        "--md-dir",
        required=True,
        help="Directory containing converted Markdown files (e.g., doc/en/user)"
    )
    parser.add_argument(
        "--site-dir",
        help="Directory containing built MkDocs site (default: <md-dir>/site)"
    )
    parser.add_argument(
        "--skip-build",
        action="store_true",
        help="Skip building HTML (use existing builds)"
    )
    parser.add_argument(
        "--skip-roundtrip",
        action="store_true",
        help="Skip round-trip HTML comparison"
    )
    parser.add_argument(
        "--skip-links",
        action="store_true",
        help="Skip link validation"
    )
    parser.add_argument(
        "--skip-images",
        action="store_true",
        help="Skip image validation"
    )
    parser.add_argument(
        "--output",
        default="validation_report.md",
        help="Output file for validation report (default: validation_report.md)"
    )
    parser.add_argument(
        "--screenshot-report",
        default="screenshot_qa_report.md",
        help="Output file for screenshot QA report (default: screenshot_qa_report.md)"
    )

    args = parser.parse_args()

    # Validate directories exist
    rst_dir = Path(args.rst_dir)
    md_dir = Path(args.md_dir)
    
    if not rst_dir.exists():
        print(f"Error: RST directory not found: {rst_dir}")
        return 1
    
    if not md_dir.exists():
        print(f"Error: Markdown directory not found: {md_dir}")
        return 1

    # Determine site directory
    site_dir = Path(args.site_dir) if args.site_dir else md_dir / "site"

    print("=" * 70)
    print("RST to Markdown Migration - Validation Suite")
    print("=" * 70)
    print(f"\nRST Directory: {rst_dir}")
    print(f"Markdown Directory: {md_dir}")
    print(f"Site Directory: {site_dir}")
    print()

    # Initialize validators
    round_trip_validator = RoundTripValidator(str(rst_dir), str(md_dir))
    link_validator = LinkValidator(str(site_dir))
    image_validator = ImageValidator(str(md_dir))

    # Run validations
    round_trip_report = None
    broken_links = []
    broken_images = []

    # 1. Round-trip validation
    if not args.skip_roundtrip:
        print("\n" + "=" * 70)
        print("STEP 1: Round-Trip Validation")
        print("=" * 70)
        
        try:
            if not args.skip_build:
                print("\nBuilding Sphinx HTML...")
                round_trip_validator.build_sphinx_html()
                print("✓ Sphinx build complete")
                
                print("\nBuilding MkDocs HTML...")
                round_trip_validator.build_mkdocs_html()
                print("✓ MkDocs build complete")
            else:
                print("\nSkipping builds (using existing HTML)")
            
            print("\nComparing HTML outputs...")
            round_trip_report = round_trip_validator.compare_content()
            print(f"✓ Comparison complete: {len(round_trip_report.content_issues)} issues found")
            
            print("\nValidating images in HTML...")
            missing_images = round_trip_validator.validate_images()
            round_trip_report.missing_images = missing_images
            print(f"✓ Image validation complete: {len(missing_images)} missing images")
            
            print("\nValidating code blocks...")
            missing_blocks = round_trip_validator.validate_code_blocks()
            round_trip_report.missing_code_blocks = missing_blocks
            print(f"✓ Code block validation complete: {len(missing_blocks)} issues")
            
        except Exception as e:
            print(f"✗ Round-trip validation failed: {e}")
            if round_trip_report is None:
                # Create minimal report
                from validators import ValidationReport, ValidationStatus
                round_trip_report = ValidationReport(
                    total_files=0,
                    successful_conversions=0,
                    failed_conversions=0,
                    overall_status=ValidationStatus.FAILED
                )
    else:
        print("\nSkipping round-trip validation")
        from validators import ValidationReport, ValidationStatus
        round_trip_report = ValidationReport(
            total_files=0,
            successful_conversions=0,
            failed_conversions=0,
            overall_status=ValidationStatus.PASSED
        )

    # 2. Link validation
    if not args.skip_links:
        print("\n" + "=" * 70)
        print("STEP 2: Link Validation")
        print("=" * 70)
        
        try:
            if not site_dir.exists():
                print(f"✗ Site directory not found: {site_dir}")
                print("  Run without --skip-build to build the site first")
            else:
                print("\nValidating internal links...")
                internal_broken = link_validator.validate_internal_links()
                print(f"✓ Internal link validation complete: {len(internal_broken)} broken links")
                
                print("\nValidating anchors...")
                anchor_broken = link_validator.validate_anchors()
                print(f"✓ Anchor validation complete: {len(anchor_broken)} broken anchors")
                
                print("\nValidating external links...")
                external_broken = link_validator.validate_external_links()
                print(f"✓ External link validation complete: {len(external_broken)} issues")
                
                broken_links = internal_broken + anchor_broken + external_broken
                
        except Exception as e:
            print(f"✗ Link validation failed: {e}")
    else:
        print("\nSkipping link validation")

    # 3. Image validation
    if not args.skip_images:
        print("\n" + "=" * 70)
        print("STEP 3: Image Validation")
        print("=" * 70)
        
        try:
            print("\nScanning for image references...")
            images = image_validator.scan_images()
            print(f"✓ Found {len(images)} image references")
            
            print("\nValidating image references...")
            broken_image_refs = image_validator.validate_references(images)
            broken_images = broken_image_refs
            print(f"✓ Image reference validation complete: {len(broken_images)} broken references")
            
            print("\nGenerating screenshot QA report...")
            screenshot_report = image_validator.generate_screenshot_report(images)
            print(f"✓ Screenshot QA report generated:")
            print(f"  - Total images: {screenshot_report.total_images}")
            print(f"  - Screenshots: {screenshot_report.screenshot_count}")
            print(f"  - Diagrams: {screenshot_report.diagram_count}")
            print(f"  - Flagged for update: {len(screenshot_report.flagged_for_update)}")
            
            # Write screenshot report
            with open(args.screenshot_report, 'w', encoding='utf-8') as f:
                f.write(screenshot_report.to_markdown())
            print(f"\n✓ Screenshot QA report written to: {args.screenshot_report}")
            
        except Exception as e:
            print(f"✗ Image validation failed: {e}")
    else:
        print("\nSkipping image validation")

    # Generate comprehensive validation report
    print("\n" + "=" * 70)
    print("STEP 4: Generate Validation Report")
    print("=" * 70)
    
    try:
        generate_validation_report(
            round_trip_report,
            broken_links,
            broken_images,
            args.output
        )
        print(f"\n✓ Validation report written to: {args.output}")
    except Exception as e:
        print(f"✗ Failed to generate validation report: {e}")
        return 1

    # Summary
    print("\n" + "=" * 70)
    print("VALIDATION SUMMARY")
    print("=" * 70)
    print(f"\nOverall Status: {round_trip_report.overall_status.value.upper()}")
    print(f"\nIssues Found:")
    print(f"  - Content issues: {len(round_trip_report.content_issues)}")
    print(f"  - Broken links: {len(broken_links)}")
    print(f"  - Broken images: {len(broken_images)}")
    print(f"  - Missing images: {len(round_trip_report.missing_images)}")
    print(f"  - Code block issues: {len(round_trip_report.missing_code_blocks)}")
    
    total_issues = (
        len(round_trip_report.content_issues) +
        len(broken_links) +
        len(broken_images) +
        len(round_trip_report.missing_images) +
        len(round_trip_report.missing_code_blocks)
    )
    
    print(f"\nTotal Issues: {total_issues}")
    print(f"\nReports Generated:")
    print(f"  - Validation Report: {args.output}")
    print(f"  - Screenshot QA Report: {args.screenshot_report}")
    print()

    # Return exit code based on validation status
    if round_trip_report.overall_status == ValidationStatus.FAILED:
        print("❌ Validation FAILED - critical issues found")
        return 1
    elif round_trip_report.overall_status == ValidationStatus.WARNING:
        print("⚠️  Validation PASSED with warnings")
        return 0
    else:
        print("✅ Validation PASSED - no issues found")
        return 0


if __name__ == "__main__":
    sys.exit(main())
