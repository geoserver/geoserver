# Documentation Instructions

For writing guide please generate and review ``docguide`` below. Documentation is written in a combination of:

* [swagger.io](http://swagger.io) - REST API reference documentation
* [sphinx-doc.org](http://www.sphinx-doc.org): user manual, developers guide and documentation guide

## Building with Maven

To build:

    mvn clean install

To generate the REST API documentation:

    mvn process-resources

To build all restructured text documentation:

    mvn compile

Profiles are defined to build individual manuals:

    mvn compile -Puser
    mvn compile -Pdeveloper
    mvn compile -Pdocguide

To package documentation into zip archives:

    mvn assembly:single

## Building with ANT

The ant ``build.xml`` can also be called directly:

    ant user
    ant developer
    ant docguide