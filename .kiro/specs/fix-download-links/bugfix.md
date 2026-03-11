# Bugfix Requirements Document

## Introduction

The GeoServer documentation migration from RST/Sphinx to Markdown/MkDocs has resulted in broken download links across approximately 50+ documentation files. The links display incomplete text (e.g., "3.0.0 mbstyle") instead of proper version numbers with full filenames (e.g., "2.28.0 geoserver-2.28.0-mbstyle-plugin.zip"). This affects user experience as the download links are not properly formatted and may not function correctly.

The bug stems from incomplete conversion of RST interpreted text roles (`:download_extension:`, `:nightly_extension:`, `:download_community:`, etc.) to Markdown link syntax with proper macro variable substitution.

## Bug Analysis

### Current Behavior (Defect)

1.1 WHEN a documentation page contains extension download links using `{{ release }} [mbstyle](URL)` format THEN the system renders only the plugin name in the link text instead of the full filename

1.2 WHEN a documentation page contains nightly/snapshot download links using `{{ version }} [mbstyle](URL)` format THEN the system renders only the plugin name in the link text instead of the full filename

1.3 WHEN a documentation page contains community module download links using similar macro patterns THEN the system renders incomplete link text without the full filename

1.4 WHEN a documentation page contains binary download links (war, bin) using similar macro patterns THEN the system renders incomplete link text without the full filename

1.5 WHEN a documentation page contains database connector download links using similar macro patterns THEN the system renders incomplete link text without the full filename

### Expected Behavior (Correct)

2.1 WHEN a documentation page contains extension download links THEN the system SHALL render the link text as "{{ release }} geoserver-{{ release }}-{plugin-name}-plugin.zip" with the version number and full filename visible

2.2 WHEN a documentation page contains nightly/snapshot download links THEN the system SHALL render the link text as "{{ snapshot }} geoserver-{{ snapshot }}-{plugin-name}-plugin.zip" with the snapshot version and full filename visible

2.3 WHEN a documentation page contains community module download links THEN the system SHALL render the link text as "{{ release }} geoserver-{{ release }}-{module-name}-plugin.zip" with the version number and full filename visible

2.4 WHEN a documentation page contains binary download links (war) THEN the system SHALL render the link text as "{{ release }} geoserver-{{ release }}.war" with the version number and full filename visible

2.5 WHEN a documentation page contains binary download links (bin) THEN the system SHALL render the link text as "{{ release }} geoserver-{{ release }}-bin.zip" with the version number and full filename visible

### Unchanged Behavior (Regression Prevention)

3.1 WHEN a documentation page contains regular Markdown links (non-download links) THEN the system SHALL CONTINUE TO render them with their existing link text format

3.2 WHEN a documentation page contains internal documentation links THEN the system SHALL CONTINUE TO render them correctly without modification

3.3 WHEN a documentation page contains external reference links (non-download) THEN the system SHALL CONTINUE TO render them correctly without modification

3.4 WHEN the macro system processes {{ release }}, {{ version }}, or {{ snapshot }} variables in non-link contexts THEN the system SHALL CONTINUE TO substitute them correctly

3.5 WHEN a documentation page contains code blocks with download examples THEN the system SHALL CONTINUE TO render them as code without link processing
