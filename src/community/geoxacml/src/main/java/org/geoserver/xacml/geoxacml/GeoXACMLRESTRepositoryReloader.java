/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.xacml.geoxacml;

import java.util.ArrayList;
import java.util.List;

import org.geoserver.rest.AbstractResource;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.StringFormat;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;

/**
 * Rest Service for reloading the GeoXACML repository
 * 
 * @author Christian Mueller
 * 
 */
public class GeoXACMLRESTRepositoryReloader extends AbstractResource {

    public final static String ReloadedMsg = "<info>GeoXACML repository reloaded</info>";

    @Override
    protected List<DataFormat> createSupportedFormats(Request request, Response response) {

        List<DataFormat> formats = new ArrayList<DataFormat>();
        // formats.add(new StringFormat( MediaType.TEXT_PLAIN ));
        formats.add(new StringFormat(MediaType.TEXT_XML));
        return formats;

    }

    @Override
    public void handleGet() {

        GeoXACMLConfig.reload();

        // get the appropriate format
        DataFormat format = getFormatGet();

        // transform the string "Hello World" to the appropriate response
        getResponse().setEntity(format.toRepresentation(ReloadedMsg));
    }

}
