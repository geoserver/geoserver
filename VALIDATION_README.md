# Validation Scripts for RST to Markdown Migration

This directory contains validation scripts to ensure the quality and accuracy of the RST to Markdown conversion for GeoServer documentation.

## Overview

The validation suite consists of three main validators:

1. **RoundTripValidator**: Compares Sphinx HTML output vs MkDocs HTML output to ensure content preservation
2. **LinkValidator**: Checks internal links, external links, and anchor references
3. **ImageValidator**: Verifies image references and identifies screenshots for QA

## Installation

Install required dependencies:

```bash
pip install -r doc/requirements.txt
```

Required packages:
- mkdocs
- mkdocs-material
- mkdocs-macros-plugin
- mkdocs-with-pdf
- pymdown-extensions
- sphinx (for round-trip comparison)

## Usage

### Quick Start

Run the complete validation suite:

```bash
python run_validation.py \
  --rst-dir doc/en/user/source \
  --md-dir doc/en/user
```

This will:
1. Build Sphinx HTML from RST sources
2. Build MkDocs HTML from Markdown files
3. Compare the two outputs
4. Validate all links
5. Validate all image references
6. Generate validation and screenshot QA reports

### Command-Line Options

```
--rst-dir PATH          Directory containing RST source files (required)
--md-dir PATH           Directory containing Markdown files (required)
--site-dir PATH         Directory containing built MkDocs site (default: <md-dir>/site)
--skip-build            Skip building HTML (use existing builds)
--skip-roundtrip        Skip round-trip HTML comparison
--skip-links            Skip link validation
--skip-images           Skip image validation
--output FILE           Output file for validation report (default: validation_report.md)
--screenshot-report FILE Output file for screenshot QA report (default: screenshot_qa_report.md)
```

### Examples

#### Validate User Manual

```bash
python run_validation.py \
  --rst-dir doc/en/user/source \
  --md-dir doc/en/user \
  --output user_validation.md \
  --screenshot-report user_screenshots.md
```

#### Validate Developer Manual

```bash
python run_validation.py \
  --rst-dir doc/en/developer/source \
  --md-dir doc/en/developer \
  --output developer_validation.md \
  --screenshot-report developer_screenshots.md
```

#### Validate Documentation Guide

```bash
python run_validation.py \
  --rst-dir doc/en/docguide/source \
  --md-dir doc/en/docguide \
  --output docguide_validation.md \
  --screenshot-report docguide_screenshots.md
```

#### Skip Builds (Use Existing HTML)

If you've already built the HTML and just want to re-run validation:

```bash
python run_validation.py \
  --rst-dir doc/en/user/source \
  --md-dir doc/en/user \
  --skip-build
```

#### Run Only Link Validation

```bash
python run_validation.py \
  --rst-dir doc/en/user/source \
  --md-dir doc/en/user \
  --skip-roundtrip \
  --skip-images
```

#### Run Only Image Validation

```bash
python run_validation.py \
  --rst-dir doc/en/user/source \
  --md-dir doc/en/user \
  --skip-roundtrip \
  --skip-links
```

## Using Validators Programmatically

You can also use the validators directly in Python scripts:

### RoundTripValidator

```python
from validators import RoundTripValidator

# Initialize validator
validator = RoundTripValidator('doc/en/user/source', 'doc/en/user')

# Build HTML from both sources
validator.build_sphinx_html()
validator.build_mkdocs_html()

# Compare content
report = validator.compare_content()

# Validate specific aspects
missing_images = validator.validate_images()
missing_blocks = validator.validate_code_blocks()
broken_links = validator.validate_links()

# Print report
print(report.to_markdown())
```

### LinkValidator

```python
from validators import LinkValidator

# Initialize validator with built site directory
validator = LinkValidator('doc/en/user/site')

# Validate different types of links
internal_broken = validator.validate_internal_links()
external_broken = validator.validate_external_links()
anchor_broken = validator.validate_anchors()

# Process results
for link in internal_broken:
    print(f"Broken link in {link.source_file}:{link.line_number}")
    print(f"  Target: {link.target_url}")
    print(f"  Error: {link.error_type.value}")
    if link.suggestion:
        print(f"  Suggestion: {link.suggestion}")
```

### ImageValidator

```python
from validators import ImageValidator

# Initialize validator
validator = ImageValidator('doc/en/user')

# Scan for all images
images = validator.scan_images()

# Validate references
broken_images = validator.validate_references(images)

# Identify screenshots
screenshots = validator.identify_screenshots(images)

# Generate QA report
screenshot_report = validator.generate_screenshot_report(images)

# Write report
with open('screenshot_qa.md', 'w') as f:
    f.write(screenshot_report.to_markdown())
```

## Output Reports

### Validation Report

The validation report (`validation_report.md`) includes:

- Summary of files processed
- Overall validation status (PASSED/FAILED/WARNING)
- Content issues found during round-trip comparison
- Broken internal links
- Broken external links
- Broken anchor references
- Missing images
- Code block issues

### Screenshot QA Report

The screenshot QA report (`screenshot_qa_report.md`) includes:

- Total image count
- Screenshot vs diagram classification
- Screenshots grouped by documentation page
- List of screenshots flagged for update
- Broken image references

This report is used to coordinate screenshot updates with the QA team.

## Validation Workflow

The recommended validation workflow for migration:

1. **Execute conversion** using `migration.py`
2. **Run validation suite** using `run_validation.py`
3. **Review validation report** and fix critical issues
4. **Review screenshot QA report** and coordinate with QA team
5. **Re-run validation** after fixes
6. **Commit changes** when validation passes

## Exit Codes

The `run_validation.py` script returns:

- `0`: Validation passed (with or without warnings)
- `1`: Validation failed (critical issues found)

This allows integration with CI/CD pipelines:

```bash
python run_validation.py --rst-dir doc/en/user/source --md-dir doc/en/user
if [ $? -eq 0 ]; then
    echo "Validation passed"
else
    echo "Validation failed"
    exit 1
fi
```

## Troubleshooting

### "Sphinx not found" Error

Install Sphinx:
```bash
pip install sphinx
```

### "MkDocs not found" Error

Install MkDocs:
```bash
pip install mkdocs mkdocs-material
```

### Build Timeout

If builds take longer than 5 minutes, the validator will timeout. You can:
1. Build manually first, then use `--skip-build`
2. Modify the timeout in `validators.py` (search for `timeout=300`)

### Memory Issues

For large documentation sets, you may encounter memory issues. Try:
1. Validate one manual at a time (user, developer, docguide separately)
2. Use `--skip-roundtrip` to skip HTML comparison
3. Increase available memory for Python

## Integration with Migration Script

The validation scripts are designed to work with `migration.py`:

```python
from validators import (
    RoundTripValidator,
    LinkValidator,
    ImageValidator,
    generate_validation_report
)

# After conversion in migration.py
validator = RoundTripValidator(rst_dir, md_dir)
validator.build_sphinx_html()
validator.build_mkdocs_html()
report = validator.compare_content()

# Check if validation passed
if report.overall_status == ValidationStatus.FAILED:
    print("Validation failed - fix issues before committing")
else:
    print("Validation passed - safe to commit")
```

## Contributing

When adding new validation checks:

1. Add the check to the appropriate validator class
2. Update the data models if needed
3. Update the report generation
4. Add tests for the new check
5. Update this README

## License

These validation scripts are part of the GeoServer project and follow the same license.
