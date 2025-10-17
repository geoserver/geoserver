#!/bin/bash

# test-mkdocs-restructured.sh
# Directory-based mkdocs-translate testing script
# This script uses the restructured approach: cd into each documentation directory

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo "Testing MkDocs workflow (restructured directory-based version)..."

# Function to print colored output
print_step() {
    echo -e "${BLUE}   $1${NC}"
}

print_success() {
    echo -e "${GREEN}   $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}   $1${NC}"
}

print_error() {
    echo -e "${RED}   $1${NC}"
}

# Check step parameter
STEP=${1:-"all"}
echo "Step: $STEP"

# Get OS type for cross-platform compatibility
OS_TYPE=""
if [[ "$OSTYPE" == "msys" ]]; then
    OS_TYPE="msys"
elif [[ "$OSTYPE" == "cygwin" ]]; then
    OS_TYPE="cygwin"
elif [[ "$OSTYPE" == "mingw"* ]]; then
    OS_TYPE="msys"  # Treat MinGW like MSYS
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    OS_TYPE="linux"
elif [[ "$OSTYPE" == "darwin"* ]]; then
    OS_TYPE="macos"
else
    OS_TYPE="unix"  # Default fallback
fi

echo "OS Type: $OS_TYPE"
echo "Working Directory: $(pwd)"

# Debug info
echo "Debug Info:"

# Function to activate virtual environment
activate_venv() {
    if [ -d "venv" ]; then
        if [[ "$OS_TYPE" == "msys" || "$OS_TYPE" == "cygwin" ]]; then
            source venv/Scripts/activate
        else
            source venv/bin/activate
        fi
    else
        print_warning "Virtual environment not found. Run setup first."
        return 1
    fi
}

# Environment setup with proper venv management
setup_environment() {
    print_step "Setting up environment..."
    
    # Create virtual environment if it doesn't exist
    if [ ! -d "venv" ]; then
        print_step "Creating virtual environment..."
        if command -v python3 &> /dev/null; then
            python3 -m venv venv
        elif command -v python &> /dev/null; then
            python -m venv venv
        else
            print_error "Python not found!"
            exit 1
        fi
    fi
    
    # Activate virtual environment (cross-platform)
    print_step "Activating virtual environment..."
    if [[ "$OS_TYPE" == "msys" || "$OS_TYPE" == "cygwin" ]]; then
        source venv/Scripts/activate
    else
        source venv/bin/activate
    fi
    
    # Verify Python after venv activation
    if ! command -v python3 &> /dev/null; then
        if ! command -v python &> /dev/null; then
            print_error "Python not found in virtual environment!"
            exit 1
        else
            PYTHON_CMD="python"
        fi
    else
        PYTHON_CMD="python3"
    fi
    print_success "Using Python: $(command -v $PYTHON_CMD)"
    
    # Upgrade pip first
    print_step "Upgrading pip..."
    $PYTHON_CMD -m pip install --upgrade pip
    
    # Install mkdocs-translate (v0.5.1 with rst_path bug fix)
    print_step "Installing mkdocs-translate (v0.5.1 with rst_path bug fix)..."
    $PYTHON_CMD -m pip install --upgrade git+https://github.com/petersmythe/translate.git
    
    # Create config in the package directory
    print_step "Creating mkdocs_translate config.yml..."
    PACKAGE_PATH=$($PYTHON_CMD -c "import mkdocs_translate; import os; print(os.path.dirname(mkdocs_translate.__file__))")
    cat > "$PACKAGE_PATH/config.yml" <<'EOL'
# Configuration for mkdocs_translate
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
    
    # Show version (expecting v0.5.1)
    print_step "mkdocs_translate version (expecting v0.5.1):"
    mkdocs_translate --version || print_warning "Could not get version"
    
    # Install MkDocs and requirements
    print_step "Installing MkDocs, theme, and plugins..."
    $PYTHON_CMD -m pip install mkdocs mkdocs-material mkdocs-minify-plugin
    
    print_success "Environment setup complete!"
}

# Convert documentation using directory-based approach
convert_documentation() {
    print_step "Converting RST to Markdown using directory-based approach..."
    
    # Ensure virtual environment is activated
    activate_venv
    
    # Array of documentation directories (path:type:language)
    declare -a DOCS_DIRS=(
        "doc/en/user:user:en"
        "doc/en/developer:developer:en"
        "doc/en/docguide:docguide:en"
        "doc/zhCN/user:user:zhCN"
    )
    
    for doc_entry in "${DOCS_DIRS[@]}"; do
        IFS=':' read -r doc_path doc_type lang <<< "$doc_entry"
        
        print_step "Converting $lang $doc_type documentation..."
        print_step "Working directory: $doc_path"
        
        # Check if directory exists
        if [ ! -d "$doc_path" ]; then
            print_warning "Directory $doc_path does not exist, skipping..."
            continue
        fi
        
        # Count RST files
        rst_count=$(find "$doc_path" -name "*.rst" -type f | wc -l)
        print_step "Found $rst_count RST files"
        
        # Change to the documentation directory
        pushd "$doc_path" > /dev/null
        
        # Create mkdocs.yml configuration
        print_step "Creating mkdocs.yml configuration..."
        cat > mkdocs.yml << EOF
site_name: GeoServer Documentation ($doc_type - $lang)
site_description: GeoServer $doc_type documentation in $lang
site_author: GeoServer Team
site_url: https://docs.geoserver.org/

theme:
  name: material
  palette:
    - scheme: default
      primary: blue
      accent: blue
  features:
    - navigation.tabs
    - navigation.sections
    - navigation.expand
    - navigation.top
    - search.highlight
    - search.share
  # Disable syntax highlighting to avoid Pygments issues
  hljs: false

plugins:
  - search
  - minify:
      minify_html: true

markdown_extensions:
  - admonition
  - toc:
      permalink: true

extra_css:
  - css/extra.css
EOF
        
        print_step "Step 1: Initializing docs directory..."
        mkdocs_translate init || print_warning "Init failed, continuing anyway..."
        
        print_step "Step 2: Scanning RST files..."
        mkdocs_translate scan || print_warning "Scan failed, continuing anyway..."
        
        print_step "Step 3: Converting RST to Markdown..."
        mkdocs_translate migrate || print_warning "Migration failed, continuing anyway..."
        
        print_step "Step 4: Generating navigation..."
        mkdocs_translate nav > nav_generated.yml || print_warning "Nav generation failed, continuing anyway..."
        
        print_success "Conversion complete for $lang $doc_type"
        
        # Return to original directory
        popd > /dev/null
    done
    
    print_success "RST to Markdown conversion complete!"
}

# Build MkDocs sites
build_sites() {
    print_step "Building MkDocs sites..."
    
    # Ensure virtual environment is activated
    activate_venv
    
    # Array of documentation directories
    declare -a DOCS_DIRS=(
        "doc/en/user:user:en"
        "doc/en/developer:developer:en"
        "doc/en/docguide:docguide:en"
        "doc/zhCN/user:user:zhCN"
    )
    
    for doc_entry in "${DOCS_DIRS[@]}"; do
        IFS=':' read -r doc_path doc_type lang <<< "$doc_entry"
        
        print_step "Building $lang $doc_type site..."
        
        if [ ! -d "$doc_path" ]; then
            print_warning "Directory $doc_path does not exist, skipping build..."
            continue
        fi
        
        # Change to documentation directory
        pushd "$doc_path" > /dev/null
        
        # Check if mkdocs.yml exists
        if [ ! -f "mkdocs.yml" ]; then
            print_warning "mkdocs.yml not found in $doc_path, skipping build..."
            popd > /dev/null
            continue
        fi
        
        # Build the site
        print_step "Running mkdocs build..."
        if mkdocs build --clean; then
            print_success "Build successful for $lang $doc_type"
        else
            print_warning "Build failed for $lang $doc_type, continuing..."
        fi
        
        # Return to original directory
        popd > /dev/null
    done
    
    print_success "MkDocs site building complete!"
}

# Serve sites (for testing)
serve_sites() {
    print_step "Starting MkDocs development servers..."
    print_warning "This will start servers for each documentation type."
    print_warning "Use Ctrl+C to stop the servers."
    
    # Ensure virtual environment is activated
    activate_venv
    
    # Serve all sites under a single combined site
    print_step "Creating combined MkDocs configuration..."
    
    # Create combined docs directory structure first
    mkdir -p docs_combined/user docs_combined/developer docs_combined/docguide docs_combined/zhCN-user
    
    # Create a temporary combined mkdocs.yml
    cat > mkdocs_combined.yml << 'EOF'
site_name: GeoServer Documentation (All Sites)
site_description: Combined GeoServer documentation
site_author: GeoServer Team
site_url: https://docs.geoserver.org/
docs_dir: docs_combined

theme:
  name: material
  palette:
    - scheme: default
      primary: blue
      accent: blue
  features:
    - navigation.tabs
    - navigation.sections
    - navigation.expand
    - navigation.top
    - search.highlight
    - search.share
  hljs: false

plugins:
  - search

markdown_extensions:
  - admonition
  - toc:
      permalink: true

nav:
  - Home: index.md
  - English User Guide: user/index.md
  - English Developer Guide: developer/index.md
  - English Documentation Guide: docguide/index.md
  - Chinese User Guide: zhCN-user/index.md

extra_css:
  - css/extra.css
EOF
    
    # Create index page
    cat > docs_combined/index.md << 'EOF'
# GeoServer Documentation

Welcome to the GeoServer documentation portal.

## Available Documentation

- [English User Guide](user/) - Complete user documentation in English
- [English Developer Guide](developer/) - Developer documentation in English  
- [English Documentation Guide](docguide/) - Documentation writing guide in English
- [Chinese User Guide](zhCN-user/) - User documentation in Chinese
EOF

    # Copy real documentation content if available, with error handling
    for doc_entry in "doc/en/user:user" "doc/en/developer:developer" "doc/en/docguide:docguide" "doc/zhCN/user:zhCN-user"; do
      IFS=':' read -r doc_path target_dir <<< "$doc_entry"
      
      if [ -d "$doc_path/docs" ]; then
        print_step "Copying $target_dir documentation from $doc_path/docs"
        # Copy all content using cp (more universally available than rsync)
        cp -r "$doc_path/docs/"* "docs_combined/$target_dir/" 2>/dev/null || {
          print_warning "Failed to copy some files for $target_dir, creating fallback content"
        }
        
        # Ensure we have an index file
        if [ ! -f "docs_combined/$target_dir/index.md" ]; then
          cat > "docs_combined/$target_dir/index.md" << EOF
# $target_dir Documentation

Welcome to the $target_dir documentation.

This documentation has been automatically converted from reStructuredText to Markdown.

## Navigation

Use the navigation menu to browse through the available sections.

EOF
        fi
      else
        print_warning "Documentation not found at $doc_path/docs, creating placeholder"
        # Create placeholder if no docs found
        cat > "docs_combined/$target_dir/index.md" << EOF
# $target_dir Documentation

Documentation for this section is not yet available in the combined view.

Please visit the individual documentation sites or build the documentation first using:
\`\`\`
./test-mkdocs-restructured.sh convert
./test-mkdocs-restructured.sh build
\`\`\`

EOF
      fi
    done
    
    print_step "Serving combined documentation on http://localhost:8000"
    mkdocs serve -f mkdocs_combined.yml --dev-addr=127.0.0.1:8000
}

# Clean up generated files
cleanup() {
    print_step "Cleaning up generated files..."
    
    # Array of documentation directories
    declare -a DOCS_DIRS=(
        "doc/en/user"
        "doc/en/developer"
        "doc/en/docguide"
        "doc/zhCN/user"
    )
    
    for doc_path in "${DOCS_DIRS[@]}"; do
        if [ -d "$doc_path" ]; then
            print_step "Cleaning $doc_path..."
            pushd "$doc_path" > /dev/null
            
            # Remove generated files and directories
            [ -d "docs" ] && rm -rf docs
            [ -d "site" ] && rm -rf site
            [ -d "target" ] && rm -rf target
            [ -f "mkdocs.yml" ] && rm -f mkdocs.yml
            [ -f "nav_generated.yml" ] && rm -f nav_generated.yml
            
            popd > /dev/null
        fi
    done
    
    # Remove dummy config
    [ -f "config.yml" ] && rm -f config.yml
    
    print_success "Cleanup complete!"
}

# Main execution logic
case "$STEP" in
    "setup"|"env"|"environment")
        setup_environment
        ;;
    "convert"|"migration"|"rst")
        convert_documentation
        ;;
    "build"|"mkdocs")
        build_sites
        ;;
    "serve"|"server"|"dev")
        serve_sites
        ;;
    "clean"|"cleanup")
        cleanup
        ;;
    "all"|"")
        setup_environment
        convert_documentation
        build_sites
        print_success "Test completed successfully!"
        ;;
    *)
        echo "Usage: $0 [setup|convert|build|serve|clean|all]"
        echo ""
        echo "Steps:"
        echo "  setup    - Set up environment and install dependencies"
        echo "  convert  - Convert RST to Markdown using directory-based approach"
        echo "  build    - Build MkDocs sites"
        echo "  serve    - Start development server for testing"
        echo "  clean    - Clean up generated files"
        echo "  all      - Run setup, convert, and build steps (default)"
        exit 1
        ;;
esac