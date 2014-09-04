/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.rest;

import org.geoserver.catalog.rest.AbstractCatalogResource;
import org.geoserver.catalog.rest.CatalogFreemarkerHTMLFormat;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.rest.format.DataFormat;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;

import freemarker.template.Configuration;

/**
 * 
 * @author Juan Marin, OpenGeo
 * 
 */
public class GlobalContactResource extends AbstractCatalogResource {

    public GlobalContactResource(Context context, Request request, Response response, Class clazz,
            GeoServer geoServer) {
        super(context, request, response, clazz, geoServer.getCatalog());
    }

    @Override
    protected DataFormat createHTMLFormat(Request request, Response response) {
        return new GlobalContactHTMLFormat(request, response, this);
    }

    @Override
    public boolean allowPut() {
        return allowExisting();
    }

    private boolean allowExisting() {
        return geoServer.getGlobal().getSettings().getContact() != null;
    }

    @Override
    protected Object handleObjectGet() throws Exception {
        if (geoServer.getSettings().getContact() == null) {
            throw new RestletException("No contact information available",
                    Status.CLIENT_ERROR_NOT_FOUND);
        }
        return geoServer.getGlobal().getSettings().getContact();
    }

    @Override
    protected void handleObjectPut(Object obj) throws Exception {
        GeoServerInfo geoServerInfo = geoServer.getGlobal();
        ContactInfo original = geoServerInfo.getSettings().getContact();
        OwsUtils.copy((ContactInfo) obj, original, ContactInfo.class);
        geoServer.save(geoServerInfo);
    }

    @Override
    protected void configurePersister(XStreamPersister persister, DataFormat format) {
        persister.setHideFeatureTypeAttributes();
        persister.getXStream().alias("contact", ContactInfo.class);
    }

    static class GlobalContactHTMLFormat extends CatalogFreemarkerHTMLFormat {

        public GlobalContactHTMLFormat(Request request, Response response, Resource resource) {
            super(ContactInfo.class, request, response, resource);
        }

        @Override
        protected Configuration createConfiguration(Object data, Class clazz) {
            Configuration cfg = super.createConfiguration(data, clazz);
            cfg.setClassForTemplateLoading(getClass(), "templates");
            return cfg;
        }
    }
}
