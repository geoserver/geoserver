#!/bin/bash

# Test mkdocs workflow - Restructured version using directory-based approach

set -e  # Exit on any error

echo "Testing MkDocs workflow (restructured version)..."

# Function to show usage
show_help() {
    echo "Usage: $0 [options] [step]"
    echo ""
    echo "Steps:"
    echo "  setup    - Set up Python environment and install dependencies"
    echo "  sphinx   - Build Sphinx documentation"
    echo "  convert  - Convert RST to Markdown using mkdocs_translate"
    echo "  mkdocs   - Build MkDocs sites"
    echo "  all      - Run all steps (default)"
    echo ""
    echo "Options:"
    echo "  -h, --help    Show this help message"
}

# Parse arguments
STEP="all"
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            exit 0
            ;;
        setup|sphinx|convert|mkdocs|all)
            STEP="$1"
            shift
            ;;
        *)
            echo "Unknown argument: $1"
            show_help
            exit 1
            ;;
    esac
done

echo "Step: $STEP"

# Get OS type for cross-platform compatibility
OS_TYPE=""
if [[ "$OSTYPE" == "msys" ]]; then
    OS_TYPE="msys"
elif [[ "$OSTYPE" == "cygwin" ]]; then
    OS_TYPE="cygwin"
else
    OS_TYPE="unix"
fi

echo "OS Type: $OS_TYPE"
echo "Working Directory: $(pwd)"

# Debug information
echo "Debug Info:"

# Setup environment
setup_environment() {
    echo "Setting up environment..."
    
    # Create virtual environment if it doesn't exist
    if [ ! -d "venv" ]; then
        python3 -m venv venv
    fi
    
    # Activate virtual environment (cross-platform)
    if [[ "$OS_TYPE" == "msys" || "$OS_TYPE" == "cygwin" ]]; then
        source venv/Scripts/activate
    else
        source venv/bin/activate
    fi
    
    # Verify Python
    if ! command -v python3 &> /dev/null; then
        echo "ERROR: python3 not found"
        exit 1
    fi
    echo "OK: Using Python: $(command -v python3)"

    # Install mkdocs-translate (UTF-8 fixed version)
    echo "Installing mkdocs-translate (UTF-8 fixed version)..."
    python3 -m pip install --upgrade pip
    python3 -m pip install git+https://github.com/petersmythe/translate.git
    
    # Install MkDocs and plugins
    echo "Installing MkDocs, theme, and plugins..."
    python3 -m pip install mkdocs mkdocs-material mkdocs-minify-plugin
    
    echo "OK: Environment setup complete!"
}

# Build Sphinx documentation
build_sphinx() {
    echo "Building Sphinx documentation..."
    
    # Build English documentation
    echo "Building English documentation..."
    cd doc/en
    python3 -m pip install -r requirements.txt
    mvn clean compile
    cd ../..
    
    # Build Chinese documentation  
    echo "Building Chinese documentation..."
    cd doc/zhCN
    python3 -m pip install -r ../en/requirements.txt
    mvn clean compile
    cd ../..
    
    echo "OK: Sphinx build complete!"
}

# Convert RST to Markdown using directory-based approach
convert_rst() {
    echo "Converting RST to Markdown using mkdocs_translate..."
    
    # Check prerequisites
    echo "Checking prerequisites..."
    if ! command -v pandoc &> /dev/null; then
        echo "ERROR: pandoc is required but not installed"
        exit 1
    fi
    echo "OK: pandoc found"
    
    # Test mkdocs_translate
    echo "Testing mkdocs_translate..."
    if ! mkdocs_translate --help &> /dev/null; then
        echo "ERROR: mkdocs_translate not working"
        exit 1
    fi
    echo "OK: mkdocs_translate is working"
    
    # Process each documentation directory individually
    echo "Processing documentation directories..."
    
    # Array of actual documentation directories: directory_path:description:lang:type
    declare -a DOC_DIRS=(
        "doc/en/user:en user:en:user"
        "doc/en/developer:en developer:en:developer" 
        "doc/en/docguide:en docguide:en:docguide"
        "doc/zhCN/user:zhCN user:zhCN:user"
    )
    
    # Process each directory
    for DIR_INFO in "${DOC_DIRS[@]}"; do
        # Split the directory path and description
        IFS=':' read -r DOC_DIR DOC_DESC LANG TYPE <<< "$DIR_INFO"
        
        if [ -d "$DOC_DIR" ] && [ -d "$DOC_DIR/source" ]; then
            echo ""
            echo "Converting $DOC_DESC documentation..."
            echo "   Working directory: $DOC_DIR"
            
            # Count RST files
            RST_COUNT=$(find "$DOC_DIR/source" -name "*.rst" 2>/dev/null | wc -l)
            echo "   Found $RST_COUNT RST files"
            
            # Save current directory and change to the doc directory
            ORIGINAL_DIR=$(pwd)
            cd "$DOC_DIR"
            
            # Create mkdocs.yml if it doesn't exist
            if [ ! -f "mkdocs.yml" ]; then
                echo "   Creating mkdocs.yml configuration..."
                if [ "$LANG" = "zhCN" ]; then
                    cat > mkdocs.yml << EOF
site_name: GeoServer $TYPE 文档 (中文)
site_description: GeoServer $TYPE 中文文档
site_author: GeoServer Project
theme:
  name: material
  language: zh
  features:
    - navigation.tabs
    - navigation.sections
    - navigation.expand
    - navigation.top
    - search.highlight
    - search.share
nav:
  - 首页: index.md
markdown_extensions:
  - admonition
  - pymdownx.details
  - pymdownx.superfences
  - tables
  - toc:
      permalink: true
plugins:
  - search
site_dir: ../../gh-pages-output/$LANG/$TYPE
EOF
                else
                    cat > mkdocs.yml << EOF
site_name: GeoServer $TYPE Documentation (English)
site_description: GeoServer $TYPE documentation in English
site_author: GeoServer Project
theme:
  name: material
  language: en
  features:
    - navigation.tabs
    - navigation.sections
    - navigation.expand
    - navigation.top
    - search.highlight
    - search.share
nav:
  - Home: index.md
markdown_extensions:
  - admonition
  - pymdownx.details
  - pymdownx.superfences
  - tables
  - toc:
      permalink: true
plugins:
  - search
site_dir: ../../gh-pages-output/$LANG/$TYPE
EOF
                fi
            fi
            
            echo "   Step 1: Initializing docs directory..."
            mkdocs_translate init
            
            echo "   Step 2: Scanning RST files..."
            mkdocs_translate scan
            
            echo "   Step 3: Converting RST to Markdown..."
            mkdocs_translate migrate
            
            echo "   Step 4: Generating navigation..."
            mkdocs_translate nav > nav_generated.yml
            
            # Return to original directory
            cd "$ORIGINAL_DIR"
            
            echo "   Conversion complete for $DOC_DESC"
            
        else
            echo "   Skipping $DOC_DESC (directory not found: $DOC_DIR)"
        fi
    done
    
    echo "OK: RST to Markdown conversion complete!"
}

# Build MkDocs sites
build_mkdocs() {
    echo "Building MkDocs sites..."
    
    # Create output directory
    mkdir -p gh-pages-output
    
    # Array of actual documentation directories to build
    declare -a MKDOCS_DIRS=(
        "doc/en/user:en/user"
        "doc/en/developer:en/developer" 
        "doc/en/docguide:en/docguide"
        "doc/zhCN/user:zhCN/user"
    )
    
    # Build each MkDocs site
    for DIR_INFO in "${MKDOCS_DIRS[@]}"; do
        # Split the directory path and output description
        MKDOCS_DIR="${DIR_INFO%%:*}"
        OUTPUT_DESC="${DIR_INFO##*:}"
        
        if [ -d "$MKDOCS_DIR" ] && [ -f "$MKDOCS_DIR/mkdocs.yml" ]; then
            echo "Building $OUTPUT_DESC documentation..."
            
            # Save current directory and change to mkdocs directory
            ORIGINAL_DIR=$(pwd)
            cd "$MKDOCS_DIR"
            
            # Build the site (outputs to site_dir specified in mkdocs.yml)
            mkdocs build
            
            # Return to original directory
            cd "$ORIGINAL_DIR"
        else
            echo "Skipping $OUTPUT_DESC (directory not found: $MKDOCS_DIR)"
        fi
    done
    
    # Create main index page
    echo "Creating main index page..."
    cat > gh-pages-output/index.html << EOF
<!DOCTYPE html>
<html>
<head>
    <title>GeoServer Documentation</title>
    <meta charset="utf-8">
</head>
<body>
    <h1>GeoServer Documentation</h1>
    <h2>English</h2>
    <ul>
        <li><a href="en/user/">User Guide</a></li>
        <li><a href="en/developer/">Developer Guide</a></li>
        <li><a href="en/docguide/">Documentation Guide</a></li>
    </ul>
    <h2>中文 (Chinese)</h2>
    <ul>
        <li><a href="zhCN/user/">用户指南</a></li>
        <li><a href="zhCN/developer/">开发者指南</a></li>
        <li><a href="zhCN/docguide/">文档指南</a></li>
    </ul>
</body>
</html>
EOF
    
    echo "OK: MkDocs build complete!"
    echo "Documentation available at: gh-pages-output/index.html"
}

# Main execution
case "$STEP" in
    setup)
        setup_environment
        ;;
    sphinx)
        build_sphinx
        ;;
    convert)
        convert_rst
        ;;
    mkdocs)
        build_mkdocs
        ;;
    all)
        setup_environment
        build_sphinx
        convert_rst
        build_mkdocs
        ;;
esac

echo "Test completed successfully!"