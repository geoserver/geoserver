package org.geoserver.rest;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.kml.icons.IconPropertyInjector;
import org.geoserver.kml.icons.IconRenderer;
import org.geotools.styling.Style;
import org.restlet.Finder;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.OutputRepresentation;
import org.restlet.resource.Resource;

public class KMLIconFinder extends Finder {
    private final Catalog catalog;
   
    public KMLIconFinder(Catalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public Resource findTarget(Request request, Response response) {
        Map<String, Object> attributes = request.getAttributes();
        String workspace = (String) attributes.get("workspace");
        String styleName = (String) attributes.get("style");
        StyleInfo styleInfo = catalog.getStyleByName(workspace, styleName);
        try {
            Style style = styleInfo.getStyle();
            Map<String, String> properties = asSimpleMap(request.getResourceRef().getQueryAsForm());
            Style adjustedStyle = IconPropertyInjector.injectProperties(style, properties);
            Resource resource = new Resource(getContext(), request, response);
            resource.getVariants().add(new IconEntity(adjustedStyle));
            return resource;
        } catch (IOException e) {
            throw new RestletException("Failed to load style: " + workspace + " " + styleName, Status.SERVER_ERROR_INTERNAL, e);
        }
    }
    
    private final Map<String, String> asSimpleMap(Form form) {
        Map<String, String> result = new HashMap<String, String>();
        for (String name : form.getNames()) 
            result.put(name, form.getFirstValue(name));
        return result;
    }
    
    private static final class IconEntity extends OutputRepresentation {
        private Style style;
        
        public IconEntity(Style style) {
            super(MediaType.IMAGE_PNG);
            this.style = style;
        }

        @Override
        public void write(OutputStream outputStream) throws IOException {
            BufferedImage image = IconRenderer.renderIcon(style);
            ImageIO.write(image, "PNG", outputStream);
        }
        
    }
}
