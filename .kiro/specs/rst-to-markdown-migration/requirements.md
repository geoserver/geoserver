# Requirements Document

## Introduction

This document specifies requirements for executing the one-time migration of GeoServer documentation from reStructuredText (RST) with Sphinx to Markdown with MkDocs. The migration targets GeoServer 3.0 and 2.28.x branches. The conversion tooling (petersmythe/translate and .github/workflows/mkdocs.yml) already exists; this project focuses on executing the migration, validating results, committing converted files, and managing the transition.

## Timeline

- **Next 2 weeks**: One-time conversion from RST to MD using existing tooling (executed per branch: 3.0 and 2.28.x)
- **Mid-March**: Separate work on GS3 PR begins; AfriGIS QA resources start on new UI (not MD-related)
- **Screenshot QA - Phase 1 (Initial)**: After GS3 UI work begins, identify screenshots needing updates
- **Screenshot QA - Phase 2 (Final)**: After GS3 UI completion, new screenshots added to MD docs
- **15 April**: GeoServer 3.0 release date

## Glossary

- **Migration_Project**: This project - executing the one-time conversion and managing the transition
- **Translation_Tool**: The petersmythe/translate tool (already exists) for RST to Markdown conversion
- **Conversion_Workflow**: Existing .github/workflows/mkdocs.yml that performs conversion
- **Migration_Branch**: Git branch where conversion is executed before merging
- **Source_MD**: Markdown files committed to repository after conversion
- **Build_Workflow**: Post-migration GitHub Actions workflow that builds from Source_MD
- **User_Manual**: Documentation in doc/en/user/ for end users
- **Developer_Manual**: Documentation in doc/en/developer/ for contributors
- **Documentation_Guide**: Documentation in doc/en/docguide/ about writing docs

## Requirements

### Requirement 1: Execute RST to Markdown Conversion

**User Story:** As a migration manager, I want to execute the conversion on target branches, so that RST files are converted to Markdown using existing tooling.

#### Acceptance Criteria

1. THE Migration_Project SHALL create migration branches from 3.0 and 2.28.x
2. THE Migration_Project SHALL execute Translation_Tool on all RST files in User_Manual, Developer_Manual, and Documentation_Guide
3. THE Migration_Project SHALL use existing Conversion_Workflow steps (init, scan, migrate, nav)
4. WHEN conversion is executed, THE Migration_Project SHALL produce Markdown files with UTF-8 encoding
5. THE Migration_Project SHALL convert 3.0 branch first, then 2.28.x branch
6. THE Migration_Project SHALL maintain the original directory structure in converted files
7. THE Migration_Project SHALL complete conversion within 2 weeks

### Requirement 2: Commit Converted Markdown Files

**User Story:** As a migration manager, I want to commit converted Markdown files to the repository, so that they become the source documentation.

#### Acceptance Criteria

1. THE Migration_Project SHALL commit all converted Markdown files to Migration_Branch
2. THE Migration_Project SHALL commit updated mkdocs.yml files with generated navigation
3. THE Migration_Project SHALL create PR for each branch: "Migrate [branch] documentation from RST to Markdown"
4. THE Migration_Project SHALL include conversion summary in PR description
5. THE Migration_Project SHALL NOT commit temporary conversion files (target/, convert/)

### Requirement 3: Update GitHub Actions Workflow

**User Story:** As a migration manager, I want to update the GitHub Actions workflow to build from source Markdown, so that the conversion step is removed and builds use committed MD files.

#### Acceptance Criteria

1. THE Migration_Project SHALL modify .github/workflows/mkdocs.yml
2. THE Migration_Project SHALL remove "Install pandoc" step from workflow
3. THE Migration_Project SHALL remove "Install mkdocs-translate" step from workflow
4. THE Migration_Project SHALL remove "Convert RST to Markdown" step from workflow
5. THE Migration_Project SHALL keep "Build all MkDocs sites" step (builds from Source_MD)
6. THE Migration_Project SHALL keep "Deploy to GitHub Pages" step
7. THE Migration_Project SHALL update workflow trigger branches to [3.0, 2.28.x]
8. THE Migration_Project SHALL commit workflow changes in the same PR

### Requirement 4: Update mkdocs.yml Configuration

**User Story:** As a migration manager, I want to update mkdocs.yml files with proper configuration, so that builds work correctly and features are enabled.

#### Acceptance Criteria

1. THE Migration_Project SHALL merge generated navigation from nav_generated.yml into mkdocs.yml
2. THE Migration_Project SHALL configure mkdocs-macros-plugin for variable substitution
3. THE Migration_Project SHALL configure pymdownx.tabbed extension for conditional content
4. THE Migration_Project SHALL configure pymdownx.superfences for code blocks
5. THE Migration_Project SHALL configure admonition extension for notes/warnings
6. THE Migration_Project SHALL configure theme branding (logo, colors)
7. THE Migration_Project SHALL define version and release variables in extra section

### Requirement 5: Validate Conversion Quality with Side-by-Side Comparison

**User Story:** As a migration manager, I want to validate conversion quality by comparing rendered HTML pages side-by-side, so that I can identify and fix major rendering issues.

#### Acceptance Criteria

1. THE Migration_Project SHALL build HTML from original RST using Sphinx
2. THE Migration_Project SHALL build HTML from converted Markdown using MkDocs
3. THE Migration_Project SHALL compare each rendered HTML page side-by-side
4. THE Migration_Project SHALL identify major rendering problems (tables not rendering as tables, missing content sections, broken formatting)
5. THE Migration_Project SHALL manually review directive conversions (guilabel, menuselection, file)
6. THE Migration_Project SHALL manually review admonition formatting (note, warning, tip)
7. THE Migration_Project SHALL verify navigation structure is correct
8. THE Migration_Project SHALL fix all critical rendering issues before creating PR
9. THE Migration_Project SHALL document known minor issues in PR description

### Requirement 6: Validate Link Integrity

**User Story:** As a migration manager, I want to validate all links work, so that users can navigate documentation.

#### Acceptance Criteria

1. THE Migration_Project SHALL create link validation script
2. THE Migration_Project SHALL validate all internal links resolve
3. THE Migration_Project SHALL validate all anchor links exist
4. THE Migration_Project SHALL validate all image references exist
5. THE Migration_Project SHALL report broken links with file and line number
6. THE Migration_Project SHALL fix all broken internal links before merging
7. THE Migration_Project SHALL document any broken external links

### Requirement 7: Validate Build Success

**User Story:** As a migration manager, I want to validate that builds succeed, so that I can confirm the migration is complete.

#### Acceptance Criteria

1. THE Migration_Project SHALL trigger GitHub Actions workflow on Migration_Branch
2. THE Migration_Project SHALL verify HTML builds successfully for all three documentation types
3. THE Migration_Project SHALL verify no build errors or warnings
4. THE Migration_Project SHALL verify GitHub Pages preview is accessible
5. THE Migration_Project SHALL test documentation on desktop and mobile browsers
6. THE Migration_Project SHALL verify search functionality works
7. THE Migration_Project SHALL verify navigation structure is correct

### Requirement 8: Document Jenkins Build Process

**User Story:** As a migration manager, I want to document the Jenkins build process, so that I can ensure GitHub Actions provides equivalent functionality.

#### Acceptance Criteria

1. THE Migration_Project SHALL access Jenkins build logs for documentation builds
2. THE Migration_Project SHALL document all Jenkins build steps
3. THE Migration_Project SHALL identify deployment targets used by Jenkins
4. THE Migration_Project SHALL compare Jenkins steps with GitHub Actions workflow
5. THE Migration_Project SHALL document any gaps or differences
6. THE Migration_Project SHALL update GitHub Actions workflow if critical functionality is missing
7. THE Migration_Project SHALL create Jenkins analysis document

### Requirement 9: Configure OSGeo Server Deployment

**User Story:** As a migration manager, I want to configure deployment to docs.geoserver.org, so that documentation is published to the production server.

#### Acceptance Criteria

1. THE Migration_Project SHALL add SSH credentials to GitHub Secrets (GEOSERVER_DOCS_SSH_KEY)
2. THE Migration_Project SHALL configure deployment to geoserverdocs@geo-docs.geoserver.org:2223
3. THE Migration_Project SHALL replicate Jenkins deployment process (ZIP transfer + unzip)
4. THE Migration_Project SHALL configure remote path: /var/www/geoserverdocs/$VER
5. THE Migration_Project SHALL configure "latest" symlink for 3.0 branch
6. THE Migration_Project SHALL test deployment on migration branch before production
7. THE Migration_Project SHALL verify URL structure matches existing docs

### Requirement 10: Update Documentation Guide for Markdown

**User Story:** As a documentation author, I want updated documentation guidelines, so that I know how to write documentation using Markdown.

#### Acceptance Criteria

1. THE Migration_Project SHALL update doc/en/docguide/ with Markdown syntax guide
2. THE Migration_Project SHALL document Markdown equivalents for RST directives
3. THE Migration_Project SHALL provide examples of guilabel, menuselection, file markup
4. THE Migration_Project SHALL document variable substitution using mkdocs-macros-plugin
5. THE Migration_Project SHALL document conditional content using pymdownx.tabbed
6. THE Migration_Project SHALL update doc/en/README.md to remove Sphinx instructions
7. THE Migration_Project SHALL document local development workflow (mkdocs serve)

### Requirement 11: Update Steering Documentation

**User Story:** As a developer, I want updated steering documentation, so that I understand the new Markdown-based documentation process.

#### Acceptance Criteria

1. THE Migration_Project SHALL update .kiro/steering/ documentation if it exists
2. THE Migration_Project SHALL document the migration from RST to Markdown
3. THE Migration_Project SHALL document the new MkDocs build process
4. THE Migration_Project SHALL document how to contribute to Markdown documentation
5. THE Migration_Project SHALL document the GitHub Actions workflow
6. THE Migration_Project SHALL remove references to Sphinx from steering docs

### Requirement 12: Coordinate Screenshot Updates

**User Story:** As a migration manager, I want to coordinate screenshot updates with the QA team, so that all screenshots are updated for GeoServer 3.0 UI.

#### Acceptance Criteria

1. THE Migration_Project SHALL brief AfriGIS team on screenshot update process
2. THE Migration_Project SHALL document how to replace screenshots in Markdown
3. THE Migration_Project SHALL create tracking spreadsheet for screenshot updates
4. THE Migration_Project SHALL assume ALL screenshots will need updating for GS3 UI
5. Phase 1 SHALL begin after mid-March when GS3 UI work begins
6. Phase 2 SHALL complete before 15 April GeoServer 3.0 release
7. THE Migration_Project SHALL provide clear instructions for screenshot file paths and naming

### Requirement 13: Test Local Development Workflow

**User Story:** As a documentation author, I want to test the local development workflow, so that I can confirm it works for future contributors.

#### Acceptance Criteria

1. THE Migration_Project SHALL test `mkdocs serve` command for live preview
2. THE Migration_Project SHALL verify browser auto-reload on file changes
3. THE Migration_Project SHALL test `mkdocs build` command for local builds
4. THE Migration_Project SHALL verify build time is faster than Sphinx
5. THE Migration_Project SHALL document local development setup in README
6. THE Migration_Project SHALL test on macOS, Linux, and Windows if possible

### Requirement 14: Configure Theme and Branding

**User Story:** As a documentation user, I want consistent GeoServer branding, so that documentation feels like part of the project.

#### Acceptance Criteria

1. THE Migration_Project SHALL configure mkdocs-material theme
2. THE Migration_Project SHALL add GeoServer logo to theme
3. THE Migration_Project SHALL configure theme colors (blue primary, light-blue accent)
4. THE Migration_Project SHALL enable dark mode support
5. THE Migration_Project SHALL configure social links (GitHub, geoserver.org)
6. THE Migration_Project SHALL enable navigation features (tabs, sections, expand, top)
7. THE Migration_Project SHALL test responsive design on mobile

### Requirement 15: Handle Chinese Translation

**User Story:** As a migration manager, I want to convert Chinese documentation, so that Chinese-speaking users have Markdown documentation.

#### Acceptance Criteria

1. THE Migration_Project SHALL convert all RST files in doc/zhCN/ to Markdown
2. THE Migration_Project SHALL verify UTF-8 encoding is preserved for Chinese characters
3. THE Migration_Project SHALL update doc/zhCN/mkdocs.yml configuration
4. THE Migration_Project SHALL build Chinese documentation to verify conversion
5. THE Migration_Project SHALL include Chinese conversion in the same PR as English
6. THE Migration_Project SHALL test Chinese search functionality

### Requirement 16: Implement Version Selector

**User Story:** As a documentation user, I want to switch between documentation versions, so that I can view docs for my GeoServer version.

#### Acceptance Criteria

1. THE Migration_Project SHALL configure version selector in mkdocs.yml
2. THE Migration_Project SHALL list available versions: 3.0, 2.28.x, latest
3. THE Migration_Project SHALL configure version URLs
4. THE Migration_Project SHALL test version switching functionality
5. THE Migration_Project SHALL document version selector configuration

### Requirement 17: Enable PDF Generation

**User Story:** As a documentation user, I want PDF versions of documentation, so that I can read offline.

#### Acceptance Criteria

1. THE Migration_Project SHALL configure mkdocs-with-pdf plugin in mkdocs.yml
2. THE Migration_Project SHALL generate PDF for User_Manual
3. THE Migration_Project SHALL generate PDF for Developer_Manual
4. THE Migration_Project SHALL generate PDF for Documentation_Guide
5. THE Migration_Project SHALL verify PDF formatting is acceptable
6. THE Migration_Project SHALL configure PDF metadata (title, author, copyright)

### Requirement 18: Handle Download Files

**User Story:** As a documentation user, I want downloadable files from the codebase, so that I can access configuration examples.

#### Acceptance Criteria

1. THE Migration_Project SHALL identify download directives in converted Markdown
2. THE Migration_Project SHALL verify download links point to correct files
3. THE Migration_Project SHALL document download file handling for future authors
4. IF download files are missing, THE Migration_Project SHALL create build hook to copy them
5. THE Migration_Project SHALL test download links work in built documentation

### Requirement 19: Create Migration Summary Report

**User Story:** As a migration manager, I want to create a migration summary report, so that stakeholders understand what was accomplished.

#### Acceptance Criteria

1. THE Migration_Project SHALL count total RST files converted
2. THE Migration_Project SHALL count total lines of documentation converted
3. THE Migration_Project SHALL list any unconverted directives or issues
4. THE Migration_Project SHALL document conversion time per branch
5. THE Migration_Project SHALL list files removed (RST sources, Sphinx config)
6. THE Migration_Project SHALL list files added (Markdown docs, updated workflows)
7. THE Migration_Project SHALL include summary in PR description

### Requirement 20: Remove RST Infrastructure

**User Story:** As a migration manager, I want to remove RST source files and Sphinx infrastructure, so that the repository only contains Markdown documentation.

#### Acceptance Criteria

1. THE Migration_Project SHALL remove doc/en/*/source/ directories (RST files)
2. THE Migration_Project SHALL remove doc/en/requirements.txt (Sphinx dependencies)
3. THE Migration_Project SHALL remove doc/en/build.xml (Ant build script)
4. THE Migration_Project SHALL update doc/en/pom.xml to remove Sphinx build profiles
5. THE Migration_Project SHALL remove .github/workflows/docs.yml (Sphinx workflow)
6. THE Migration_Project SHALL commit all removals in the same PR as converted files
7. THE Migration_Project SHALL perform removal AFTER successful validation and build verification
