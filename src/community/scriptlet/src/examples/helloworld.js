var StringRepresentation = Packages.org.restlet.resource.StringRepresentation;
var MediaType = Packages.org.restlet.data.MediaType;

response.setEntity(new StringRepresentation(
    "Hello world!", MediaType.TEXT_PLAIN
));
