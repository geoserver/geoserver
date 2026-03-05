"""
MkDocs hook to handle download files in documentation.

This hook scans Markdown files for download links and copies referenced files
from the source directory to the MkDocs output directory, ensuring download
links work in the built documentation.
"""

import os
import re
import shutil
from pathlib import Path
from typing import List, Set, Tuple
import logging

logger = logging.getLogger("mkdocs.plugins.download_files")


def scan_download_links(docs_dir: str) -> Set[Tuple[str, str]]:
    """
    Scan Markdown files for download references.
    
    Args:
        docs_dir: Path to the MkDocs docs directory
        
    Returns:
        Set of tuples (markdown_file_path, download_file_path) for all download links found
    """
    download_links = set()
    docs_path = Path(docs_dir)
    
    # Pattern to match Markdown links: [text](file.ext)
    # Matches common download file extensions
    # Excludes absolute paths starting with /
    # Excludes macros containing {{
    link_pattern = re.compile(
        r'\[([^\]]+)\]\(([^/){{][^){{]*\.(?:zip|xml|properties|sld|json|csv|yaml|yml|txt|sql|sh|bat|jar))\)',
        re.IGNORECASE
    )
    
    # Walk through all Markdown files
    for md_file in docs_path.rglob("*.md"):
        try:
            with open(md_file, 'r', encoding='utf-8') as f:
                content = f.read()
                
            # Find all download links in this file
            for match in link_pattern.finditer(content):
                link_text = match.group(1)
                link_path = match.group(2)
                
                # Skip external URLs (http://, https://, ftp://)
                if link_path.startswith(('http://', 'https://', 'ftp://')):
                    continue
                
                # Skip anchor links
                if link_path.startswith('#'):
                    continue
                
                # Get relative path from markdown file
                md_relative = md_file.relative_to(docs_path)
                
                # Store the relationship
                download_links.add((str(md_relative), link_path))
                
        except Exception as e:
            logger.warning(f"Error scanning {md_file}: {e}")
    
    return download_links


def copy_download_files(docs_dir: str, site_dir: str, download_links: Set[Tuple[str, str]]) -> int:
    """
    Copy download files from docs directory to site output directory.
    
    Args:
        docs_dir: Path to the MkDocs docs directory
        site_dir: Path to the MkDocs site output directory
        download_links: Set of (markdown_file, download_file) tuples
        
    Returns:
        Number of files successfully copied
    """
    docs_path = Path(docs_dir)
    site_path = Path(site_dir)
    copied_count = 0
    
    for md_file, download_file in download_links:
        # Get the directory containing the markdown file
        md_dir = Path(md_file).parent
        
        # Resolve the download file path relative to the markdown file
        source_file = docs_path / md_dir / download_file
        
        # Destination should mirror the source structure
        dest_file = site_path / md_dir / download_file
        
        # Check if source file exists
        if not source_file.exists():
            logger.warning(f"Download file not found: {source_file}")
            continue
        
        # Create destination directory if needed
        dest_file.parent.mkdir(parents=True, exist_ok=True)
        
        # Copy the file
        try:
            shutil.copy2(source_file, dest_file)
            logger.debug(f"Copied: {source_file} -> {dest_file}")
            copied_count += 1
        except Exception as e:
            logger.error(f"Error copying {source_file} to {dest_file}: {e}")
    
    return copied_count


def on_pre_build(config, **kwargs):
    """
    Hook called before the build starts.
    
    Scans for download links and logs information about files to be copied.
    """
    docs_dir = config.get('docs_dir', 'docs')
    
    logger.info("Scanning for download links in documentation...")
    download_links = scan_download_links(docs_dir)
    
    if download_links:
        logger.info(f"Found {len(download_links)} download file references")
        
        # Store in config for use in on_post_build
        config['_download_links'] = download_links
    else:
        logger.info("No download file references found")
        config['_download_links'] = set()


def copy_api_directory(config) -> int:
    """
    Copy the API directory (Swagger/OpenAPI specs) to the site output.
    
    The API directory contains OpenAPI YAML specifications that are referenced
    throughout the REST API documentation. This function copies the entire
    doc/en/api/ directory to the site output, preserving the directory structure.
    
    Only copies for the user manual build, as the API documentation is only
    accessible through the user manual (documentation switcher links to ../user/api/).
    
    Args:
        config: MkDocs configuration object
        
    Returns:
        Number of API files successfully copied
    """
    # Get the base documentation directory (doc/en/)
    docs_dir = Path(config.get('docs_dir', 'docs'))
    site_dir = Path(config.get('site_dir', 'site'))
    
    # Only copy API directory for user manual build
    # Check if this is the user manual by looking at the docs_dir path
    if 'user' not in str(docs_dir):
        logger.debug("Skipping API directory copy (not user manual build)")
        return 0
    
    # API directory is at doc/en/api/ (sibling to docs directory)
    # docs_dir is typically doc/en/user/docs, so we need to go up two levels to get to doc/en/
    api_source = docs_dir.parent.parent / 'api'
    api_dest = site_dir / 'api'
    
    if not api_source.exists():
        logger.warning(f"API directory not found at {api_source}")
        return 0
    
    copied_count = 0
    
    try:
        # Create destination directory
        api_dest.mkdir(parents=True, exist_ok=True)
        
        # Copy all files from api directory, preserving structure
        for item in api_source.rglob('*'):
            if item.is_file():
                # Calculate relative path from api_source
                rel_path = item.relative_to(api_source)
                dest_file = api_dest / rel_path
                
                # Create parent directories if needed
                dest_file.parent.mkdir(parents=True, exist_ok=True)
                
                # Copy the file
                shutil.copy2(item, dest_file)
                logger.debug(f"Copied API file: {rel_path}")
                copied_count += 1
        
        logger.info(f"Successfully copied {copied_count} API specification files")
        
    except Exception as e:
        logger.error(f"Error copying API directory: {e}")
    
    return copied_count


def on_post_build(config, **kwargs):
    """
    Hook called after the build completes.
    
    Copies download files from docs directory to site output directory,
    and copies the API directory (Swagger/OpenAPI specs) to the site output.
    """
    docs_dir = config.get('docs_dir', 'docs')
    site_dir = config.get('site_dir', 'site')
    download_links = config.get('_download_links', set())
    
    # Copy download files
    if download_links:
        logger.info(f"Copying {len(download_links)} download files to output directory...")
        copied_count = copy_download_files(docs_dir, site_dir, download_links)
        
        if copied_count > 0:
            logger.info(f"Successfully copied {copied_count} download files")
        else:
            logger.warning("No download files were copied")
    else:
        logger.info("No download files to copy")
    
    # Copy API directory (Swagger/OpenAPI specs)
    logger.info("Copying API specifications to output directory...")
    copy_api_directory(config)
