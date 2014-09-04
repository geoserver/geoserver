/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xslt.rest;

import java.util.Collection;
import java.util.List;

import org.geoserver.catalog.rest.AbstractCatalogListResource;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.wfs.xslt.config.TransformInfo;
import org.geoserver.wfs.xslt.config.TransformRepository;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;

import com.thoughtworks.xstream.XStream;

public class TransformListResource extends AbstractCatalogListResource {

    private TransformRepository repository;

    public TransformListResource(Context context, Request request, Response response,
            TransformRepository repository) {
        super(context, request, response, TransformInfo.class, null);
        this.repository = repository;
    }
    
    @Override
    protected DataFormat createHTMLFormat(Request request, Response response) {
        return new TransformHTMLDataFormat(clazz, request, response, this);
    }


    @Override
    protected Collection handleListGet() throws Exception {
        List<TransformInfo> result = repository.getAllTransforms();
        return result;
    }

    @Override
    protected void aliasCollection(Object data, XStream xstream) {
        xstream.alias("transforms", Collection.class, data.getClass());
    }
    
    @Override
    protected String getItemName(XStreamPersister xp) {
        return "transform";
    }
    
}
