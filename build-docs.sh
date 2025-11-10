#!/bin/bash
# Build script that replicates the GitHub Actions workflow steps exactly

set -e

echo "=== GeoServer MkDocs Build Script ==="
echo "Replicating GitHub Actions workflow locally"
echo ""

# Verify we're in the right directory
if [ ! -f "src/pom.xml" ] || [ ! -d "doc" ]; then
    echo "ERROR: This script must be run from the GeoServer project root"
    echo "Expected files: src/pom.xml, doc/ directory"
    exit 1
fi

echo "✓ Project structure validated"

# Setup Python virtual environment
echo ""
echo "=== Setting up Python Virtual Environment ==="
python3 -m venv venv
source venv/bin/activate

echo "✓ Virtual environment created and activated"

# Install Pandoc 3.5 (matching workflow)
echo ""
echo "=== Installing Pandoc 3.5 ==="
PANDOC_VERSION="3.5"

# Check OS and install accordingly
if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    echo "Installing Pandoc ${PANDOC_VERSION} on Linux..."
    wget -q https://github.com/jgm/pandoc/releases/download/${PANDOC_VERSION}/pandoc-${PANDOC_VERSION}-1-amd64.deb
    sudo dpkg -i pandoc-${PANDOC_VERSION}-1-amd64.deb
    rm pandoc-${PANDOC_VERSION}-1-amd64.deb
elif [[ "$OSTYPE" == "darwin"* ]]; then
    echo "Installing Pandoc ${PANDOC_VERSION} on macOS..."
    wget -q https://github.com/jgm/pandoc/releases/download/${PANDOC_VERSION}/pandoc-${PANDOC_VERSION}-x86_64-macOS.pkg
    sudo installer -pkg pandoc-${PANDOC_VERSION}-x86_64-macOS.pkg -target /
    rm pandoc-${PANDOC_VERSION}-x86_64-macOS.pkg
else
    echo "⚠ WARNING: Unsupported OS for automatic Pandoc installation"
    echo "Please install Pandoc 3.5 manually from: https://github.com/jgm/pandoc/releases"
    echo "Current Pandoc version:"
    pandoc --version || echo "Pandoc not found"
fi

echo "=== Pandoc Version ==="
pandoc --version
echo ""
echo "✓ Pandoc installation complete"

# Install Python dependencies (exact versions from workflow)
echo ""
echo "=== Installing Python Dependencies ==="
echo "Installing mkdocs-translate from petersmythe fork..."
pip install git+https://github.com/petersmythe/translate.git

echo "Installing MkDocs and plugins..."
pip install mkdocs mkdocs-material mkdocs-minify-plugin

echo "✓ Python dependencies installed"

# Create minimal config for mkdocs_translate
echo ""
echo "=== Configuring mkdocs_translate ==="
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

echo "✓ mkdocs_translate configured"

# Validate mkdocs_translate installation
echo ""
echo "=== Validating mkdocs_translate Installation ==="
echo "Checking mkdocs_translate version..."
mkdocs_translate --version || echo "Version check not available"
mkdocs_translate --help > /dev/null || (echo "mkdocs_translate validation failed" && exit 1)
echo "✓ mkdocs_translate is working correctly"

# Show environment information
echo ""
echo "=== Environment Information ==="
echo "Pandoc version:"
pandoc --version | head -2
echo ""
echo "mkdocs_translate version:"
mkdocs_translate --version
echo ""
echo "Python-Markdown version:"
python -c "import markdown; print(f'Python-Markdown version: {markdown.__version__}')"
echo ""

# Build Sphinx documentation first (English) - COMMENTED OUT
# echo ""
# echo "=== Building Sphinx Documentation ==="
# cd doc/en
# 
# if [ -f requirements.txt ]; then
#     echo "Installing Sphinx requirements..."
#     pip install -r requirements.txt
#     echo "✓ Sphinx requirements installed"
# else
#     echo "WARNING: No requirements.txt found in doc/en"
# fi
# 
# echo "Running Maven compile..."
# mvn -B -ntp compile
# echo "✓ Maven compile completed"
# 
# cd - > /dev/null

# Convert RST to Markdown
echo ""
echo "=== Converting RST to Markdown ==="

# Array of documentation directories to process (same as workflow)
declare -a DOC_DIRS=(
  "doc/en/docguide:docguide:en"
  "doc/en/developer:developer:en"
  "doc/en/user:user:en"
)

# Process each directory
for DIR_INFO in "${DOC_DIRS[@]}"; do
  IFS=':' read -r DOC_DIR DOC_TYPE LANG <<< "$DIR_INFO"
  
  if [ -d "$DOC_DIR" ] && [ -d "$DOC_DIR/source" ]; then
    echo "Converting $LANG $DOC_TYPE documentation..."
    echo "   Working directory: $DOC_DIR"
    
    # Count RST files
    RST_COUNT=$(find "$DOC_DIR/source" -name "*.rst" 2>/dev/null | wc -l)
    echo "   Found $RST_COUNT RST files"
    
    # Change to the documentation directory
    cd "$DOC_DIR"
    
    # Create mkdocs.yml if it doesn't exist
    if [ ! -f "mkdocs.yml" ]; then
      echo "   Creating mkdocs.yml configuration..."
      cat > mkdocs.yml << EOF
site_name: GeoServer $DOC_TYPE Documentation (English)
site_description: GeoServer $DOC_TYPE documentation in English
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
site_dir: ../../gh-pages-output/$LANG/$DOC_TYPE
EOF
    fi
    
    echo "   Step 1: Initializing docs directory..."
    mkdocs_translate init || (echo "Init failed, exiting..." && exit 1)
    
    echo "   Step 2: Scanning RST files..."
    mkdocs_translate scan || (echo "Scan failed, exiting..." && exit 2)
    
    echo "   Step 3: Converting RST to Markdown..."
    mkdocs_translate migrate || (echo "Migration failed, exiting..." && exit 3)
    
    echo "   Step 4: Generating navigation..."
    mkdocs_translate nav > nav_generated.yml || (echo "Nav generation failed, exiting..." && exit 4)
    
    # Debug: Show generated markdown for index.md
    if [ -f "docs/index.md" ]; then
      echo "   === Generated docs/index.md (first 30 lines) ==="
      head -30 docs/index.md
      echo "   === End of index.md preview ==="
    fi
    
    # Ensure docs directory exists with fallback content
    if [ ! -d "docs" ]; then
      echo "   Creating fallback docs directory..."
      mkdir -p docs
      echo "# GeoServer $DOC_TYPE Documentation" > docs/index.md
    fi
    
    # Return to project root
    cd - > /dev/null
    
    echo "   ✓ Conversion complete for $LANG $DOC_TYPE"
  else
    echo "   ⚠ Skipping $LANG $DOC_TYPE (directory not found: $DOC_DIR)"
  fi
done

# Build all MkDocs sites
echo ""
echo "=== Building MkDocs Sites ==="

# Create main output directory
mkdir -p gh-pages-output

# Array of documentation directories to build (same as workflow)
declare -a MKDOCS_DIRS=(
  "doc/en/docguide:docguide:en"
  "doc/en/developer:developer:en"
  "doc/en/user:user:en"
)

# Build each MkDocs site
for DIR_INFO in "${MKDOCS_DIRS[@]}"; do
  IFS=':' read -r DOC_DIR DOC_TYPE LANG <<< "$DIR_INFO"
  
  if [ -d "$DOC_DIR" ] && [ -f "$DOC_DIR/mkdocs.yml" ]; then
    echo "Building $LANG $DOC_TYPE documentation..."
    
    # Change to the documentation directory
    cd "$DOC_DIR"
    
    # Debug: Show the mkdocs.yml being used
    echo "   === Contents of $DOC_DIR/mkdocs.yml ==="
    echo "   --- markdown_extensions section ---"
    grep -A 10 "markdown_extensions:" mkdocs.yml || echo "   No markdown_extensions found"
    echo "   --- end of markdown_extensions ---"
    echo ""
    
    # Ensure docs directory exists
    if [ ! -d "docs" ]; then
      mkdir -p docs
      echo "# GeoServer $DOC_TYPE Documentation" > docs/index.md
      echo "Documentation build in progress." >> docs/index.md
    fi
    
    # Build the site - fail on error (site_dir already set in mkdocs.yml)
    if ! mkdocs build --clean; then
      echo "❌ ERROR: MkDocs build FAILED for $LANG/$DOC_TYPE"
      echo "Build cannot continue with errors. Please fix the issues above."
      exit 1
    fi
    echo "✓ Successfully built $LANG/$DOC_TYPE"
    
    # Return to project root
    cd - > /dev/null
  else
    echo "⚠ Skipping $LANG $DOC_TYPE (directory or mkdocs.yml not found: $DOC_DIR)"
  fi
done

# Create main index page
echo ""
echo "=== Creating Main Index Page ==="
mkdir -p gh-pages-output
cat > gh-pages-output/index.html <<EOL
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>GeoServer Documentation Guide</title>
    <style>
        body { font-family: Arial, sans-serif; max-width: 800px; margin: 50px auto; padding: 20px; }
        h1 { color: #2c3e50; }
        .doc-link { display: block; padding: 20px; background: #f8f9fa; border: 1px solid #dee2e6; border-radius: 8px; text-decoration: none; color: #495057; transition: background-color 0.2s; margin: 20px 0; }
        .doc-link:hover { background: #e9ecef; }
        .server-info { background: #d4edda; border: 1px solid #c3e6cb; border-radius: 5px; padding: 15px; margin: 20px 0; }
    </style>
</head>
<body>
    <h1>MkDocs build</h1>
    <p>For comparison against the current Sphinx documentation</p>
    
    <div class="server-info">
        <strong>Local Development Server:</strong><br>
        To serve this documentation locally, run: <code>python -m http.server 8000</code> from the gh-pages-output directory<br>
        Then visit: <a href="http://localhost:8000">http://localhost:8000</a>
    </div>
    
    <h2>English Documentation</h2>
    <a href="en/docguide/" class="doc-link">
        <strong>Documentation Guide</strong><br>
        <small>Guidelines for editing GeoServer docs</small>
    </a>
    
    <a href="en/developer/" class="doc-link">
        <strong>Developer Guide</strong><br>
        <small>Developer documentation and programming guide</small>
    </a>
    
    <a href="en/user/" class="doc-link">
        <strong>User Guide</strong><br>
        <small>Complete user documentation and tutorials</small>
    </a>

</body>
</html>
EOL

echo "✓ Main index page created"

echo ""
echo "=== Build Complete! ==="
echo ""
echo "📁 Output directory: gh-pages-output/"
echo "🌐 To serve locally:"
echo "   cd gh-pages-output"
echo "   python -m http.server 8000"
echo "   Open: http://localhost:8000"
echo ""
echo "📖 Direct access to documentation:"
echo "   Documentation Guide: gh-pages-output/en/docguide/index.html"
echo "   Developer Guide: gh-pages-output/en/developer/index.html"
echo "   User Guide: gh-pages-output/en/user/index.html"
echo ""

# Deactivate virtual environment
deactivate