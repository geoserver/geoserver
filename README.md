<img src="/doc/themes/geoserver/static/GeoServer_500.png" width="353">

[![Gitter](https://badges.gitter.im/geoserver/geoserver.svg)](https://gitter.im/geoserver/geoserver?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![DOI](https://zenodo.org/badge/2751199.svg)](https://zenodo.org/badge/latestdoi/2751199)

[GeoServer](https://geoserver.org) is an open source software server written in Java that 
allows users to share and edit geospatial data. Designed for interoperability, it publishes data from 
any major spatial data source using open standards.

Being a community-driven project, GeoServer is developed, tested, and supported by a diverse group of 
individuals and organizations from around the world.

## Standards

GeoServer forms a core component of the Geospatial Web with full support of geospatial standards.

GeoServer is the reference implementation of the Open Geospatial Consortium (OGC) 
Web Feature Service (WFS) and Web Coverage Service (WCS) standards, as well as a high performance 
certified compliant Web Map Service (WMS). GeoServer implements additional standards
including OGC API - Features, Catalog Service for the Web (CSW), and implementing Web Processing Service (WPS).
These services supports a wide range of data formats, with GeoServer actings as the reference implementation for GeoPackage and GeoTIFF formats, and implementing support for GML and de facto formats like Shapefile.

Visit [GeoServer Product Details](https://portal.ogc.org/public_ogc/compliance/product.php?pid=1874) for the
current certification status of GeoServer.

## License

GeoServer licensed under the [GPL](https://docs.geoserver.org/3.0.x/en/user/introduction/license/).

## Using

Please refer to the [user guide](https://docs.geoserver.org/3.0.x/en/user/) for information
on how to install and use GeoServer.

## Building

GeoServer uses [Apache Maven](https://maven.apache.org/) for a build system. To 
build the application run maven from the ```src``` directory.

```bash
mvn clean install
```

See the [developer guide](https://docs.geoserver.org/3.0.x/en/developer/) for more details.

## Documentation

GeoServer uses [Material for MkDocs](https://squidfunk.github.io/mkdocs-material/) for documentation written in Markdown. Use ``mkdocs`` to run the docs locally for feedback while editing:

Serve live preview locally:
```bash
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
mkdocs serve
```

Open preview in browser:
```bash
python3 -m webbrowser http://localhost:8000
```

See the [documentation guide])(https://docs.geoserver.org/3.0.x/en/docguide/) for more details.

## Bugs

GeoServer uses [JIRA](https://osgeo-org.atlassian.net/projects/GEOS), hosted by 
[Atlassian](https://www.atlassian.com/), for issue tracking.

<a id="mailing-lists"></a> <!-- to retain the existing anchor tag -->

## Community support

The [Community support page](https://geoserver.org/comm/) on the GeoServer web site provides
access to the various channels of communication, as well as some indication of the [code of conduct](https://geoserver.org/comm/userlist-guidelines.html) when posting to the groups.

## Contributing

Please read [the contribution guidelines](https://github.com/geoserver/geoserver/blob/main/CONTRIBUTING.md) before contributing pull requests to the GeoServer project.

## More Information

Visit the [website](https://geoserver.org/) and read the [docs](https://docs.geoserver.org/). 

