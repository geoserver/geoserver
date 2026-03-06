#!/usr/bin/env python3
"""
Validation scripts for RST to Markdown migration.

This module provides validators to ensure conversion quality:
- RoundTripValidator: Compare Sphinx HTML vs MkDocs HTML
- LinkValidator: Check internal links, external links, and anchors
- ImageValidator: Verify image references and identify screenshots
"""

import os
import re
import subprocess
from dataclasses import dataclass, field
from datetime import datetime
from enum import Enum
from pathlib import Path
from typing import List, Dict, Optional, Set, Tuple
from urllib.parse import urlparse, urljoin
import hashlib


class ValidationStatus(Enum):
    """Overall validation status"""
    PASSED = "passed"
    FAILED = "failed"
    WARNING = "warning"


class LinkErrorType(Enum):
    """Types of link errors"""
    MISSING_FILE = "missing_file"
    BROKEN_ANCHOR = "broken_anchor"
    EXTERNAL_404 = "external_404"
    MALFORMED_URL = "malformed_url"


@dataclass
class ContentIssue:
    """Represents a content validation issue"""
    file_path: str
    line_number: int
    issue_type: str
    description: str
    severity: str  # "error", "warning", "info"


@dataclass
class BrokenLink:
    """Represents a broken link"""
    source_file: str
    line_number: int
    link_text: str
    target_url: str
    error_type: LinkErrorType
    suggestion: Optional[str] = None


@dataclass
class BrokenImage:
    """Represents a broken image reference"""
    markdown_file: str
    line_number: int
    image_path: str
    alt_text: str


@dataclass
class ImageReference:
    """Represents an image reference in documentation"""
    file_path: str
    markdown_file: str
    line_number: int
    alt_text: str
    width: Optional[int] = None
    height: Optional[int] = None
    file_size: int = 0
    is_screenshot: bool = False
    needs_update: bool = False


@dataclass
class ValidationReport:
    """Validation report for conversion"""
    total_files: int
    successful_conversions: int
    failed_conversions: int
    content_issues: List[ContentIssue] = field(default_factory=list)
    missing_images: List[str] = field(default_factory=list)
    missing_code_blocks: List[str] = field(default_factory=list)
    broken_links: List[BrokenLink] = field(default_factory=list)
    overall_status: ValidationStatus = ValidationStatus.PASSED

    def to_markdown(self) -> str:
        """Generate markdown report"""
        lines = [
            "# Validation Report",
            f"\nGenerated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}",
            f"\n## Summary",
            f"- Total Files: {self.total_files}",
            f"- Successful: {self.successful_conversions}",
            f"- Failed: {self.failed_conversions}",
            f"- Overall Status: {self.overall_status.value}",
        ]

        if self.content_issues:
            lines.append(f"\n## Content Issues ({len(self.content_issues)})")
            for issue in self.content_issues[:20]:  # Limit to first 20
                lines.append(f"- [{issue.severity.upper()}] {issue.file_path}:{issue.line_number} - {issue.description}")

        if self.broken_links:
            lines.append(f"\n## Broken Links ({len(self.broken_links)})")
            for link in self.broken_links[:20]:
                lines.append(f"- {link.source_file}:{link.line_number} - {link.target_url} ({link.error_type.value})")

        if self.missing_images:
            lines.append(f"\n## Missing Images ({len(self.missing_images)})")
            for img in self.missing_images[:20]:
                lines.append(f"- {img}")

        return "\n".join(lines)


@dataclass
class ScreenshotQAReport:
    """Screenshot QA report"""
    total_images: int
    screenshot_count: int
    diagram_count: int
    screenshots_by_page: Dict[str, List[ImageReference]] = field(default_factory=dict)
    flagged_for_update: List[ImageReference] = field(default_factory=list)
    broken_references: List[str] = field(default_factory=list)
    generation_date: datetime = field(default_factory=datetime.now)

    def to_markdown(self) -> str:
        """Generate markdown report"""
        lines = [
            "# Screenshot QA Report",
            f"\nGenerated: {self.generation_date.strftime('%Y-%m-%d %H:%M:%S')}",
            f"\n## Summary",
            f"- Total Images: {self.total_images}",
            f"- Screenshots: {self.screenshot_count}",
            f"- Diagrams: {self.diagram_count}",
            f"- Flagged for Update: {len(self.flagged_for_update)}",
            f"- Broken References: {len(self.broken_references)}",
        ]

        if self.screenshots_by_page:
            lines.append("\n## Screenshots by Page")
            for page, images in sorted(self.screenshots_by_page.items()):
                lines.append(f"\n### {page}")
                for img in images:
                    size_kb = img.file_size / 1024 if img.file_size > 0 else 0
                    lines.append(f"- `{img.file_path}` ({size_kb:.1f}KB) - {img.alt_text}")

        return "\n".join(lines)


class RoundTripValidator:
    """Validates conversion by comparing Sphinx HTML vs MkDocs HTML"""

    def __init__(self, rst_dir: str, md_dir: str):
        """
        Initialize validator with source and converted directories.

        Args:
            rst_dir: Directory containing RST source files
            md_dir: Directory containing converted Markdown files
        """
        self.rst_dir = Path(rst_dir)
        self.md_dir = Path(md_dir)
        self.sphinx_build_dir = self.rst_dir / "_build" / "html"
        self.mkdocs_build_dir = self.md_dir / "site"

    def build_sphinx_html(self) -> str:
        """Build HTML from original RST using Sphinx"""
        print(f"Building Sphinx HTML from {self.rst_dir}...")
        try:
            result = subprocess.run(
                ["make", "html"],
                cwd=self.rst_dir,
                capture_output=True,
                text=True,
                timeout=300
            )
            if result.returncode != 0:
                raise RuntimeError(f"Sphinx build failed: {result.stderr}")
            return str(self.sphinx_build_dir)
        except subprocess.TimeoutExpired:
            raise RuntimeError("Sphinx build timed out after 5 minutes")
        except FileNotFoundError:
            raise RuntimeError("Sphinx not found. Install with: pip install sphinx")

    def build_mkdocs_html(self) -> str:
        """Build HTML from converted Markdown using MkDocs"""
        print(f"Building MkDocs HTML from {self.md_dir}...")
        try:
            result = subprocess.run(
                ["mkdocs", "build", "--strict"],
                cwd=self.md_dir,
                capture_output=True,
                text=True,
                timeout=300
            )
            if result.returncode != 0:
                raise RuntimeError(f"MkDocs build failed: {result.stderr}")
            return str(self.mkdocs_build_dir)
        except subprocess.TimeoutExpired:
            raise RuntimeError("MkDocs build timed out after 5 minutes")
        except FileNotFoundError:
            raise RuntimeError("MkDocs not found. Install with: pip install mkdocs")

    def compare_content(self) -> ValidationReport:
        """Compare content structure and completeness"""
        print("Comparing Sphinx and MkDocs HTML outputs...")
        
        issues = []
        sphinx_files = self._get_html_files(self.sphinx_build_dir)
        mkdocs_files = self._get_html_files(self.mkdocs_build_dir)

        # Check for missing files
        sphinx_basenames = {f.stem for f in sphinx_files}
        mkdocs_basenames = {f.stem for f in mkdocs_files}

        missing_in_mkdocs = sphinx_basenames - mkdocs_basenames
        for basename in missing_in_mkdocs:
            issues.append(ContentIssue(
                file_path=basename,
                line_number=0,
                issue_type="missing_file",
                description=f"File present in Sphinx but missing in MkDocs",
                severity="error"
            ))

        # Compare file pairs
        for sphinx_file in sphinx_files:
            mkdocs_file = self._find_matching_file(sphinx_file, mkdocs_files)
            if mkdocs_file:
                file_issues = self._compare_html_files(sphinx_file, mkdocs_file)
                issues.extend(file_issues)

        report = ValidationReport(
            total_files=len(sphinx_files),
            successful_conversions=len(sphinx_files) - len(missing_in_mkdocs),
            failed_conversions=len(missing_in_mkdocs),
            content_issues=issues
        )

        if issues:
            error_count = sum(1 for i in issues if i.severity == "error")
            report.overall_status = ValidationStatus.FAILED if error_count > 0 else ValidationStatus.WARNING

        return report

    def validate_images(self) -> List[str]:
        """Verify all images present in both versions"""
        print("Validating images...")
        missing_images = []

        sphinx_images = self._find_images(self.sphinx_build_dir)
        mkdocs_images = self._find_images(self.mkdocs_build_dir)

        for img in sphinx_images:
            if img not in mkdocs_images:
                missing_images.append(img)

        return missing_images

    def validate_code_blocks(self) -> List[str]:
        """Verify all code blocks present in both versions"""
        print("Validating code blocks...")
        missing_blocks = []

        # This is a simplified check - in practice, you'd parse HTML more thoroughly
        sphinx_blocks = self._count_code_blocks(self.sphinx_build_dir)
        mkdocs_blocks = self._count_code_blocks(self.mkdocs_build_dir)

        if sphinx_blocks != mkdocs_blocks:
            missing_blocks.append(
                f"Code block count mismatch: Sphinx={sphinx_blocks}, MkDocs={mkdocs_blocks}"
            )

        return missing_blocks

    def validate_links(self) -> List[BrokenLink]:
        """Verify all links work in both versions"""
        # This delegates to LinkValidator for detailed link checking
        link_validator = LinkValidator(str(self.mkdocs_build_dir))
        return link_validator.validate_internal_links()

    def _get_html_files(self, directory: Path) -> List[Path]:
        """Get all HTML files in directory"""
        if not directory.exists():
            return []
        return list(directory.rglob("*.html"))

    def _find_matching_file(self, sphinx_file: Path, mkdocs_files: List[Path]) -> Optional[Path]:
        """Find matching MkDocs file for Sphinx file"""
        basename = sphinx_file.stem
        for mkdocs_file in mkdocs_files:
            if mkdocs_file.stem == basename:
                return mkdocs_file
        return None

    def _compare_html_files(self, sphinx_file: Path, mkdocs_file: Path) -> List[ContentIssue]:
        """Compare two HTML files for content differences"""
        issues = []

        try:
            sphinx_content = sphinx_file.read_text(encoding='utf-8')
            mkdocs_content = mkdocs_file.read_text(encoding='utf-8')

            # Simple heuristic checks
            sphinx_text = self._extract_text_content(sphinx_content)
            mkdocs_text = self._extract_text_content(mkdocs_content)

            # Check for significant content length differences
            length_diff = abs(len(sphinx_text) - len(mkdocs_text)) / max(len(sphinx_text), 1)
            if length_diff > 0.2:  # More than 20% difference
                issues.append(ContentIssue(
                    file_path=str(sphinx_file.relative_to(self.sphinx_build_dir)),
                    line_number=0,
                    issue_type="content_length_mismatch",
                    description=f"Content length differs by {length_diff*100:.1f}%",
                    severity="warning"
                ))

        except Exception as e:
            issues.append(ContentIssue(
                file_path=str(sphinx_file),
                line_number=0,
                issue_type="comparison_error",
                description=f"Error comparing files: {str(e)}",
                severity="error"
            ))

        return issues

    def _extract_text_content(self, html: str) -> str:
        """Extract text content from HTML (simple regex-based)"""
        # Remove script and style tags
        html = re.sub(r'<script[^>]*>.*?</script>', '', html, flags=re.DOTALL)
        html = re.sub(r'<style[^>]*>.*?</style>', '', html, flags=re.DOTALL)
        # Remove HTML tags
        text = re.sub(r'<[^>]+>', '', html)
        # Normalize whitespace
        text = re.sub(r'\s+', ' ', text)
        return text.strip()

    def _find_images(self, directory: Path) -> Set[str]:
        """Find all image references in HTML files"""
        images = set()
        if not directory.exists():
            return images

        for html_file in directory.rglob("*.html"):
            content = html_file.read_text(encoding='utf-8')
            # Find img src attributes
            img_matches = re.findall(r'<img[^>]+src=["\']([^"\']+)["\']', content)
            images.update(img_matches)

        return images

    def _count_code_blocks(self, directory: Path) -> int:
        """Count code blocks in HTML files"""
        count = 0
        if not directory.exists():
            return count

        for html_file in directory.rglob("*.html"):
            content = html_file.read_text(encoding='utf-8')
            # Count <pre> or <code> blocks
            count += len(re.findall(r'<pre[^>]*>|<code[^>]*class=["\'][^"\']*language-', content))

        return count


class LinkValidator:
    """Validates link integrity in documentation"""

    def __init__(self, site_dir: str):
        """
        Initialize link validator.

        Args:
            site_dir: Directory containing built HTML site
        """
        self.site_dir = Path(site_dir)
        self.all_pages: Set[str] = set()
        self.all_anchors: Dict[str, Set[str]] = {}

    def validate_internal_links(self) -> List[BrokenLink]:
        """Check all internal links resolve"""
        print("Validating internal links...")
        broken_links = []

        # First pass: collect all pages and anchors
        self._collect_pages_and_anchors()

        # Second pass: validate links
        for html_file in self.site_dir.rglob("*.html"):
            file_broken_links = self._validate_links_in_file(html_file)
            broken_links.extend(file_broken_links)

        return broken_links

    def validate_external_links(self) -> List[BrokenLink]:
        """Check external links are accessible (basic check)"""
        print("Validating external links...")
        broken_links = []

        for html_file in self.site_dir.rglob("*.html"):
            content = html_file.read_text(encoding='utf-8')
            
            # Find external links
            link_pattern = r'<a[^>]+href=["\']([^"\']+)["\'][^>]*>([^<]*)</a>'
            for match in re.finditer(link_pattern, content):
                url = match.group(1)
                link_text = match.group(2)
                
                if self._is_external_url(url):
                    # Basic validation - check if URL is well-formed
                    if not self._is_valid_url(url):
                        broken_links.append(BrokenLink(
                            source_file=str(html_file.relative_to(self.site_dir)),
                            line_number=self._get_line_number(content, match.start()),
                            link_text=link_text,
                            target_url=url,
                            error_type=LinkErrorType.MALFORMED_URL
                        ))

        return broken_links

    def validate_anchors(self) -> List[BrokenLink]:
        """Check section anchors exist"""
        print("Validating anchors...")
        broken_links = []

        for html_file in self.site_dir.rglob("*.html"):
            content = html_file.read_text(encoding='utf-8')
            relative_path = str(html_file.relative_to(self.site_dir))
            
            # Find anchor links
            anchor_pattern = r'<a[^>]+href=["\']#([^"\']+)["\'][^>]*>([^<]*)</a>'
            for match in re.finditer(anchor_pattern, content):
                anchor = match.group(1)
                link_text = match.group(2)
                
                # Check if anchor exists in this file
                if relative_path in self.all_anchors:
                    if anchor not in self.all_anchors[relative_path]:
                        broken_links.append(BrokenLink(
                            source_file=relative_path,
                            line_number=self._get_line_number(content, match.start()),
                            link_text=link_text,
                            target_url=f"#{anchor}",
                            error_type=LinkErrorType.BROKEN_ANCHOR
                        ))

        return broken_links

    def _collect_pages_and_anchors(self):
        """Collect all pages and their anchors"""
        for html_file in self.site_dir.rglob("*.html"):
            relative_path = str(html_file.relative_to(self.site_dir))
            self.all_pages.add(relative_path)
            
            content = html_file.read_text(encoding='utf-8')
            anchors = set()
            
            # Find all id attributes (potential anchors)
            id_pattern = r'id=["\']([^"\']+)["\']'
            for match in re.finditer(id_pattern, content):
                anchors.add(match.group(1))
            
            self.all_anchors[relative_path] = anchors

    def _validate_links_in_file(self, html_file: Path) -> List[BrokenLink]:
        """Validate all links in a single HTML file"""
        broken_links = []
        content = html_file.read_text(encoding='utf-8')
        relative_path = str(html_file.relative_to(self.site_dir))
        
        # Find all links
        link_pattern = r'<a[^>]+href=["\']([^"\']+)["\'][^>]*>([^<]*)</a>'
        for match in re.finditer(link_pattern, content):
            url = match.group(1)
            link_text = match.group(2)
            
            # Skip external links and anchors (handled separately)
            if self._is_external_url(url) or url.startswith('#'):
                continue
            
            # Check if target file exists
            target_path = self._resolve_link(relative_path, url)
            if target_path and target_path not in self.all_pages:
                broken_links.append(BrokenLink(
                    source_file=relative_path,
                    line_number=self._get_line_number(content, match.start()),
                    link_text=link_text,
                    target_url=url,
                    error_type=LinkErrorType.MISSING_FILE,
                    suggestion=self._suggest_fix(target_path)
                ))

        return broken_links

    def _is_external_url(self, url: str) -> bool:
        """Check if URL is external"""
        return url.startswith(('http://', 'https://', 'ftp://', '//'))

    def _is_valid_url(self, url: str) -> bool:
        """Check if URL is well-formed"""
        try:
            result = urlparse(url)
            return all([result.scheme, result.netloc])
        except:
            return False

    def _resolve_link(self, source_file: str, link: str) -> Optional[str]:
        """Resolve relative link to absolute path"""
        try:
            source_dir = Path(source_file).parent
            # Remove anchor if present
            link_path = link.split('#')[0]
            if not link_path:
                return None
            
            # Resolve relative path
            target = (source_dir / link_path).resolve()
            return str(target.relative_to(self.site_dir.resolve()))
        except:
            return None

    def _suggest_fix(self, broken_path: str) -> Optional[str]:
        """Suggest a fix for broken link"""
        # Simple suggestion: find similar filenames
        broken_name = Path(broken_path).stem
        for page in self.all_pages:
            if broken_name.lower() in page.lower():
                return f"Did you mean: {page}?"
        return None

    def _get_line_number(self, content: str, position: int) -> int:
        """Get line number for character position"""
        return content[:position].count('\n') + 1


class ImageValidator:
    """Validates image references and identifies screenshots"""

    def __init__(self, docs_dir: str):
        """
        Initialize image validator.

        Args:
            docs_dir: Directory containing Markdown documentation
        """
        self.docs_dir = Path(docs_dir)

    def scan_images(self) -> List[ImageReference]:
        """Find all image references in Markdown files"""
        print("Scanning for image references...")
        images = []

        for md_file in self.docs_dir.rglob("*.md"):
            content = md_file.read_text(encoding='utf-8')
            
            # Find Markdown image syntax: ![alt](path)
            img_pattern = r'!\[([^\]]*)\]\(([^)]+)\)'
            for match in re.finditer(img_pattern, content):
                alt_text = match.group(1)
                img_path = match.group(2)
                line_number = self._get_line_number(content, match.start())
                
                # Resolve image path
                full_path = self._resolve_image_path(md_file, img_path)
                
                image_ref = ImageReference(
                    file_path=img_path,
                    markdown_file=str(md_file.relative_to(self.docs_dir)),
                    line_number=line_number,
                    alt_text=alt_text
                )
                
                # Get file info if image exists
                if full_path and full_path.exists():
                    image_ref.file_size = full_path.stat().st_size
                    image_ref.is_screenshot = self._is_screenshot(full_path, alt_text)
                    image_ref.needs_update = image_ref.is_screenshot
                
                images.append(image_ref)

        return images

    def validate_references(self, images: List[ImageReference]) -> List[BrokenImage]:
        """Check all referenced images exist"""
        print("Validating image references...")
        broken_images = []

        for img_ref in images:
            full_path = self._resolve_image_path(
                self.docs_dir / img_ref.markdown_file,
                img_ref.file_path
            )
            
            if not full_path or not full_path.exists():
                broken_images.append(BrokenImage(
                    markdown_file=img_ref.markdown_file,
                    line_number=img_ref.line_number,
                    image_path=img_ref.file_path,
                    alt_text=img_ref.alt_text
                ))

        return broken_images

    def identify_screenshots(self, images: List[ImageReference]) -> List[ImageReference]:
        """Identify GUI screenshots for QA"""
        print("Identifying screenshots...")
        screenshots = [img for img in images if img.is_screenshot]
        return screenshots

    def generate_screenshot_report(self, images: List[ImageReference]) -> ScreenshotQAReport:
        """Generate report for screenshot QA"""
        print("Generating screenshot QA report...")
        
        screenshots = self.identify_screenshots(images)
        diagrams = [img for img in images if not img.is_screenshot]
        
        # Group screenshots by page
        screenshots_by_page = {}
        for img in screenshots:
            page = img.markdown_file
            if page not in screenshots_by_page:
                screenshots_by_page[page] = []
            screenshots_by_page[page].append(img)
        
        # Find broken references
        broken_refs = []
        for img in images:
            full_path = self._resolve_image_path(
                self.docs_dir / img.markdown_file,
                img.file_path
            )
            if not full_path or not full_path.exists():
                broken_refs.append(img.file_path)
        
        return ScreenshotQAReport(
            total_images=len(images),
            screenshot_count=len(screenshots),
            diagram_count=len(diagrams),
            screenshots_by_page=screenshots_by_page,
            flagged_for_update=screenshots,
            broken_references=broken_refs
        )

    def _resolve_image_path(self, md_file: Path, img_path: str) -> Optional[Path]:
        """Resolve image path relative to Markdown file"""
        try:
            # Handle absolute paths from docs root
            if img_path.startswith('/'):
                return self.docs_dir / img_path.lstrip('/')
            
            # Handle relative paths
            md_dir = md_file.parent
            full_path = (md_dir / img_path).resolve()
            
            # Check if path is within docs directory
            if self.docs_dir.resolve() in full_path.parents or full_path == self.docs_dir.resolve():
                return full_path
            
            return None
        except:
            return None

    def _is_screenshot(self, img_path: Path, alt_text: str) -> bool:
        """Heuristic detection of GUI screenshots"""
        # Check filename patterns
        filename_lower = img_path.name.lower()
        screenshot_patterns = [
            'screenshot', 'screen', 'webadmin', 'gui', 'ui', 'dialog',
            'window', 'panel', 'menu', 'button', 'form', 'editor'
        ]
        
        for pattern in screenshot_patterns:
            if pattern in filename_lower or pattern in alt_text.lower():
                return True
        
        # Check file extension (screenshots are typically PNG or JPG)
        if img_path.suffix.lower() in ['.png', '.jpg', '.jpeg']:
            # Additional heuristic: larger files are more likely screenshots
            if img_path.exists() and img_path.stat().st_size > 10000:  # > 10KB
                return True
        
        return False

    def _get_line_number(self, content: str, position: int) -> int:
        """Get line number for character position"""
        return content[:position].count('\n') + 1


def generate_validation_report(
    round_trip_report: ValidationReport,
    link_issues: List[BrokenLink],
    image_issues: List[BrokenImage],
    output_file: str = "validation_report.md"
) -> None:
    """
    Generate comprehensive validation report.

    Args:
        round_trip_report: Report from RoundTripValidator
        link_issues: Broken links from LinkValidator
        image_issues: Broken images from ImageValidator
        output_file: Output file path
    """
    print(f"Generating validation report: {output_file}")
    
    # Merge all issues into round_trip_report
    round_trip_report.broken_links = link_issues
    round_trip_report.missing_images = [img.image_path for img in image_issues]
    
    # Update overall status
    total_errors = (
        len([i for i in round_trip_report.content_issues if i.severity == "error"]) +
        len(link_issues) +
        len(image_issues)
    )
    
    if total_errors > 0:
        round_trip_report.overall_status = ValidationStatus.FAILED
    elif round_trip_report.content_issues:
        round_trip_report.overall_status = ValidationStatus.WARNING
    else:
        round_trip_report.overall_status = ValidationStatus.PASSED
    
    # Write report
    report_content = round_trip_report.to_markdown()
    
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write(report_content)
    
    print(f"Validation report written to {output_file}")
    print(f"Overall status: {round_trip_report.overall_status.value}")


if __name__ == "__main__":
    # Example usage
    print("Validation Scripts for RST to Markdown Migration")
    print("=" * 50)
    print("\nUsage examples:")
    print("\n1. Round-trip validation:")
    print("   validator = RoundTripValidator('doc/en/user/source', 'doc/en/user')")
    print("   validator.build_sphinx_html()")
    print("   validator.build_mkdocs_html()")
    print("   report = validator.compare_content()")
    print("\n2. Link validation:")
    print("   link_validator = LinkValidator('doc/en/user/site')")
    print("   broken_links = link_validator.validate_internal_links()")
    print("\n3. Image validation:")
    print("   img_validator = ImageValidator('doc/en/user')")
    print("   images = img_validator.scan_images()")
    print("   screenshot_report = img_validator.generate_screenshot_report(images)")
