"""
Central version configuration for GeoServer documentation.

This module provides version and release variables to mkdocs-macros-plugin,
ensuring consistent version numbers across all documentation manuals
(user, developer, docguide, and translations).

To update the version for a new release, simply change the values below.
"""

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
    
    # Additional useful variables
    env.variables['geoserver_repo'] = 'https://github.com/geoserver/geoserver'
    env.variables['docs_url'] = 'https://docs.geoserver.org'
