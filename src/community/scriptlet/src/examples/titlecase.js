/**
 * titlecase.js: Ensure that all layer titles in the GeoServer catalog are in
 *    title case.
 */

StringRepresentation = Packages.org.restlet.resource.StringRepresentation;
MediaType = Packages.org.restlet.data.MediaType;

function toTitleCase(str) {
    return (""+str).replace(/(:?^|\s)\w/g, function(x) {
        return x.toUpperCase();
    });
}

var iter = catalog.getLayers().iterator();
var report = "Updated: ";

while (iter.hasNext()) {
    var layer = iter.next();
    var resource = layer.getResource();
    var title = resource.getTitle();
    report += "\n " + title;
    title = toTitleCase(title);
    resource.setTitle(title);
    catalog.save(resource);
    report += " -> " + title;
}

response.setEntity(new StringRepresentation(
    report, MediaType.TEXT_PLAIN
));
