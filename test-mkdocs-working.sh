#!/bin/bash

# Working MkDocs testing script with proper GeoServer structure understanding
# Usage: ./test-mkdocs-working.sh [step]
# Steps: setup, build-sphinx, convert, build-mkdocs, serve, all

set -e

STEP=${1:-all}
VENV_DIR="venv"

echo "Testing MkDocs workflow (working version)..."
echo "Step: $STEP"
echo "OS Type: $OSTYPE"
echo "Working Directory: $(pwd)"

# Cross-platform virtual environment activation
activate_venv() {
    if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "win32" ]]; then
        source $VENV_DIR/Scripts/activate
    else
        source $VENV_DIR/bin/activate
    fi
}

# Debug function
debug_info() {
    echo "Debug Info:"
    echo "   - Python3: $(command -v python3 || echo 'not found')"
    echo "   - Python: $(command -v python || echo 'not found')"
    echo "   - Pandoc: $(command -v pandoc || echo 'not found')"
    echo "   - Maven: $(command -v mvn || echo 'not found')"
    echo "   - Virtual env exists: $([ -d "$VENV_DIR" ] && echo 'yes' || echo 'no')"
    
    if [ -d "$VENV_DIR" ]; then
        if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "win32" ]]; then
            echo "   - Activation script: $([ -f "$VENV_DIR/Scripts/activate" ] && echo 'exists' || echo 'missing')"
        else
            echo "   - Activation script: $([ -f "$VENV_DIR/bin/activate" ] && echo 'exists' || echo 'missing')"
        fi
    fi
    
    echo "   - GeoServer doc structure:"
    if [ -d "doc/en" ]; then
        echo "     - doc/en exists: $(ls doc/en/ | tr '\n' ' ')"
        for doc_type in user developer docguide; do
            if [ -d "doc/en/$doc_type" ]; then
                echo "     - doc/en/$doc_type/source exists: $([ -d "doc/en/$doc_type/source" ] && echo 'yes' || echo 'no')"
                if [ -d "doc/en/$doc_type/source" ]; then
                    rst_count=$(find "doc/en/$doc_type/source" -name "*.rst" -type f | wc -l)
                    echo "     - RST files in $doc_type: $rst_count"
                fi
            fi
        done
    fi
}

setup_environment() {
    echo "Setting up environment..."
    
    # Check if pandoc is installed
    if ! command -v pandoc &> /dev/null; then
        echo "ERROR: pandoc is required but not installed."
        echo "   Install with:"
        echo "   - Linux: sudo apt-get install pandoc"
        echo "   - macOS: brew install pandoc"
        echo "   - Windows: winget install JohnMacFarlane.Pandoc"
        exit 1
    fi
    
    # Check if Python is available (try python3 first, then python)
    if command -v python3 &> /dev/null; then
        PYTHON_CMD="python3"
    elif command -v python &> /dev/null; then
        PYTHON_CMD="python"
    else
        echo "ERROR: python is required but not installed."
        exit 1
    fi
    
    echo "OK: Using Python: $PYTHON_CMD"
    
    # Create virtual environment
    if [ ! -d "$VENV_DIR" ]; then
        echo "Creating Python virtual environment..."
        $PYTHON_CMD -m venv $VENV_DIR
    fi
    
    # Activate virtual environment
    activate_venv
    
    # dummy config.yml
    echo "Creating a dummy config.yml..."
    PACKAGE_PATH=$(python -c "import mkdocs_translate; import os; print(os.path.dirname(mkdocs_translate.__file__))")
    mkdir -p "$PACKAGE_PATH"
    cat > "$PACKAGE_PATH/config.yml" <<'EOL'
# Minimal configuration for mkdocs_translate
project_folder: "."
rst_folder: "source"
docs_folder: "docs"
build_folder: "target"
anchor_file: "anchors.txt"
convert_folder: "convert"
upload_folder: "upload"
download_folder: "download"
deepl_base_url: "https://api-free.deepl.com"
substitutions: {}
extlinks: {}
EOL

    # Install mkdocs-translate (directory version)
    echo "Installing mkdocs-translate (directory version)..."
    pip install git+https://github.com/petersmythe/translate.git
    
    # Install MkDocs
    echo "Installing MkDocs, theme, and plugins..."
    pip install mkdocs mkdocs-material mkdocs-minify-plugin
    
    echo "OK: Environment setup complete!"
}

build_sphinx() {
    echo "Building Sphinx documentation..."
    
    activate_venv
    
    # Check if Maven is available
    if ! command -v mvn &> /dev/null; then
        echo "WARNING: Maven not found. Skipping Sphinx build..."
        echo "   To install Maven:"
        echo "   - Linux: sudo apt-get install maven"
        echo "   - macOS: brew install maven"
        echo "   - Windows: winget install Apache.Maven"
        return 0
    fi
    
    # Build English docs
    if [ -d "doc/en" ]; then
        echo "Building English documentation..."
        cd doc/en
        if [ -f "requirements.txt" ]; then
            pip install -r requirements.txt
        fi
        mvn -B -ntp compile || echo "WARNING: Maven build failed, continuing..."
        cd ../..
    fi
    
    # Build Chinese docs
    if [ -d "doc/zhCN" ]; then
        echo "Building Chinese documentation..."
        cd doc/zhCN
        if [ -f "requirements.txt" ]; then
            pip install -r requirements.txt
        elif [ -f "../en/requirements.txt" ]; then
            pip install -r ../en/requirements.txt
        fi
        mvn -B -ntp compile || echo "WARNING: Maven build failed, continuing..."
        cd ../..
    fi
    
    echo "OK: Sphinx build complete!"
}

convert_to_mkdocs() {
    echo "Converting RST to Markdown using mkdocs_translate..."
    
    activate_venv
    
    # Check prerequisites
    echo "Checking prerequisites..."
    
    # Check pandoc
    if ! command -v pandoc &> /dev/null; then
        echo "ERROR: pandoc is required but not installed."
        echo "   Install with:"
        echo "   - Linux: sudo apt-get install pandoc"
        echo "   - macOS: brew install pandoc"
        echo "   - Windows: winget install JohnMacFarlane.Pandoc"
        return 1
    fi
    echo "OK: pandoc found"
    
    # Verify mkdocs_translate is working
    echo "Testing mkdocs_translate..."
    if ! mkdocs_translate --help > /dev/null 2>&1; then
        echo "ERROR: mkdocs_translate is not working. Please check the installation."
        return 1
    fi
    echo "OK: mkdocs_translate is working"
    
    for lang in en zhCN; do
        if [ -d "doc/$lang" ]; then
            echo "Processing $lang documentation..."
            
            for doc_type in user developer docguide; do
                echo ""
                echo "Converting $lang $doc_type documentation..."
                
                # Create working directory
                work_dir="doc/$lang/mkdocs_$doc_type"
                mkdir -p "$work_dir"
                cd "$work_dir"
                
                # The GeoServer structure has: doc/en/user/source/, doc/en/developer/source/, etc.
                source_path="../$doc_type/source"
                
                if [ -d "$source_path" ]; then
                    echo "   Found source directory: $source_path"
                    
                    # Count RST files
                    rst_count=$(find "$source_path" -name "*.rst" -type f | wc -l)
                    echo "   Found $rst_count RST files"
                    
                    if [ $rst_count -gt 0 ]; then
                        echo "   Initializing mkdocs structure..."
                        rst_folder="$(pwd)/$source_path"
                        docs_folder="$(pwd)/docs"
                        
                        # Step 1: Initialize docs directory (copy assets)
                        echo "   Step 1: Initializing docs directory..."
                        if mkdocs_translate init "$source_path"; then
                            echo "   OK: Init successful"
                        else
                            echo "   WARNING: Init failed, continuing anyway..."
                        fi
                        
                        # Step 2: Scan RST files for anchors and downloads
                        echo "   Step 2: Scanning RST files..."
                        mkdocs_translate scan || echo "   WARNING: Scan failed, continuing..."
                        
                        # Step 3: Generate navigation structure  
                        echo "   Step 3: Generating navigation..."
                        mkdocs_translate nav || echo "   WARNING: Nav generation failed, continuing..."
                        
                        # Step 4: Migrate RST files to Markdown
                        echo "   Step 4: Migrating RST files..."
                        if mkdocs_translate migrate; then
                            echo "   OK: Migration successful for $lang/$doc_type"
                            
                            # Count generated markdown files
                            md_count=$(find "$docs_folder" -name "*.md" -type f 2>/dev/null | wc -l)
                            echo "   Generated $md_count Markdown files"
                        else
                            echo "   WARNING: Migration failed for $lang/$doc_type, creating fallback"
                            mkdir -p docs
                            cat > docs/index.md <<EOF
# $doc_type Documentation ($lang)

This documentation is being converted from reStructuredText to Markdown.

## Conversion Status
- Source RST files found: $rst_count
- Migration status: Failed
- Fallback content created

## Original Location
- Source: \`$source_path\`
EOF
                        fi
                    else
                        echo "   WARNING: No RST files found in $source_path"
                        mkdir -p docs
                        echo "# $doc_type Documentation ($lang)" > docs/index.md
                        echo "" >> docs/index.md
                        echo "No RST source files found for conversion." >> docs/index.md
                    fi
                else
                    echo "   ERROR: Source directory not found: $source_path"
                    echo "   Available in doc/$lang/:"
                    ls -la ../  2>/dev/null || echo "   Cannot list directory"
                    
                    mkdir -p docs
                    echo "# $doc_type Documentation ($lang)" > docs/index.md
                    echo "" >> docs/index.md
                    echo "Expected source directory not found: $source_path" >> docs/index.md
                fi
                
                cd ../../..
            done
        fi
    done
    
    echo "Conversion process complete!"
}

build_mkdocs() {
    echo "Building MkDocs sites..."
    
    activate_venv
    
    # Create output directory
    mkdir -p gh-pages-output
    
    # Build each documentation set
    for lang in en zhCN; do
        for doc_type in user developer docguide; do
            work_dir="doc/$lang/mkdocs_$doc_type"
            if [ -d "$work_dir" ]; then
                echo "Building $lang $doc_type..."
                cd "$work_dir"
                
                # Ensure docs directory exists
                if [ ! -d "docs" ]; then
                    mkdir -p docs
                    echo "# GeoServer $doc_type Documentation ($lang)" > docs/index.md
                    echo "" >> docs/index.md
                    echo "Documentation build in progress." >> docs/index.md
                fi
                
                # Create mkdocs.yml if it doesn't exist
                if [ ! -f "mkdocs.yml" ]; then
                    lang_name="English"
                    nav_home="Home"
                    mkdocs_lang="en"
                    if [ "$lang" = "zhCN" ]; then
                        lang_name="中文"
                        nav_home="首页"
                        mkdocs_lang="zh"  # MkDocs Material uses 'zh' not 'zhCN'
                    fi
                    
                    cat > mkdocs.yml <<EOF
site_name: GeoServer $doc_type Documentation ($lang_name)
site_description: GeoServer $doc_type documentation
site_author: GeoServer Project

theme:
  name: material
  language: $mkdocs_lang
  features:
    - navigation.tabs
    - navigation.sections
    - navigation.expand
    - navigation.top
    - search.highlight
    - search.share

nav:
  - $nav_home: index.md

markdown_extensions:
  - admonition
  - pymdownx.details
  - pymdownx.superfences
  - tables
  - toc:
      permalink: true

plugins:
  - search

site_dir: ../../../gh-pages-output/$lang/$doc_type
EOF
                fi
                
                # Build the site
                echo "   Building with MkDocs..."
                if mkdocs build --clean; then
                    echo "   Successfully built $lang/$doc_type"
                else
                    echo "   MkDocs build failed for $lang/$doc_type, creating fallback"
                    # Create fallback HTML
                    mkdir -p "../../../gh-pages-output/$lang/$doc_type"
                    cat > "../../../gh-pages-output/$lang/$doc_type/index.html" <<EOF
<!DOCTYPE html>
<html lang="$lang">
<head>
    <meta charset="UTF-8">
    <title>GeoServer $doc_type Documentation</title>
    <style>body{font-family:Arial,sans-serif;max-width:800px;margin:50px auto;padding:20px;}</style>
</head>
<body>
    <h1>GeoServer $doc_type Documentation</h1>
    <p><em>Build failed - fallback content</em></p>
    <p><strong>Language:</strong> $lang</p>
</body>
</html>
EOF
                fi
                
                cd ../../..
            fi
        done
    done
    
    # Create main index page
    echo "Creating main documentation portal..."
    cat > gh-pages-output/index.html <<'EOF'
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>GeoServer Documentation Portal</title>
    <style>
        body { font-family: Arial, sans-serif; max-width: 1000px; margin: 0 auto; padding: 40px 20px; }
        h1 { color: #2c3e50; text-align: center; }
        .lang-section { margin: 40px 0; }
        .lang-title { color: #34495e; font-size: 1.5em; margin-bottom: 20px; }
        .doc-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 20px; }
        .doc-link { display: block; padding: 20px; background: #f8f9fa; border: 1px solid #dee2e6; border-radius: 5px; text-decoration: none; color: #495057; transition: background 0.2s; }
        .doc-link:hover { background: #e9ecef; }
        .doc-title { font-weight: bold; margin-bottom: 8px; }
        .doc-desc { font-size: 0.9em; color: #6c757d; }
    </style>
</head>
<body>
    <h1>GeoServer Documentation Portal</h1>
    
    <div class="lang-section">
        <h2 class="lang-title">English Documentation</h2>
        <div class="doc-grid">
            <a href="en/user/" class="doc-link">
                <div class="doc-title">User Guide</div>
                <div class="doc-desc">Complete guide for GeoServer users and administrators</div>
            </a>
            <a href="en/developer/" class="doc-link">
                <div class="doc-title">Developer Guide</div>
                <div class="doc-desc">Technical documentation for GeoServer developers</div>
            </a>
            <a href="en/docguide/" class="doc-link">
                <div class="doc-title">Documentation Guide</div>
                <div class="doc-desc">Guidelines for editing GeoServer documentation</div>
            </a>
        </div>
    </div>
    
    <div class="lang-section">
        <h2 class="lang-title">中文文档</h2>
        <div class="doc-grid">
            <a href="zhCN/user/" class="doc-link">
                <div class="doc-title">用户指南</div>
                <div class="doc-desc">GeoServer 用户和管理员完整指南</div>
            </a>
            <a href="zhCN/developer/" class="doc-link">
                <div class="doc-title">开发者指南</div>
                <div class="doc-desc">GeoServer 开发人员技术文档</div>
            </a>
            <a href="zhCN/docguide/" class="doc-link">
                <div class="doc-title">文档指南</div>
                <div class="doc-desc">编辑 GeoServer 文档的指南</div>
            </a>
        </div>
    </div>
</body>
</html>
EOF
    
    echo "MkDocs build complete!"
    echo "Output directory: gh-pages-output/"
}

serve_local() {
    echo "Starting local server..."
    
    if [ ! -d "gh-pages-output" ]; then
        echo "ERROR: No build output found. Run with 'all' or 'build-mkdocs' first."
        exit 1
    fi
    
    # Determine Python command
    if command -v python3 &> /dev/null; then
        PYTHON_CMD="python3"
    elif command -v python &> /dev/null; then
        PYTHON_CMD="python"
    else
        echo "ERROR: python is required but not installed."
        exit 1
    fi
    
    cd gh-pages-output
    echo "Server starting at: http://localhost:8000"
    echo "Press Ctrl+C to stop"
    $PYTHON_CMD -m http.server 8000
}

# Main execution
case $STEP in
    "setup")
        setup_environment
        ;;
    "build-sphinx")
        setup_environment
        build_sphinx
        ;;
    "convert")
        setup_environment
        convert_to_mkdocs
        ;;
    "build-mkdocs")
        setup_environment
        build_mkdocs
        ;;
    "serve")
        serve_local
        ;;
    "debug")
        debug_info
        ;;
    "all")
        debug_info
        setup_environment
        build_sphinx
        convert_to_mkdocs
        build_mkdocs
        echo ""
        echo "All steps complete!"
        echo "Output directory: gh-pages-output/"
        echo "To serve locally: ./test-mkdocs-working.sh serve"
        ;;
    *)
        echo "ERROR: Unknown step: $STEP"
        echo "Available steps: setup, build-sphinx, convert, build-mkdocs, serve, debug, all"
        exit 1
        ;;
esac