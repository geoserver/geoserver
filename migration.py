#!/usr/bin/env python3
"""
GeoServer Documentation Migration Script
Orchestrates one-time conversion from RST/Sphinx to Markdown/MkDocs
"""

import os
import sys
import subprocess
import glob
import shutil
import time
from pathlib import Path
from dataclasses import dataclass, field
from typing import List, Dict, Optional
from datetime import datetime
from enum import Enum


class ValidationStatus(Enum):
    """Validation status enumeration"""
    PASSED = "passed"
    FAILED = "failed"
    WARNING = "warning"


@dataclass
class ConversionResult:
    """Result of converting a single file or directory"""
    source_file: str
    output_file: str
    success: bool
    warnings: List[str] = field(default_factory=list)
    errors: List[str] = field(default_factory=list)
    unconverted_directives: List[str] = field(default_factory=list)
    line_count: int = 0
    conversion_time: float = 0.0


@dataclass
class ValidationReport:
    """Report of validation results"""
    total_files: int
    successful_conversions: int
    failed_conversions: int
    content_issues: List[str] = field(default_factory=list)
    missing_images: List[str] = field(default_factory=list)
    missing_code_blocks: List[str] = field(default_factory=list)
    broken_links: List[str] = field(default_factory=list)
    overall_status: ValidationStatus = ValidationStatus.PASSED


@dataclass
class MigrationReport:
    """Complete migration report"""
    branch: str
    start_time: datetime
    end_time: Optional[datetime] = None
    total_files_converted: int = 0
    total_lines_converted: int = 0
    conversion_results: List[ConversionResult] = field(default_factory=list)
    validation_report: Optional[ValidationReport] = None
    unconverted_directives: Dict[str, int] = field(default_factory=dict)
    summary: str = ""


class TranslationToolWrapper:
    """Wrapper for petersmythe/translate tool execution"""
    
    def __init__(self, doc_dir: str, doc_type: str, lang: str = "en"):
        self.doc_dir = Path(doc_dir)
        self.doc_type = doc_type
        self.lang = lang
        self.source_dir = self.doc_dir / "source"
        self.docs_dir = self.doc_dir / "docs"
        
    def count_rst_files(self) -> int:
        """Count RST files in source directory"""
        if not self.source_dir.exists():
            return 0
        return len(list(self.source_dir.rglob("*.rst")))
    
    def run_init(self) -> bool:
        """Initialize docs directory"""
        print(f"   Step 1: Initializing docs directory...")
        try:
            result = subprocess.run(
                [sys.executable, "-m", "mkdocs_translate.cli", "--log", "INFO", "init"],
                cwd=self.doc_dir,
                capture_output=True,
                text=True,
                check=False
            )
            if result.returncode != 0:
                print(f"   ERROR: Init failed: {result.stderr}")
                return False
            return True
        except Exception as e:
            print(f"   ERROR: Init exception: {e}")
            return False
    
    def run_scan(self) -> bool:
        """Scan RST files"""
        print(f"   Step 2: Scanning RST files...")
        try:
            result = subprocess.run(
                [sys.executable, "-m", "mkdocs_translate.cli", "--log", "INFO", "scan"],
                cwd=self.doc_dir,
                capture_output=True,
                text=True,
                check=False
            )
            if result.returncode != 0:
                print(f"   ERROR: Scan failed: {result.stderr}")
                return False
            return True
        except Exception as e:
            print(f"   ERROR: Scan exception: {e}")
            return False
    
    def run_migrate(self) -> bool:
        """Convert RST to Markdown"""
        print(f"   Step 3: Converting RST to Markdown...")
        try:
            result = subprocess.run(
                [sys.executable, "-m", "mkdocs_translate.cli", "--log", "INFO", "migrate"],
                cwd=self.doc_dir,
                capture_output=True,
                text=True,
                check=False
            )
            if result.returncode != 0:
                print(f"   ERROR: Migration failed: {result.stderr}")
                return False
            return True
        except Exception as e:
            print(f"   ERROR: Migration exception: {e}")
            return False
    
    def run_nav(self) -> bool:
        """Generate navigation YAML"""
        print(f"   Step 4: Generating navigation...")
        try:
            nav_file = self.doc_dir / "nav_generated.yml"
            with open(nav_file, 'w') as f:
                result = subprocess.run(
                    [sys.executable, "-m", "mkdocs_translate.cli", "nav"],
                    cwd=self.doc_dir,
                    stdout=f,
                    stderr=subprocess.PIPE,
                    text=True,
                    check=False
                )
            if result.returncode != 0:
                print(f"   ERROR: Nav generation failed: {result.stderr}")
                return False
            return True
        except Exception as e:
            print(f"   ERROR: Nav generation exception: {e}")
            return False
    
    def run_postprocess(self) -> bool:
        """Postprocess generated Markdown files"""
        print(f"   Step 5: Postprocessing generated Markdown files...")
        try:
            postprocess_script = """
from mkdocs_translate import translate
import glob, os
for md in glob.glob('docs/**/*.md', recursive=True):
    try:
        tmp = md + '.posttmp'
        translate.postprocess_rst_markdown(md, tmp)
        os.replace(tmp, md)
    except Exception as e:
        print('postprocess failed for', md, e)
print('   [OK] Postprocessing complete')
"""
            result = subprocess.run(
                [sys.executable, "-c", postprocess_script],
                cwd=self.doc_dir,
                capture_output=True,
                text=True,
                check=False
            )
            print(result.stdout)
            if result.returncode != 0:
                print(f"   WARNING: Postprocessing had issues: {result.stderr}")
            return True
        except Exception as e:
            print(f"   WARNING: Postprocessing exception: {e}")
            return True  # Non-critical
    
    def run_interpreted_text_postprocess(self) -> bool:
        """Postprocess interpreted text roles to convert to proper Markdown links"""
        print(f"   Step 6: Converting interpreted text roles to Markdown links...")
        try:
            import re
            import glob
            
            # Role mappings for GeoServer documentation
            role_mappings = {
                'website': 'https://geoserver.org/',
                'developer': 'https://docs.geoserver.org/latest/en/developer/',
                'user': '../user/',
                'api': 'api/',  # REST API YAML files in api/ subdirectory
                'geotools': 'https://docs.geotools.org/latest/userguide/',
                'geot': 'https://osgeo-org.atlassian.net/browse/GEOT-',  # GeoTools JIRA issues
                'wiki': 'https://github.com/geoserver/geoserver/wiki/',
                'geos': 'https://osgeo-org.atlassian.net/browse/GEOS-',  # JIRA issues
                'docguide': '../docguide/',  # Relative path to documentation guide
                'download_community': 'https://build.geoserver.org/geoserver/main/community-latest/',
                'download_extension': 'https://build.geoserver.org/geoserver/main/ext-latest/',
                ':download_community': 'https://build.geoserver.org/geoserver/main/community-latest/',  # With colon prefix
            }
            
            # Pattern: `text <url>`{.interpreted-text role="rolename"}
            pattern = r'`([^`]+) <([^>]+)>`\{\.interpreted-text role="([^"]+)"\}'
            
            # Pattern for simple role without URL: `text`{.interpreted-text role="rolename"}
            simple_pattern = r'`([^`]+)`\{\.interpreted-text role="([^"]+)"\}'
            
            converted_count = 0
            file_count = 0
            unknown_roles = {}
            
            for md_file in glob.glob(str(self.docs_dir / '**/*.md'), recursive=True):
                try:
                    with open(md_file, 'r', encoding='utf-8') as f:
                        content = f.read()
                    
                    original_content = content
                    file_converted = [0]
                    
                    def replace_role(match):
                        text = match.group(1)
                        url = match.group(2)
                        role = match.group(3)
                        
                        if role in role_mappings:
                            full_url = role_mappings[role] + url
                            file_converted[0] += 1
                            return f'[{text}]({full_url})'
                        else:
                            # Special handling for 'ref' role - convert to internal link
                            if role == 'ref':
                                # Extract anchor from URL (usually filename#anchor)
                                if '#' in url:
                                    file_converted[0] += 1
                                    return f'[{text}]({url})'
                                else:
                                    file_converted[0] += 1
                                    return f'[{text}](#{url})'
                            # Unknown role - track and leave as-is
                            unknown_roles[role] = unknown_roles.get(role, 0) + 1
                            return match.group(0)
                    
                    def replace_simple_role(match):
                        text = match.group(1)
                        role = match.group(2)
                        
                        # For download roles, just use the text as filename
                        if role in ['download_community', 'download_extension', ':download_community']:
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
                        
                        # Unknown simple role - track and leave as-is
                        unknown_roles[role] = unknown_roles.get(role, 0) + 1
                        return match.group(0)
                    
                    # Apply both patterns
                    content = re.sub(pattern, replace_role, content)
                    content = re.sub(simple_pattern, replace_simple_role, content)
                    
                    if content != original_content:
                        with open(md_file, 'w', encoding='utf-8') as f:
                            f.write(content)
                        file_count += 1
                        converted_count += file_converted[0]
                
                except Exception as e:
                    print(f"      WARNING: Failed to process {md_file}: {e}")
            
            # Report unknown roles summary
            if unknown_roles:
                print(f"      INFO: Unknown roles found: {dict(list(unknown_roles.items())[:5])}")
            
            print(f"   [OK] Converted {converted_count} interpreted text roles in {file_count} files")
            return True
            
        except Exception as e:
            print(f"   WARNING: Interpreted text postprocessing exception: {e}")
            return True  # Non-critical
    
    def run_frontmatter_postprocess(self) -> bool:
        """Add frontmatter to files that use mkdocs-macros variables"""
        print(f"   Step 7: Adding frontmatter to files with variables...")
        try:
            import re
            import glob
            
            # Pattern to detect mkdocs-macros variables
            variable_pattern = r'\{\{\s*(version|release)\s*\}\}'
            
            # Frontmatter to add
            frontmatter = "---\nrender_macros: true\n---\n\n"
            
            # Pattern to detect existing frontmatter
            existing_frontmatter_pattern = r'^---\n.*?\n---\n'
            
            added_count = 0
            skipped_count = 0
            
            for md_file in glob.glob(str(self.docs_dir / '**/*.md'), recursive=True):
                try:
                    with open(md_file, 'r', encoding='utf-8') as f:
                        content = f.read()
                    
                    # Check if file contains variables
                    if not re.search(variable_pattern, content):
                        continue
                    
                    # Check if frontmatter already exists
                    if re.match(existing_frontmatter_pattern, content, re.DOTALL):
                        # Check if render_macros is already set
                        if 'render_macros:' in content[:200]:  # Check first 200 chars
                            skipped_count += 1
                            continue
                        else:
                            # Has frontmatter but no render_macros - add it
                            content = re.sub(
                                r'^(---\n)',
                                r'\1render_macros: true\n',
                                content,
                                count=1
                            )
                    else:
                        # No frontmatter - add it
                        content = frontmatter + content
                    
                    with open(md_file, 'w', encoding='utf-8') as f:
                        f.write(content)
                    added_count += 1
                
                except Exception as e:
                    print(f"      WARNING: Failed to process {md_file}: {e}")
            
            print(f"   [OK] Added frontmatter to {added_count} files (skipped {skipped_count} with existing frontmatter)")
            return True
            
        except Exception as e:
            print(f"   WARNING: Frontmatter postprocessing exception: {e}")
            return True  # Non-critical
    
    def convert_directory(self) -> ConversionResult:
        """Execute full conversion pipeline for this directory"""
        start_time = time.time()
        result = ConversionResult(
            source_file=str(self.source_dir),
            output_file=str(self.docs_dir),
            success=False
        )
        
        # Count files
        rst_count = self.count_rst_files()
        print(f"   Found {rst_count} RST files")
        result.line_count = rst_count
        
        # Run conversion pipeline
        if not self.run_init():
            result.errors.append("Init step failed")
            return result
        
        if not self.run_scan():
            result.errors.append("Scan step failed")
            return result
        
        if not self.run_migrate():
            result.errors.append("Migrate step failed")
            return result
        
        if not self.run_nav():
            result.errors.append("Nav generation failed")
            return result
        
        self.run_postprocess()  # Non-critical
        self.run_interpreted_text_postprocess()  # Non-critical
        self.run_frontmatter_postprocess()  # Non-critical
        
        result.success = True
        result.conversion_time = time.time() - start_time
        return result


class MigrationOrchestrator:
    """Orchestrates the complete migration process"""
    
    # Documentation directories to process
    DOC_DIRS = [
        ("doc/en/docguide", "docguide", "en"),
        ("doc/en/developer", "developer", "en"),
        ("doc/en/user", "user", "en"),
    ]
    
    # Directive mappings for reference
    DIRECTIVE_MAPPINGS = {
        ":guilabel:": "**Label**",
        ":menuselection:": "**Menu → Item**",
        ":file:": "`path/to/file`",
        ":download:": "[text](file){download}",
        ".. code-block::": "```language",
        ".. note::": "!!! note",
        ".. warning::": "!!! warning",
        ".. tip::": "!!! tip",
        ".. only:: snapshot": '=== "Snapshot"',
        ".. only:: release": '=== "Release"',
    }
    
    # Variable substitutions
    VARIABLE_SUBSTITUTIONS = {
        "|version|": "{{ version }}",
        "|release|": "{{ release }}",
    }
    
    def __init__(self, branch: str):
        self.branch = branch
        self.report = MigrationReport(
            branch=branch,
            start_time=datetime.now()
        )
        self.project_root = Path.cwd()
    
    def validate_environment(self) -> bool:
        """Validate project structure and dependencies"""
        print("=== Validating Environment ===")
        
        # Check project structure
        if not (self.project_root / "doc").exists():
            print("ERROR: doc/ directory not found")
            return False
        
        print("✓ Project structure validated")
        
        # Check Python dependencies
        try:
            import mkdocs_translate
            print("✓ mkdocs_translate is installed")
        except ImportError:
            print("ERROR: mkdocs_translate not installed")
            print("Run: pip install git+https://github.com/petersmythe/translate.git")
            return False
        
        return True
    
    def convert_all_files(self) -> List[ConversionResult]:
        """Convert all RST files to Markdown"""
        print("\n=== Converting RST to Markdown ===\n")
        
        results = []
        
        for doc_dir, doc_type, lang in self.DOC_DIRS:
            doc_path = self.project_root / doc_dir
            source_path = doc_path / "source"
            
            if not doc_path.exists() or not source_path.exists():
                print(f"⚠ Skipping {lang} {doc_type} (directory not found: {doc_dir})")
                continue
            
            print(f"Converting {lang} {doc_type} documentation...")
            print(f"   Working directory: {doc_dir}")
            
            # Create wrapper and execute conversion
            wrapper = TranslationToolWrapper(str(doc_path), doc_type, lang)
            result = wrapper.convert_directory()
            results.append(result)
            
            if result.success:
                print(f"   ✓ Conversion complete for {lang} {doc_type}")
                self.report.total_files_converted += result.line_count
            else:
                print(f"   ✗ Conversion failed for {lang} {doc_type}")
                for error in result.errors:
                    print(f"      ERROR: {error}")
        
        self.report.conversion_results = results
        return results
    
    def validate_conversion(self) -> ValidationReport:
        """Validate conversion results"""
        print("\n=== Validating Conversion ===\n")
        
        total_files = 0
        successful = 0
        failed = 0
        
        for result in self.report.conversion_results:
            total_files += 1
            if result.success:
                successful += 1
            else:
                failed += 1
        
        status = ValidationStatus.PASSED if failed == 0 else ValidationStatus.FAILED
        
        validation_report = ValidationReport(
            total_files=total_files,
            successful_conversions=successful,
            failed_conversions=failed,
            overall_status=status
        )
        
        print(f"Total directories processed: {total_files}")
        print(f"Successful conversions: {successful}")
        print(f"Failed conversions: {failed}")
        print(f"Overall status: {status.value}")
        
        self.report.validation_report = validation_report
        return validation_report
    
    def generate_config(self) -> None:
        """Generate or update mkdocs.yml configurations"""
        print("\n=== Generating MkDocs Configurations ===\n")
        
        for doc_dir, doc_type, lang in self.DOC_DIRS:
            doc_path = self.project_root / doc_dir
            mkdocs_yml = doc_path / "mkdocs.yml"
            nav_generated = doc_path / "nav_generated.yml"
            
            if not doc_path.exists():
                continue
            
            print(f"Processing {doc_type} configuration...")
            
            # Check if nav_generated.yml exists
            if nav_generated.exists():
                print(f"   ✓ Navigation generated: {nav_generated}")
                print(f"   → Merge nav_generated.yml into mkdocs.yml manually")
            else:
                print(f"   ⚠ No nav_generated.yml found")
            
            # Note: Actual mkdocs.yml updates should be done manually or in a separate task
            # This is just reporting what needs to be done
    
    def create_summary_report(self) -> MigrationReport:
        """Create migration summary report"""
        print("\n=== Creating Migration Summary Report ===\n")
        
        self.report.end_time = datetime.now()
        duration = (self.report.end_time - self.report.start_time).total_seconds()
        
        # Build summary
        summary_lines = [
            f"Migration Summary for Branch: {self.branch}",
            f"=" * 60,
            f"Start Time: {self.report.start_time.strftime('%Y-%m-%d %H:%M:%S')}",
            f"End Time: {self.report.end_time.strftime('%Y-%m-%d %H:%M:%S')}",
            f"Duration: {duration:.2f} seconds",
            f"",
            f"Conversion Results:",
            f"  Total directories: {len(self.report.conversion_results)}",
            f"  Total RST files: {self.report.total_files_converted}",
            f"",
        ]
        
        for result in self.report.conversion_results:
            status = "✓" if result.success else "✗"
            summary_lines.append(
                f"  {status} {result.source_file}: {result.line_count} files "
                f"({result.conversion_time:.2f}s)"
            )
        
        if self.report.validation_report:
            summary_lines.extend([
                f"",
                f"Validation Results:",
                f"  Status: {self.report.validation_report.overall_status.value}",
                f"  Successful: {self.report.validation_report.successful_conversions}",
                f"  Failed: {self.report.validation_report.failed_conversions}",
            ])
        
        summary_lines.extend([
            f"",
            f"Next Steps:",
            f"  1. Review generated Markdown files in docs/ directories",
            f"  2. Merge nav_generated.yml into mkdocs.yml for each manual",
            f"  3. Test builds with: mkdocs build",
            f"  4. Validate links and images",
            f"  5. Commit converted files",
        ])
        
        self.report.summary = "\n".join(summary_lines)
        
        # Print summary
        print(self.report.summary)
        
        # Save to file
        report_file = self.project_root / f"migration_report_{self.branch}.txt"
        with open(report_file, 'w', encoding='utf-8') as f:
            f.write(self.report.summary)
        
        print(f"\n✓ Report saved to: {report_file}")
        
        return self.report
    
    def run(self) -> bool:
        """Execute complete migration workflow"""
        print("=" * 60)
        print("GeoServer Documentation Migration")
        print(f"Branch: {self.branch}")
        print("=" * 60)
        print()
        
        # Validate environment
        if not self.validate_environment():
            return False
        
        # Convert files
        results = self.convert_all_files()
        
        # Validate conversion
        validation = self.validate_conversion()
        
        # Generate configs
        self.generate_config()
        
        # Create summary report
        self.create_summary_report()
        
        # Return success if no failures
        return validation.overall_status != ValidationStatus.FAILED


def main():
    """Main entry point"""
    import argparse
    
    parser = argparse.ArgumentParser(
        description="GeoServer Documentation Migration Script"
    )
    parser.add_argument(
        "--branch",
        default="3.0",
        help="Branch name (default: 3.0)"
    )
    parser.add_argument(
        "--validate-only",
        action="store_true",
        help="Only run validation, skip conversion"
    )
    
    args = parser.parse_args()
    
    orchestrator = MigrationOrchestrator(args.branch)
    
    if args.validate_only:
        orchestrator.validate_conversion()
    else:
        success = orchestrator.run()
        sys.exit(0 if success else 1)


if __name__ == "__main__":
    main()
