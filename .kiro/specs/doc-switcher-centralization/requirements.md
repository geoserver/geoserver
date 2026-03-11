# Requirements Document

## Introduction

The GeoServer documentation system currently maintains three separate MkDocs configuration files (user, developer, and docguide manuals) that each contain identical `doc_switcher` configuration. This duplication creates maintenance overhead, as any change to the documentation switcher menu must be replicated across all three files. This feature will centralize the `doc_switcher` configuration into a single shared YAML file that can be referenced by all MkDocs configuration files, reducing duplication and simplifying maintenance.

## Glossary

- **MkDocs**: A static site generator for building documentation from Markdown files
- **MkDocs_Material**: The Material theme for MkDocs used by GeoServer documentation
- **Doc_Switcher**: A navigation component that allows users to switch between different documentation types (User Manual, Developer Manual, Documentation Guide, Swagger APIs)
- **Macros_Plugin**: The MkDocs macros plugin that enables Python-based template processing and variable injection
- **Config_File**: An MkDocs YAML configuration file (mkdocs.yml)
- **Shared_Config**: A centralized YAML file containing the doc_switcher configuration
- **Doc_Type**: A unique identifier for each documentation manual type (user, developer, docguide)

## Requirements

### Requirement 1: Create Shared Configuration File

**User Story:** As a documentation maintainer, I want a single source of truth for doc_switcher configuration, so that I only need to update one file when making changes.

#### Acceptance Criteria

1. THE System SHALL create a file at `doc/themes/geoserver/doc_switcher.yml`
2. THE Shared_Config SHALL contain the complete doc_switcher array structure
3. THE Shared_Config SHALL include all four documentation types: User Manual, Developer Manual, Documentation Guide, and Swagger APIs
4. FOR EACH documentation type entry, THE Shared_Config SHALL include label, url, and type fields
5. THE Shared_Config SHALL use valid YAML syntax

### Requirement 2: Load Shared Configuration

**User Story:** As a documentation build system, I want to load the shared doc_switcher configuration at build time, so that all documentation sites use consistent navigation.

#### Acceptance Criteria

1. WHEN a Config_File is processed, THE Macros_Plugin SHALL load the Shared_Config file
2. THE Macros_Plugin SHALL parse the Shared_Config as YAML data
3. IF the Shared_Config file cannot be found, THEN THE System SHALL raise a descriptive error message
4. IF the Shared_Config contains invalid YAML, THEN THE System SHALL raise a descriptive error message
5. THE System SHALL make the loaded doc_switcher data available to MkDocs templates

### Requirement 3: Integrate with Existing Configuration Files

**User Story:** As a documentation maintainer, I want the existing mkdocs.yml files to reference the shared configuration, so that duplication is eliminated.

#### Acceptance Criteria

1. THE Config_File SHALL retain the doc_type field as a local configuration value
2. THE Config_File SHALL NOT contain a hardcoded doc_switcher array
3. WHEN the Macros_Plugin processes a Config_File, THE System SHALL inject the doc_switcher data from Shared_Config into the extra section
4. THE System SHALL preserve all other existing configuration values in each Config_File
5. THE System SHALL apply this integration to all three Config_Files: user/mkdocs.yml, developer/mkdocs.yml, and docguide/mkdocs.yml

### Requirement 4: Maintain Template Compatibility

**User Story:** As a documentation theme developer, I want the centralized configuration to work with existing templates, so that no template changes are required.

#### Acceptance Criteria

1. THE System SHALL expose doc_switcher data to templates using the same variable name as before
2. WHEN a template accesses `extra.doc_switcher`, THE System SHALL provide the data from Shared_Config
3. WHEN a template accesses `extra.doc_type`, THE System SHALL provide the value from the local Config_File
4. THE System SHALL maintain backward compatibility with the existing template structure
5. THE System SHALL NOT require changes to HTML template files

### Requirement 5: Validate Configuration Consistency

**User Story:** As a documentation maintainer, I want to ensure the shared configuration is valid, so that build errors are caught early.

#### Acceptance Criteria

1. WHEN the Shared_Config is loaded, THE System SHALL validate that it contains a list structure
2. FOR EACH entry in the doc_switcher list, THE System SHALL validate that required fields (label, url, type) are present
3. IF validation fails, THEN THE System SHALL raise a descriptive error message indicating which field is missing
4. THE System SHALL validate that the type field contains one of the expected values: user, developer, docguide, or swagger
5. THE System SHALL perform validation before the MkDocs build process begins

### Requirement 6: Support Build Process Integration

**User Story:** As a CI/CD pipeline, I want the centralized configuration to work seamlessly with existing build processes, so that no pipeline changes are required.

#### Acceptance Criteria

1. WHEN MkDocs builds documentation, THE System SHALL load the Shared_Config without requiring additional command-line arguments
2. THE System SHALL work with the existing `mkdocs build` command
3. THE System SHALL work with the existing `mkdocs serve` command for local development
4. THE System SHALL NOT require changes to the GitHub Actions workflow files
5. THE System SHALL maintain the same build output structure as before centralization

### Requirement 7: Document Implementation Approach

**User Story:** As a future documentation maintainer, I want clear documentation of how the centralization works, so that I can maintain and troubleshoot the system.

#### Acceptance Criteria

1. THE System SHALL include inline comments in the Shared_Config explaining its purpose
2. THE System SHALL include inline comments in each Config_File explaining how the shared configuration is loaded
3. THE System SHALL document the Macros_Plugin configuration required for loading the Shared_Config
4. THE System SHALL document the file path relationship between Config_Files and Shared_Config
5. THE System SHALL include a comment indicating that doc_type must remain in each individual Config_File
