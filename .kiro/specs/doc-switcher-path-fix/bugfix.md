# Bugfix Requirements Document

## Introduction

The GeoServer documentation switcher uses relative paths (`../user/`, `../developer/`, etc.) in the centralized `doc_switcher.yml` configuration file. These relative paths work correctly at the first nesting level (e.g., `/en/user/`) but fail at deeper nesting levels. When a user navigates to a page at level 2 or deeper (e.g., `/en/user/introduction/` or `/en/user/introduction/overview/`) and attempts to switch documentation types, the relative path resolution is incorrect, resulting in 404 errors.

The bug affects the multi-branch deployment structure where documentation is deployed at paths like `https://petersmythe.github.io/geoserver/migration/3.0-rst-to-md/en/user/`. The `../` operator only goes up one directory level, so at deeper nesting, it cannot reach the correct target directory.

## Bug Analysis

### Current Behavior (Defect)

1.1 WHEN a user is at nesting level 2 (e.g., `/en/user/introduction/`) AND clicks the "Developer Manual" switcher link THEN the system navigates to `/en/user/developer/` resulting in a 404 error

1.2 WHEN a user is at nesting level 3 (e.g., `/en/user/introduction/overview/`) AND clicks the "Developer Manual" switcher link THEN the system navigates to `/en/user/introduction/developer/` resulting in a 404 error

1.3 WHEN a user is at nesting level 2 or deeper AND clicks any doc_switcher link THEN the system applies relative path resolution from the current page location instead of from the documentation root

1.4 WHEN a user is at any nesting level greater than 1 AND clicks the "Documentation Guide" switcher link THEN the system constructs an incorrect path that includes parent directories from the current location

### Expected Behavior (Correct)

2.1 WHEN a user is at nesting level 2 (e.g., `/en/user/introduction/`) AND clicks the "Developer Manual" switcher link THEN the system SHALL navigate to `/en/developer/` (or the full path including branch prefix if applicable)

2.2 WHEN a user is at nesting level 3 (e.g., `/en/user/introduction/overview/`) AND clicks the "Developer Manual" switcher link THEN the system SHALL navigate to `/en/developer/` (or the full path including branch prefix if applicable)

2.3 WHEN a user is at any nesting level AND clicks any doc_switcher link THEN the system SHALL resolve the path from the documentation root, not from the current page location

2.4 WHEN a user is at any nesting level AND clicks the "Documentation Guide" switcher link THEN the system SHALL navigate to the correct absolute path `/en/docguide/` (or the full path including branch prefix if applicable)

2.5 WHEN the documentation is deployed on a multi-branch structure (e.g., `/migration/3.0-rst-to-md/en/user/`) AND a user clicks any doc_switcher link THEN the system SHALL preserve the branch prefix in the target URL

### Unchanged Behavior (Regression Prevention)

3.1 WHEN a user is at nesting level 1 (e.g., `/en/user/`) AND clicks any doc_switcher link THEN the system SHALL CONTINUE TO navigate to the correct documentation type

3.2 WHEN a user switches between documentation types THEN the system SHALL CONTINUE TO maintain the same language context (e.g., `/en/`)

3.3 WHEN the doc_switcher.yml configuration is loaded by the MkDocs macros plugin THEN the system SHALL CONTINUE TO inject the doc_switcher data into the extra section of each mkdocs.yml

3.4 WHEN the documentation templates access `extra.doc_switcher` THEN the system SHALL CONTINUE TO provide the doc_switcher array with label, url, and type fields

3.5 WHEN the documentation is built with `mkdocs build` or `mkdocs serve` THEN the system SHALL CONTINUE TO process the centralized doc_switcher.yml without requiring additional command-line arguments
