"""
Central version configuration for GeoServer documentation.

This module provides version and release variables to mkdocs-macros-plugin,
ensuring consistent version numbers across all documentation manuals
(user, developer, docguide, and translations).

To update the version for a new release, change the values below.
"""

import os
import re


def _detect_branch():
    # GitHub Actions gives us refs in the form refs/heads/<branch>
    github_ref = os.environ.get('GITHUB_REF', '')
    if github_ref.startswith('refs/heads/'):
        return github_ref.replace('refs/heads/', '', 1)
    if github_ref.startswith('refs/pull/'):
        # PR builds have head ref in GITHUB_HEAD_REF
        return os.environ.get('GITHUB_HEAD_REF', '')

    # Backwards compatibility / local testing
    branch = os.environ.get('BRANCH_NAME', '') or os.environ.get('DOCS_BRANCH', '')
    if branch:
        return branch

    # Fallback to main when unknown
    return 'main'


def _guess_is_snapshot(branch):
    b = branch.lower()
    if b == 'main':
        return True
    if b.endswith('.x'):
        return True
    if b.endswith('-snapshot'):
        return True
    if 'snapshot' in b:
        return True

    return False


def _guess_version(branch, default='3.0'):
    if branch == 'main':
        return default

    m = re.match(r'^(\d+\.\d+)\.x$', branch)
    if m:
        return m.group(1)

    m = re.match(r'^(\d+\.\d+)-snapshot$', branch, re.IGNORECASE)
    if m:
        return m.group(1)

    m = re.match(r'^(\d+\.\d+)(?:\.\d+)?$', branch)
    if m:
        return m.group(1)

    return default


def _guess_release(branch, version, default='3.0.0'):
    # Numeric release tag exact (3.1.2)
    if re.match(r'^\d+\.\d+\.\d+$', branch):
        return branch

    # branch major.minor format: 3.0 -> 3.0.0
    if re.match(r'^\d+\.\d+$', branch):
        return branch + '.0'

    # branch major.minor.x format: 3.0.x -> 3.0.0
    m = re.match(r'^(\d+\.\d+)\.x$', branch)
    if m:
        return m.group(1) + '.0'

    # snapshot branches: 3.0-SNAPSHOT -> 3.0.0
    m = re.match(r'^(\d+\.\d+)-snapshot$', branch, re.IGNORECASE)
    if m:
        return m.group(1) + '.0'

    return default


def define_env(env):
    """
    Define macros and variables for mkdocs-macros-plugin.

    This function is called by mkdocs-macros-plugin and allows us to
    define variables that will be available in all Markdown files.
    """

    # Determine branch and mode from environment
    branch = _detect_branch()

    is_snapshot = os.environ.get('DOCS_SNAPSHOT_MODE', '').lower() == 'snapshot'
    is_release = os.environ.get('DOCS_SNAPSHOT_MODE', '').lower() == 'release'

    release_branch = bool(re.match(r'^\d+\.\d+\.\d+$', branch))

    if not (is_snapshot or is_release):
        if release_branch:
            is_release = True
            is_snapshot = False
        else:
            is_snapshot = True
            is_release = False

    version = _guess_version(branch, default='3.0')

    # release for build artifacts is exact tag on true release branch, else derived or default
    if is_release and release_branch:
        release = branch
    else:
        release = _guess_release(branch, version, default='3.0.0')

    snapshot = f"{version}-SNAPSHOT"

    env.variables['branch'] = branch
    env.variables['is_snapshot'] = is_snapshot
    env.variables['is_release'] = is_release

    # Central version configuration
    env.variables['version'] = version
    env.variables['release'] = release
    env.variables['snapshot'] = snapshot

    # Branch used for nightly build URLs
    # Nightlies are served from the active branch (main, 3.0.x, 2.28.x, etc), not only main.
    # If we are on a snapshot branch (like 3.0-SNAPSHOT) fallback to main.
    if branch.lower().endswith('-snapshot'):
        build_branch = 'main'
    else:
        build_branch = branch

    # Download URL templates
    # Usage: [geoserver-{{ release }}-war.zip]({{ download_release }}war)
    env.variables['download_release'] = (
        'https://sourceforge.net/projects/geoserver/files/GeoServer/'
        + env.variables['release'] + '/geoserver-' + env.variables['release'] + '-'
    )
    env.variables['download_extension'] = (
        'https://sourceforge.net/projects/geoserver/files/GeoServer/'
        + env.variables['release'] + '/extensions/geoserver-' + env.variables['release'] + '-'
    )

    # Usage: [geoserver-{{ snapshot }}-war.zip]({{ nightly_release }}war)
    env.variables['nightly_release'] = (
        'https://build.geoserver.org/geoserver/'
        + build_branch + '/geoserver-' + env.variables['snapshot'] + '-'
    )
    env.variables['nightly_extension'] = (
        'https://build.geoserver.org/geoserver/'
        + build_branch + '/extensions/geoserver-' + env.variables['snapshot'] + '-'
    )
    env.variables['nightly_community'] = (
        'https://build.geoserver.org/geoserver/'
        + build_branch + '/community-latest/geoserver-' + env.variables['snapshot'] + '-'
    )

    # Additional useful variables
    env.variables['geoserver_repo'] = 'https://github.com/geoserver/geoserver'
    env.variables['docs_url'] = 'https://docs.geoserver.org'

    # API base URL for REST API Swagger/OpenAPI specs
    env.variables['api_url'] = '../../api'
    env.variables['api_url3'] = '../../../api'
    env.variables['api_url4'] = '../../../../api'

