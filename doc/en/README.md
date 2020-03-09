# Documentation Instructions

For writing guide please generate and review ``docguide`` below. Documentation is written in a combination of:

* [swagger.io](http://swagger.io) - REST API reference documentation
* [sphinx-doc.org](http://www.sphinx-doc.org): user manual, developers guide and documentation guide

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

Profiles are defined to build individual manuals:

    mvn compile -Puser
    mvn compile -Pdeveloper
    mvn compile -Pdocguide

To generate user pdf:

    mvn compile -Puser-pdf

The ant ``build.xml`` can also be called directly with the ``project.version`` name:

    ant user -Dproject.version=2.18.0
    ant developer -Dproject.version=2.18.0
    ant docguide -Dproject.version=2.18.0