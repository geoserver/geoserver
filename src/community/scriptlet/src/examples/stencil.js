/**
 * stencil.js: Take an image from a stencils directory and recolor it.
 * Respected query parameters: 
 *     fg     : the hex code for the foreground color (will replace black pixels
 *              in the stencil image
 *     bg     : the hex code for the background color (will replace white pixels
 *              in the stencil image
 *     stencil: the name of the stencil image, without the directory path or
 *              file extension.
 *
 * Stencils are found by looking for {name}.png in the scripts/stencils/
 * directory.
 */

var MediaType = Packages.org.restlet.data.MediaType;
var Status = Packages.org.restlet.data.Status;
var FileRepresentation = Packages.org.restlet.resource.FileRepresentation;
var StringRepresentation = Packages.org.restlet.resource.StringRepresentation;
var JavaScriptStreamRepresentation = Packages.org.geoserver.rest.javascript.JavaScriptStreamRepresentation;

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

if (!(img.getParentFile().equals(path) && img.exists())) {
    response.setEntity(new StringRepresentation("Could not find stencil: " + stencil, MediaType.TEXT_PLAIN));
    response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
} else {
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
}
