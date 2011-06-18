/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.sfs;

import org.geoserver.catalog.FeatureTypeInfo;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;

/**
 * The Simple Feature Service layer description document
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class DescribeResource extends Resource {
    FeatureTypeInfo featureType;

    public DescribeResource(Context context, Request request, Response response,
            FeatureTypeInfo featureType) {
        super(context, request, response);
        this.featureType = featureType;
    }

    @Override
    public void handleGet() {
        Representation represeentation = new DescribeJSONFormat().toRepresentation(featureType);
        getResponse().setEntity(represeentation);
    }

}
