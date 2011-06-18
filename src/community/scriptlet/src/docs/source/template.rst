Template Rendering
==================
GeoServer uses the `Freemarker templating language <http://freemarker.org/>`_
for providing customizable output, and you can continue to use this with the
scriptlet extension.  Here is a small script that generates some information and
formats it using Freemarker::

    var TemplateRepresentation = 
        Packages.org.restlet.ext.freemarker.TemplateRepresentation;
    var MediaType = Packages.org.restlet.data.MediaType;

    var config = new Packages.freemarker.template.Configuration();
    config.setDirectoryForTemplateLoading(loader.find("scripts/templates/"));

    var context = {"message": "hello friends", "date": new java.util.Date()};
    var map = new java.util.HashMap();
    for (var x in context) {
        map.put(x, context[x]);
    }

    response.setEntity(new TemplateRepresentation(
        "hello.ftl", config,map, MediaType.TEXT_HTML
    ));

This script is only a little more complicated than the one discussed in
:doc:`hello`.  Again, we're accessing a Java library; this time it is the
Freemarker template engine.  The configuration is fairly straightforward,
especially if you are already familiar with Freemarker::

    var config = new Packages.freemarker.template.Configuration();
    config.setDirectoryForTemplateLoading(loader.find("scripts/templates/"));

Populating the template context is a little trickier.  Freemarker doesn't
understand JavaScript objects as context objects, so we have to translate to a
Java HashMap::

    var map = new java.util.HashMap();
    for (var x in context) {
        map.put(x, context[x]);
    }

.. note:: Freemarker does have a Rhino compatibility extension, but I haven't
    been able to get it to work from within a Rhino script.  Updates as I make
    progress.

Finally, we take advantage of the Freemarker extension to Restlet to render the
output::
    
    response.setEntity(new TemplateRepresentation(
        "hello.ftl", config,map, MediaType.TEXT_HTML
    ));

This will render a template from :file:`{DATA_DIR}/scripts/templates/` when the
script is called.  Here is an example Freemarker template to go along with the
script::

    <html>
        <head>
            <title>A Test Page Via JavaScript and Restlet </title>
        </head>
        <body>
            <h1>My Message For You Is:</h1>
            <strong>${message}</strong>, sent on ${date?date}.
        </body>
    </html>
