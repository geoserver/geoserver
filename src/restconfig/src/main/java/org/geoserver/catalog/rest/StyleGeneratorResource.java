/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.catalog.StyleGenerator;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.StyleType;
import org.geoserver.catalog.Styles;
import org.geoserver.rest.AbstractResource;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.geotools.util.Converters;
import org.geotools.util.Version;
import org.geotools.util.logging.Logging;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.StringRepresentation;

public class StyleGeneratorResource extends AbstractResource {

    static Logger LOGGER = Logging.getLogger("org.geoserver.catalog.rest");

    private StyleGenerator styleGen;
    private StyleType styleType;

    public StyleGeneratorResource(Context context, Request request, Response response, 
            StyleGenerator styleGen, StyleType styleType) {
        super(context, request, response);
        this.styleGen = styleGen;
        this.styleType = styleType;
    }
    
    @Override
    protected List<DataFormat> createSupportedFormats(Request request,Response response) {
        List<DataFormat> formats =  new ArrayList<DataFormat>();
        boolean prettyPrint = isPrettyPrint(request);
        for (StyleHandler sh : Styles.handlers()) {
            for (Version ver : sh.getVersions()) {
                formats.add(new StyleFormat(sh.mimeType(ver), ver, prettyPrint, sh, request));
            }
        }

        return formats;
    }
    
    boolean isPrettyPrint(Request request) {
        Form q = request.getResourceRef().getQueryAsForm();
        String pretty = q.getFirstValue("pretty");
        return pretty != null && Boolean.TRUE.equals(Converters.convert(pretty, Boolean.class));
    }
    
    @Override
    public void handleGet() {
        DataFormat df = getFormatGet();
        if (!(df instanceof StyleFormat)) {
            throw new RestletException( "Unable to create style of type "
                    + df.getMediaType(), Status.CLIENT_ERROR_BAD_REQUEST );
            
        }
        
        StyleFormat sf = (StyleFormat)df;
        String style;
        try {
            style = styleGen.generateStyle(sf.getHandler(), styleType, "Default style");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error generating style: " + e.getMessage(), e);
            throw new RestletException( "Error generating style: " + e.getMessage()
                    , Status.SERVER_ERROR_INTERNAL);
        }
        
        getResponse().setEntity(new StringRepresentation(style));
        LOGGER.fine( "GET default "+styleType.toString()+" style");
    }

}
