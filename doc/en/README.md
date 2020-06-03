# Documentation Instructions

For writing guide please generate and review ``docguide`` below. Documentation is written in a combination of:

* [swagger.io](http://swagger.io) - REST API reference documentation
* [sphinx-doc.org](http://www.sphinx-doc.org): user manual, developers guide and documentation guide

GeoServer documentation is released using [Creative Commons Attribution 4.0 International](LICENSE.md).

## Building with Maven

To build:

    mvn clean install

To package documentation into zip archives:

    mvn assembly:single

### REST API

To generate the REST API documentation:

    mvn process-resources
    
To generate a specific REST API endpoint:

    mvn process-resoruces:system-status
    

### Manuals

To build all restructured text documentation:

    mvn compile

And to package into zips:

    mvn package

Profiles are defined to build individual manuals:

    mvn compile -Puser
    mvn compile -Pdeveloper
    mvn compile -Pdocguide

And can be packaged individually:
    
    mvn package:single@user
    mvn package:single@developer
    mvn package:single@docguide

To generate user pdf:

    mvn compile -Puser-pdf

The ant ``build.xml`` can also be called directly with the ``project.version`` name:

    ant user -Dproject.version=2.18.0
    ant developer -Dproject.version=2.18.0
    ant docguide -Dproject.version=2.18.0
