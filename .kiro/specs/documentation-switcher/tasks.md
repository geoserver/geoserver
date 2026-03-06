# Implementation Plan: Documentation Switcher

## Overview

Implement a documentation type switcher as a MkDocs Material theme customization. The switcher will be a dropdown component in the navigation header that allows users to navigate between user manual, developer manual, docguide, and swagger APIs.

## Tasks

- [x] 1. Set up theme directory structure
  - Create `doc/themes/geoserver/partials/` directory
  - Create `doc/themes/geoserver/overrides/partials/` directory
  - Create `doc/themes/geoserver/stylesheets/` directory if not exists
  - _Requirements: 1.1, 3.1_

- [ ] 2. Create switcher component template
  - [x] 2.1 Implement header-switcher.html Jinja2 template
    - Read `config.extra.doc_type` and `config.extra.doc_switcher` from configuration
    - Render dropdown button with current documentation type label
    - Render dropdown menu with all documentation type links
    - Mark active documentation type with CSS class
    - _Requirements: 1.2, 2.1, 2.2_
  
  - [ ]* 2.2 Write property test for configuration rendering
    - **Property 3: Complete Configuration Rendering**
    - **Validates: Requirements 2.1, 2.3**

- [ ] 3. Override Material theme header
  - [x] 3.1 Create header.html override template
    - Extend Material theme's base header template
    - Include switcher component after logo
    - Preserve existing header functionality
    - _Requirements: 1.1, 1.3_
  
  - [ ]* 3.2 Write property test for switcher presence
    - **Property 2: Switcher Presence Across Pages**
    - **Validates: Requirements 1.3**

- [x] 4. Implement CSS styling
  - [x] 4.1 Add switcher styles to extra.css
    - Style switcher button and dropdown container
    - Implement hover and focus states
    - Add responsive breakpoints for mobile devices
    - Use Material theme CSS variables for consistency
    - _Requirements: 3.1, 3.2, 3.3, 3.4_
  
  - [ ]* 4.2 Write property test for responsive behavior
    - **Property 4: Responsive Behavior**
    - **Validates: Requirements 3.3**

- [x] 5. Configure documentation types
  - [x] 5.1 Add switcher configuration to mkdocs.yml files
    - Add `extra.doc_type` and `extra.doc_switcher` to each documentation type's config
    - Define all four documentation types with labels and URLs
    - Ensure each config identifies its own type correctly
    - _Requirements: 2.2, 2.3, 2.4_
  
  - [ ]* 5.2 Write property test for active type display
    - **Property 1: Active Documentation Type Display**
    - **Validates: Requirements 1.2, 2.4**

- [x] 6. Checkpoint - Build and verify
  - Build all documentation types and verify switcher appears correctly
  - Test navigation between documentation types
  - Ensure all tests pass, ask the user if questions arise

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- The implementation uses Jinja2 templates, CSS, and YAML configuration
- No JavaScript required for basic functionality (CSS-only dropdown)
