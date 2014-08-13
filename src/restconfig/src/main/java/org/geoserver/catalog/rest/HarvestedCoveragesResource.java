/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.ReflectiveXMLFormat;
import org.opengis.coverage.grid.GridCoverageReader;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;

import com.thoughtworks.xstream.XStream;

/**
 * A {@link CatalogResource} representing new coverages which have been added through 
 * harvesting but which haven't been configured yet. 
 * 
 * @author Daniele Romagnoli, GeoSolutions SAS
 *
 */
public class HarvestedCoveragesResource extends AbstractCatalogResource {

    public HarvestedCoveragesResource(Context context, Request request,
            Response response, Catalog catalog) {
        super(context, request, response, CoverageInfo.class, catalog);
        
    }

    @Override
    protected DataFormat createHTMLFormat(Request request, Response response) {
        return new ResourceHTMLFormat(CoverageInfo.class, request, response, this);
    }

    @Override
    protected Object handleObjectGet() throws Exception {
        String workspace = getAttribute( "workspace");
        String coveragestore = getAttribute( "coveragestore");
        String coverage = getAttribute( "coverage" );

        LOGGER.fine( "GET coverage " + coveragestore + "," + coverage );
        final CoverageStoreInfo cs = catalog.getCoverageStoreByName(workspace, coveragestore);
        final GridCoverageReader reader = cs.getGridCoverageReader(null, null);
        final String[] coverageNames = reader.getGridCoverageNames();
        final List<String> coverages = new ArrayList<String>();
        for (String name: coverageNames) {
            coverages.add(name);
        }
        return coverages;
    }

    @Override
    protected ReflectiveXMLFormat createXMLFormat(Request request, Response response) {
        return new ReflectiveXMLFormat() {
          
            @Override
            protected void write(Object data, OutputStream output)
                    throws IOException {
                XStream xstream = new XStream();
                xstream.alias( "coverageName", String.class);
                xstream.toXML( data, output );
            }
        };
    }

    void clear(CoverageInfo info) {
        catalog.getResourcePool().clear(info.getStore());
    }

}
