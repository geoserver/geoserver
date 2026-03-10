"""
Central version configuration for GeoServer documentation.

This module provides version and release variables to mkdocs-macros-plugin,
ensuring consistent version numbers across all documentation manuals
(user, developer, docguide, and translations).

To update the version for a new release, simply change the values below.
"""

import os
import yaml
from pathlib import Path
from urllib.parse import urlparse


def define_env(env):
    """
    Define macros and variables for mkdocs-macros-plugin.
    
    This function is called by mkdocs-macros-plugin and allows us to
    define variables that will be available in all Markdown files.
    """
    
    # Central version configuration
    # Update these values when releasing a new version
    env.variables['version'] = '3.0'
    env.variables['release'] = '3.0.0'
    env.variables['snapshot'] = '3.0-SNAPSHOT'
    
    # Additional useful variables
    env.variables['geoserver_repo'] = 'https://github.com/geoserver/geoserver'
    env.variables['docs_url'] = 'https://docs.geoserver.org'
    
    # API base URL for REST API Swagger/OpenAPI specs
    # The Swagger UI is hosted at ../api/ and uses URL fragments to load specific YAML files
    # This resolves to the correct path regardless of where the documentation is deployed
    # Usage in Markdown: [API reference]({{ api_url }}/styles.yaml)
    env.variables['api_url'] = '../api/#1.0.0'
    
    # Load shared doc_switcher configuration
    # The doc_switcher provides navigation between different documentation types
    # (User Manual, Developer Manual, Documentation Guide, Swagger APIs)
    # This configuration is centralized in a single YAML file to avoid duplication
    # across the three mkdocs.yml files
    config_path = Path(__file__).parent / 'themes' / 'geoserver' / 'doc_switcher.yml'
    with open(config_path, 'r') as f:
        config = yaml.safe_load(f)
    
    # Extract base path for absolute URL construction
    base_path = extract_base_path(env)
    
    # Convert relative paths to absolute paths
    # This ensures doc_switcher links work correctly at any nesting level
    doc_switcher = []
    for entry in config['doc_switcher']:
        absolute_entry = entry.copy()
        # Skip external URLs (those starting with http/https)
        if not entry['url'].startswith('http'):
            # Convert relative path to absolute path
            absolute_entry['url'] = construct_absolute_path(
                base_path, 
                entry['url'], 
                entry.get('type', '')
            )
        doc_switcher.append(absolute_entry)
    
    # Inject doc_switcher into config.extra so it's available to theme templates
    # Theme templates can access this via {{ config.extra.doc_switcher }}
    env.conf['extra']['doc_switcher'] = doc_switcher


def extract_base_path(env):
    """
    Extract the base path for constructing absolute doc_switcher URLs.
    
    Priority:
    1. DOCS_BASE_PATH environment variable (for migration branch testing)
    2. Parse from site_url in mkdocs.yml (for production and local development)
    
    Examples:
    - site_url: https://docs.geoserver.org/3.0/en/user/ → /3.0/en/
    - site_url: https://petersmythe.github.io/geoserver/migration/3.0-rst-to-md/en/user/
      with DOCS_BASE_PATH → /geoserver/migration/3.0-rst-to-md/en/
    - DOCS_BASE_PATH=/geoserver/migration/3.0-rst-to-md → /geoserver/migration/3.0-rst-to-md/en/
    
    Args:
        env: The mkdocs-macros-plugin environment object
    
    Returns:
        str: Base path with leading slash and trailing slash (e.g., '/3.0/en/')
    """
    # Check for environment variable first (migration branch override)
    env_base_path = os.environ.get('DOCS_BASE_PATH', '').strip()
    if env_base_path:
        # Ensure leading slash
        if not env_base_path.startswith('/'):
            env_base_path = '/' + env_base_path
        # Add /en/ if not present (language directory)
        if not env_base_path.endswith('/en/'):
            env_base_path = env_base_path.rstrip('/') + '/en/'
        return env_base_path
    
    # Parse from site_url
    site_url = env.conf.get('site_url', '')
    if not site_url:
        # Fallback to root if no site_url configured
        return '/en/'
    
    # Parse URL and extract path component
    parsed = urlparse(site_url)
    path = parsed.path.rstrip('/')
    
    # Remove current doc type from the end (user, developer, docguide)
    current_doc_type = env.conf.get('extra', {}).get('doc_type', '')
    if current_doc_type and path.endswith('/' + current_doc_type):
        path = path[:-len(current_doc_type)-1]
    
    # Ensure trailing slash
    if not path.endswith('/'):
        path += '/'
    
    return path


def construct_absolute_path(base_path, relative_url, target_doc_type):
    """
    Construct an absolute path from base path and relative URL.

    This function converts relative doc_switcher URLs (e.g., '../developer/')
    into absolute paths that work at any nesting level. It removes the '../'
    prefix and combines the base path with the target path.

    Args:
        base_path: Base path extracted from site_url or environment (e.g., '/3.0/en/')
        relative_url: Relative URL from doc_switcher.yml (e.g., '../developer/')
        target_doc_type: Target documentation type (e.g., 'developer', 'user')

    Examples:
        - base_path='/3.0/en/', relative_url='../developer/', target='developer'
          → '/3.0/en/developer/'
        - base_path='/3.0/en/', relative_url='../user/api/', target='swagger'
          → '/3.0/en/user/api/'
        - base_path='/geoserver/migration/3.0-rst-to-md/en/', relative_url='../developer/'
          → '/geoserver/migration/3.0-rst-to-md/en/developer/'

    Returns:
        str: Absolute path with leading slash
    """
    # Remove ../ prefix (we're constructing from base_path, not navigating relatively)
    target_path = relative_url.replace('../', '')

    # Combine base_path with target path
    absolute_path = base_path.rstrip('/') + '/' + target_path.lstrip('/')

    # Ensure leading slash for root-relative URLs
    if not absolute_path.startswith('/'):
        absolute_path = '/' + absolute_path

    return absolute_path
