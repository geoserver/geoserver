/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import net.opengis.wfs20.CreateStoredQueryResponseType;
import net.opengis.wfs20.CreateStoredQueryType;
import net.opengis.wfs20.QueryExpressionTextType;
import net.opengis.wfs20.StoredQueryDescriptionType;
import net.opengis.wfs20.Wfs20Factory;
import org.geoserver.platform.ServiceException;

/**
 * Web Feature Service CreateStoredQuery operation.
 *
 * @author Justin Deoliveira, OpenGeo
 * @version $Id$
 */
public class CreateStoredQuery {

    /** The default create stored query language, according to spec */
    public static final String DEFAULT_LANGUAGE =
            "urn:ogc:def:queryLanguage:OGC-WFS::WFSQueryExpression";

    /** service config */
    WFSInfo wfs;

    /** stored query provider */
    StoredQueryProvider storedQueryProvider;

    public CreateStoredQuery(WFSInfo wfs, StoredQueryProvider storedQueryProvider) {
        this.wfs = wfs;
        this.storedQueryProvider = storedQueryProvider;
    }

    public CreateStoredQueryResponseType run(CreateStoredQueryType request) throws WFSException {
        for (StoredQueryDescriptionType sqd : request.getStoredQueryDefinition()) {
            if (storedQueryProvider.getStoredQuery(sqd.getId()) != null) {
                WFSException e =
                        new WFSException(
                                request,
                                "Stored query already exists",
                                WFSException.DUPLICATE_STORED_QUERY_ID_VALUE);
                e.setLocator(sqd.getId());
                throw e;
            }

            validateStoredQuery(request, sqd);

            try {
                storedQueryProvider.createStoredQuery(sqd);
            } catch (Exception e) {
                throw new WFSException(request, "Error occured creating stored query", e);
            }
        }

        Wfs20Factory factory = Wfs20Factory.eINSTANCE;
        CreateStoredQueryResponseType response = factory.createCreateStoredQueryResponseType();
        response.setStatus("OK");
        return response;
    }

    void validateStoredQuery(CreateStoredQueryType request, StoredQueryDescriptionType sq)
            throws WFSException {
        if (sq.getQueryExpressionText().isEmpty()) {
            throw new WFSException(request, "Stored query does not specify any queries");
        }

        // check for supported language
        for (QueryExpressionTextType queryExpressionTextType : sq.getQueryExpressionText()) {
            String language = queryExpressionTextType.getLanguage();
            if (language != null && !storedQueryProvider.supportsLanguage(language)) {
                WFSException e =
                        new WFSException(
                                request,
                                "Invalid language " + queryExpressionTextType.getLanguage(),
                                ServiceException.INVALID_PARAMETER_VALUE);
                e.setLocator("language");
                throw e;
            }
        }

        // check for multiple languages
        String language = sq.getQueryExpressionText().get(0).getLanguage();
        for (int i = 1; i < sq.getQueryExpressionText().size(); i++) {
            if (!language.equals(sq.getQueryExpressionText().get(i).getLanguage())) {
                throw new WFSException(
                        request,
                        "Stored query specifies queries with multiple languages. "
                                + "Not supported");
            }
        }

        try {
            storedQueryProvider.createStoredQuery(sq, false).validate();
        } catch (WFSException e) {
            throw new WFSException(request, e.getMessage(), e, e.getCode());
        } catch (Exception e) {
            throw new WFSException(request, "Error validating stored query", e);
        }
    }
}
