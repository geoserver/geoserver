# Documentation Instructions

For writing guide please generate and review [docguide](https://docs.geoserver.org/3.0.x/en/docguide/). Documentation is written in a combination of:

* [material for mkdocs](https://squidfunk.github.io/mkdocs-material/): user manual, developers guide and documentation guide
* [swagger.io](http://swagger.io): REST API reference documentation

GeoServer documentation is released using [Creative Commons Attribution 4.0 International](LICENSE.md).

## Python Virtual Environment Setup

The documentation is written with [mkdocs](https://www.mkdocs.org/), which is a Python documentation generator. We use [Material for mkdocs](https://squidfunk.github.io/mkdocs-material/) theme which provides excellent documentation.

1. From the root of your GeoServer checkout:

   ```bash
   virtualenv venv
   ```

2. Activate virtual environment and install (or update) requirements:
   ```bash
   source venv/bin/activate
   pip install -r requirements.txt
   ```
   
3. Use ***mkdocs*** to preview from virtual environment:

   ```bash
   mkdocs serve
   ```

3. Preview:

   ```
   python3 -m webbrowser http://localhost:8000
   ```

## Building with Maven

To build:
```bash
mvn clean install
```

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

### Manuals

To build all restructured text documentation:

```bash
mvn compile
```

And to package into zips:

```bash
mvn package
```

Profiles are defined to build individual manuals:

```bash
mvn compile -Puser
mvn compile -Pdeveloper
mvn compile -Pdocguide
```

And can be packaged individually:

```bash    
mvn package:single@user
mvn package:single@developer
mvn package:single@docguide
```
To generate user pdf:

```bash
mvn compile -Puser-pdf
```
    
#### Writing

The ant ``build.xml`` can also be called directly:

```
ant user
```

This uses ``sphinx-build`` to generate documentation into ``target/user/html/index.html``.

To view content while editing:
```
ant user-site
```

This uses ``sphinx-autobuild`` to serve docs on next available port, opening a browser to review generated pages. The browser will refresh as pages are edited and saved.

Additional targets are available:
```
ant developer
ant developer-site
ant docguide
ant docguide-site
```

The `config.py` script looks up the current project version, and most recent release, information in `src/pom.xml`.

* When calling ant directly terminal output reports these details during startup:
  
  ```
  
  ```
  
* When called via `mvn` a flag `-Dsphinx.options=--quite` passed to ant, and is used to ask sphinx to reduce output