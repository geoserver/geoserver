# Implementation Plan: Doc Switcher Centralization

## Overview

Centralize the duplicated `doc_switcher` configuration from three MkDocs configuration files into a single shared YAML file. This is a straightforward configuration refactoring that eliminates duplication while maintaining backward compatibility.

## Tasks

- [x] 1. Create shared doc_switcher configuration file
  - Create `doc/themes/geoserver/doc_switcher.yml` with the doc_switcher array
  - Include comments explaining the file's purpose and usage
  - Copy the existing doc_switcher structure from any of the three mkdocs.yml files
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [x] 2. Update version.py to load shared configuration
  - Add YAML loading code to the `define_env()` function in `doc/version.py`
  - Load `doc/themes/geoserver/doc_switcher.yml` using relative path from version.py location
  - Inject the doc_switcher data into `env.variables['doc_switcher']`
  - Add inline comments explaining the loading logic
  - _Requirements: 2.1, 2.2, 2.5, 3.3_

- [x] 3. Update MkDocs configuration files
  - [x] 3.1 Update user manual configuration
    - Remove the `doc_switcher` array from `doc/en/user/mkdocs.yml`
    - Add comment explaining doc_switcher is loaded by macros plugin
    - Verify `doc_type: "user"` remains in the file
    - _Requirements: 3.1, 3.2, 3.4, 3.5_
  
  - [x] 3.2 Update developer manual configuration
    - Remove the `doc_switcher` array from `doc/en/developer/mkdocs.yml`
    - Add comment explaining doc_switcher is loaded by macros plugin
    - Verify `doc_type: "developer"` remains in the file
    - _Requirements: 3.1, 3.2, 3.4, 3.5_
  
  - [x] 3.3 Update docguide manual configuration
    - Remove the `doc_switcher` array from `doc/en/docguide/mkdocs.yml`
    - Add comment explaining doc_switcher is loaded by macros plugin
    - Verify `doc_type: "docguide"` remains in the file
    - _Requirements: 3.1, 3.2, 3.4, 3.5_

- [x] 4. Manual testing and verification
  - Build all three manuals using `mkdocs build` and verify no errors
  - Test local development using `mkdocs serve` for each manual
  - Verify doc_switcher navigation appears correctly in rendered HTML
  - Verify all four links work (User Manual, Developer Manual, Documentation Guide, Swagger APIs)
  - Compare rendered output before and after to ensure consistency
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 6.1, 6.2, 6.3, 6.5_

- [ ]* 5. Write unit tests for configuration loading
  - Test that doc_switcher.yml can be loaded successfully
  - Test that loaded data has expected structure (list with label, url, type fields)
  - Test error handling for missing file or invalid YAML
  - _Requirements: 2.3, 2.4, 5.1, 5.2, 5.3, 5.4, 5.5_

- [ ]* 6. Update developer documentation
  - Document the centralized configuration approach
  - Explain how to modify doc_switcher (edit shared file, not individual mkdocs.yml)
  - Document the file path relationships
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

## Notes

- Tasks marked with `*` are optional and can be skipped for faster implementation
- This is a simple configuration refactoring with minimal code changes
- The macros plugin and PyYAML are already installed, no new dependencies needed
- Manual testing (task 4) is the most critical validation step
- Error handling relies on Python's built-in exceptions (FileNotFoundError, KeyError, yaml.YAMLError)
