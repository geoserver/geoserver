/**
 * freemarker.js: Demonstrate rendering values from JavaScript scripts into 
 * Freemarker templates.
 */

var TemplateRepresentation = 
    Packages.org.restlet.ext.freemarker.TemplateRepresentation;
var MediaType = Packages.org.restlet.data.MediaType;

var config = new Packages.freemarker.template.Configuration();
config.setDirectoryForTemplateLoading(loader.find("scripts/templates"));

var context = {"message": "hello friends", "date": new java.util.Date()};
var map = new java.util.HashMap();
for (var x in context) {
    map.put(x, context[x]);
}

response.setEntity(new TemplateRepresentation(
    "hello.ftl", config,map, MediaType.TEXT_HTML
));

