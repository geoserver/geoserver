/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.catalog.rest;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.opengis.filter.Filter;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

/**
 * Returns a single granule in a particular coverage of a structured grid reader
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 */
public class GranuleResource extends AbstractGranuleResource {

    private String granuleId;
    private String format = "xml";

    public GranuleResource(Context context, Request request, Response response, Catalog catalog,
            CoverageInfo coverage, String granuleId) {
        super(context, request, response, catalog, coverage);
        // here we have isseus with the . appearing in the feature ids, which makes it 
        // impossible for restlet to extract the format parameter automatically
        if(granuleId.endsWith(".xml")) {
            format = "xml";
            granuleId = granuleId.substring(0, granuleId.length() - 4);
        } else if(granuleId.endsWith(".json")) {
            format = "json";
            granuleId = granuleId.substring(0, granuleId.length() - 5);
        }
        this.granuleId = granuleId;
    }
    
    @Override
    protected DataFormat getFormatGet() {
        if("xml".equals(format)) {
            return new FeaturesGMLFormat();  
        } else if("json".equals(format)) {
            return new FeaturesJSONFormat();
        } else {
            throw new IllegalArgumentException("Only supported formats are " +
            		"xml and json, this one is not supported: " + format);
        }
    }
    
    @Override
    protected Object handleObjectGet() throws Exception {
        SimpleFeatureCollection fc = (SimpleFeatureCollection) super.handleObjectGet();
        if(fc.size() == 0) {
            throw new RestletException("Could not find a granule with id " + granuleId + " in coveage " + coverage.getPrefixedName(), Status.CLIENT_ERROR_NOT_FOUND);
        }
        return fc;
    }


    @Override
    protected Query getResourceQuery() {
        Filter filter = FF.id(FF.featureId(granuleId));
        return new Query(null, filter);
    }
}
