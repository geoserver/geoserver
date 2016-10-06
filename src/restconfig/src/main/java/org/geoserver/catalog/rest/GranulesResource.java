/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.catalog.rest;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.rest.RestletException;
import org.geotools.data.Query;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.opengis.filter.Filter;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

/**
 * Returns a filterable/pageable view of the granules in a particular coverage of a structured grid
 * reader
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 */
public class GranulesResource extends AbstractGranuleResource {

    public GranulesResource(Context context, Request request, Response response, Catalog catalog,
            CoverageInfo coverage) {
        super(context, request, response, catalog, coverage);
    }

    @Override
    protected Query getResourceQuery() {

        // build the query
        Query q = new Query(Query.ALL);

        // ... filter
        Form form = getRequest().getResourceRef().getQueryAsForm();
        Filter filter = parseFilter(form);
        if (filter != null) {
            q.setFilter(filter);
        }

        // ... offset
        Integer offset = getNonNegativeVariable(form, "offset", true);
        q.setStartIndex(offset);

        // ... limit
        Integer limit = getNonNegativeVariable(form, "limit", true);
        if (limit != null) {
            q.setMaxFeatures(limit);
        }

        return q;
    }

    private Filter parseFilter(Form form) {
        String cql = form.getFirstValue("filter");
        if (cql != null) {
            try {
                return ECQL.toFilter(cql);
            } catch (CQLException e) {
                throw new RestletException("Invalid cql syntax: " + e.getMessage(),
                        Status.CLIENT_ERROR_BAD_REQUEST);
            }
        }

        return null;
    }

    private Integer getNonNegativeVariable(Form form, String variable, boolean allowZero) {
        String offset = form.getFirstValue(variable);
        if (offset != null) {
            try {
                int value = Integer.parseInt(offset);
                if (value < 0 || (!allowZero && value == 0)) {
                    throw new RestletException("Invalid " + variable + " value, : " + value,
                            Status.CLIENT_ERROR_BAD_REQUEST);
                }
                return value;
            } catch (NumberFormatException e) {
                throw new RestletException("Invalid " + variable
                        + " value, must be a positive integer: " + e.getMessage(),
                        Status.CLIENT_ERROR_BAD_REQUEST);
            }
        }

        return null;
    }

}
