# Documentation Instructions

For writing guide please generate and review [docguide](https://docs.geoserver.org/3.0.x/en/docguide/). Documentation is written in a combination of:

* [material for mkdocs](https://squidfunk.github.io/mkdocs-material/): user manual, developers guide and documentation guide
* [swagger.io](http://swagger.io): REST API reference documentation

GeoServer documentation is released using [Creative Commons Attribution 4.0 International](LICENSE.md).

## Building with Python

The documentation is written with [mkdocs](https://www.mkdocs.org/), which is a Python documentation generator. We use [Material for mkdocs](https://squidfunk.github.io/mkdocs-material/) theme which provides excellent documentation.

1. From the root of your GeoServer checkout:

   ```bash
   python3 -m venv venv
   ```

2. Activate virtual environment and install (or update) requirements:
   ```bash
   source venv/bin/activate
   pip install -r requirements.txt
   ```
   
3. Use ***mkdocs*** to serve preview from virtual environment:

   ```bash
   mkdocs serve
   ```

4. Open preview in browser:

   ```bash
   python3 -m webbrowser http://localhost:8000
   ```

## Building with Maven

To build:
```bash
mvn clean install
```

The documentation is packaged for offline use (inlining javascript and using `index.html` links).

### index.html

The file `index.html` is the landing page for the [online documentation](https://docs.geoserver.org/index.html). It exists outside of the version hierarchy of the rest of the documentation.

### REST API

To generate the REST API documentation:

```bash
mvn process-resources
```
    
To generate a specific REST API endpoint:

```bash
mvn process-resources:system-status
```

The api documentation is packaged for offline use, as a replacement for the interactive swagger docs
available on the website (or in local preview).

#### Writing

The ant ``build.xml`` can also be called directly:

```
ant build
```

This uses ``mkdocs build`` to generate documentation into ``../../target/index.html``.

To view content while editing:
```
ant site
```

This uses ``mkdocs serve`` to serve docs locally.

The `../version.py` mkdocs hook looks up the current project version, and most recent release, information in `src/pom.xml`. This information is made availabel to the macros plugin for use when writing.