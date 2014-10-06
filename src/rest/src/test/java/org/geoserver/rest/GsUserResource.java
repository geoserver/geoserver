/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import org.geotools.factory.CommonFactoryFinder;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Function;
import org.restlet.data.MediaType;
import org.restlet.resource.Resource;

public class GsUserResource extends Resource {

    @Override
    public void handleGet() {

        FilterFactory ff = CommonFactoryFinder.getFilterFactory();
        Function function = ff.function("env", ff.literal("GSUSER"), ff.literal("USER_NOT_FOUND"));
        String result = function.evaluate(null, String.class);

        getResponse().setEntity(result, MediaType.TEXT_PLAIN);
    }
}
