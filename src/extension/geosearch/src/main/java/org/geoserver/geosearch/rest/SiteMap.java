/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geosearch.rest;

import static org.geoserver.rest.util.RESTUtils.getBaseURL;

import java.util.ArrayList;
import java.util.List;

import org.geoserver.catalog.Catalog;
import org.geoserver.rest.AbstractResource;
import org.geoserver.rest.format.DataFormat;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;

/**
 * A REST resource that uses a {@link SiteMapXMLFormat} to produce the {@code sitemap.xml} document.
 * 
 * @author groldan
 * @see SiteMapXMLFormat
 */
public class SiteMap extends AbstractResource {

    private final Catalog catalog;

    public SiteMap(final Context context, final Request request, final Response response,
            final Catalog catalog) {

        super(context, request, response);

        this.catalog = catalog;
    }

    @Override
    public void handleGet() {
        DataFormat format = getFormatGet();
        getResponse().setEntity(format.toRepresentation(catalog));
    }

    /**
     * @see org.geoserver.rest.AbstractResource#createSupportedFormats(org.restlet.data.Request,
     *      org.restlet.data.Response)
     */
    @Override
    protected List<DataFormat> createSupportedFormats(final Request request, final Response response) {
        final List<DataFormat> siteMapFormats = new ArrayList<DataFormat>(1);
        final String baseURL = getBaseURL(request);
        siteMapFormats.add(new SiteMapXMLFormat(baseURL));
        return siteMapFormats;
    }

}
