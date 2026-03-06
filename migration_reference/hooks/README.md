# MkDocs Download Files Hook

This directory contains MkDocs hooks for handling download files in the GeoServer documentation.

## Overview

The `download_files.py` hook automatically scans Markdown files for download links and copies the referenced files from the docs directory to the MkDocs output directory. This ensures that download links work correctly in the built documentation.

## How It Works

### 1. Scanning Phase (on_pre_build)

Before the build starts, the hook:
- Scans all Markdown files in the docs directory
- Identifies download links using regex pattern matching
- Looks for common download file extensions: `.zip`, `.xml`, `.properties`, `.sld`, `.json`, `.csv`, `.yaml`, `.yml`, `.txt`, `.sql`, `.sh`, `.bat`, `.jar`
- Skips external URLs (http://, https://, ftp://)
- Skips anchor links (#)
- Stores the list of download files for the copy phase

### 2. Copy Phase (on_post_build)

After the build completes, the hook:
- Copies each referenced download file from the docs directory to the site output directory
- Preserves the directory structure (files are copied to the same relative path)
- Creates destination directories as needed
- Logs warnings for missing files
- Reports the number of files successfully copied

## Supported Link Formats

The hook recognizes standard Markdown link syntax:

```markdown
[Download the file](filename.zip)
[Configuration example](config.xml)
[Style file](style.sld)
```

Relative paths are supported:

```markdown
[Download](../data/example.zip)
[Config](../../config/settings.properties)
```

## File Extensions Detected

The hook automatically detects links to files with these extensions:

- Archives: `.zip`, `.jar`
- Configuration: `.xml`, `.properties`, `.yaml`, `.yml`, `.json`
- Styles: `.sld`
- Data: `.csv`, `.txt`, `.sql`
- Scripts: `.sh`, `.bat`

## Configuration

The hook is configured in `mkdocs.yml`:

```yaml
hooks:
  - hooks/download_files.py
```

No additional configuration is required. The hook automatically:
- Uses the `docs_dir` setting from mkdocs.yml (default: `docs`)
- Uses the `site_dir` setting from mkdocs.yml (default: `site`)

## Testing

To test the hook without building the full documentation:

```bash
# Run the test script
python test_download_hook.py
```

The test script will:
1. Scan for download links
2. Report the number of links found
3. Test copying files to a temporary directory
4. Show sample results

## Logging

The hook uses Python's logging module with the logger name `mkdocs.plugins.download_files`.

Log levels:
- **INFO**: Summary information (number of files found/copied)
- **DEBUG**: Detailed information (each file copied)
- **WARNING**: Missing files or scan errors
- **ERROR**: Copy failures

To see detailed logs, run mkdocs with the `--verbose` flag:

```bash
mkdocs build --verbose
```

## Troubleshooting

### Download link not working

1. Check that the file exists in the docs directory at the expected path
2. Verify the link uses relative path syntax
3. Check the build logs for "Download file not found" warnings
4. Ensure the file extension is in the supported list

### File not copied

1. Check file permissions (must be readable)
2. Verify the destination directory is writable
3. Look for error messages in the build log

### Hook not running

1. Verify `hooks/download_files.py` is listed in mkdocs.yml
2. Check that the hooks directory is in the same directory as mkdocs.yml
3. Ensure Python can import the hook (check for syntax errors)

## Implementation Details

### scan_download_links(docs_dir)

Scans Markdown files for download references.

**Parameters:**
- `docs_dir`: Path to the MkDocs docs directory

**Returns:**
- Set of tuples `(markdown_file_path, download_file_path)`

**Algorithm:**
1. Walk through all `.md` files recursively
2. Read each file and search for link patterns
3. Extract link text and target path
4. Filter out external URLs and anchors
5. Store the relationship between Markdown file and download file

### copy_download_files(docs_dir, site_dir, download_links)

Copies download files from docs to site output.

**Parameters:**
- `docs_dir`: Path to the MkDocs docs directory
- `site_dir`: Path to the MkDocs site output directory
- `download_links`: Set of `(markdown_file, download_file)` tuples

**Returns:**
- Number of files successfully copied

**Algorithm:**
1. For each download link:
   - Resolve the source file path (relative to the Markdown file)
   - Determine the destination path (same relative structure in site_dir)
   - Create destination directory if needed
   - Copy the file with metadata preserved (shutil.copy2)
   - Log success or failure

## Future Enhancements

Possible improvements for future versions:

1. **Configurable file extensions**: Allow customizing the list of detected extensions
2. **Download directory mapping**: Support mapping source directories to different output locations
3. **File validation**: Check file sizes, checksums, or content types
4. **Broken link report**: Generate a report of all missing download files
5. **External file fetching**: Optionally download files from external URLs during build

## License

This hook is part of the GeoServer documentation project and follows the same license as GeoServer.
