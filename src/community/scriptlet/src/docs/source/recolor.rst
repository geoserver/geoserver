Recoloring An Image
-------------------
JavaScript is generally not considered as a choice for manipulating binary data
such as raster graphics.  However, when running outside of the browser, that
sort of barrier is no longer a problem.  In this tutorial we'll create a script
that lets you recolor an image to generate, for example, colored badges to put
on a map.  The script will read in a black-and-white image and replace the black
parts with a user-provided "foreground" color, and the white parts with a
user-provided "background" color.

First, let's set up some imports, define a method for parsing RGB strings, and
read some query parameters.  (This should look familiar from :doc:`colorramp`.)
The code looks like::

    var MediaType = Packages.org.restlet.data.MediaType;
    var Status = Packages.org.restlet.data.Status;
    var FileRepresentation = Packages.org.restlet.resource.FileRepresentation;
    var StringRepresentation = Packages.org.restlet.resource.StringRepresentation;
    var JavaScriptStreamRepresentation = 
        Packages.org.geoserver.rest.javascript.JavaScriptStreamRepresentation;

    function parseColor(str) {
        str = str + '';
        str = str.replace(/^(0x|#)*/, '');
        return parseInt(str, 16) | 0xFF000000;
    }

    var form = request.getResourceRef().getQueryAsForm();
    form.addAll(request.getEntityAsForm());

    var fg = form.getFirstValue("fg") || "0xFF0000" ;
    fg = parseColor(fg);
    var bg = form.getFirstValue("bg") || "0x0000FF";
    bg = parseColor(bg);
    var stencil = form.getFirstValue("stencil");
    var path = loader.find("scripts/stencils/");
    var img = new java.io.File(path, stencil + ".png");

Now, let's check that the image exists and is not actually some hacker's attempt
at reading the GeoServer user database::

    if (!(img.getParentFile().equals(path) && img.exists())) {
        response.setEntity(new StringRepresentation(
            "Could not find stencil: " + stencil, MediaType.TEXT_PLAIN)
        );
        response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);

Finally, we can read in the image and do the color replacement::

    ImageIO = Packages.javax.imageio.ImageIO;
    img = ImageIO.read(img);

    for (var x = 0; x < img.getWidth(); x++) {
        for (var y = 0; y < img.getHeight(); y++) {
            var rgb = img.getRGB(x, y) & 0x00FFFFFF;
            if (rgb == 0xFFFFFF) {
                img.setRGB(x, y, bg);
            } else if (rgb == 0x000000) {
                img.setRGB(x, y, fg);
            } else { }
        }
    }

    response.setEntity(new JavaScriptStreamRepresentation({
        getMediaType: function() {
            return MediaType.IMAGE_PNG;
        },

        write: function(out) {
            ImageIO.write(img, "PNG", out);
            out.flush();
            out.close();
        }
    }));

One particular point to note here is that we're using a special type of
Representation object, the
org.geoserver.rest.javascript.JavaScriptStreamRepresentation.  While Rhino does
allow for subclassing of Java objects and interfaces, it requires a default
constructor (or in the case of interfaces, no constructor) in order to do so.
The JavaScriptStreamRepresentation is literally a simple subclass of Restlet's
StreamRepresentation class that adds a default constructor.  You should always
use it when outputting binary data from scriptlet scripts, but make sure to
override getMediaType() when you do.  Trying to pass a MediaType to the
constructor will fail.
does allow for subclassing 
