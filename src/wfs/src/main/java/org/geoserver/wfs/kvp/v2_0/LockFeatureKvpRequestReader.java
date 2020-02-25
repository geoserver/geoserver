/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.kvp.v2_0;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.opengis.wfs20.LockFeatureType;
import net.opengis.wfs20.ParameterExpressionType;
import net.opengis.wfs20.ParameterType;
import net.opengis.wfs20.StoredQueryType;
import net.opengis.wfs20.Wfs20Factory;
import org.eclipse.emf.ecore.EObject;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.StoredQuery;
import org.geoserver.wfs.StoredQueryProvider;
import org.geoserver.wfs.WFSException;
import org.geoserver.wfs.kvp.BaseFeatureKvpRequestReader;
import org.geoserver.wfs.request.LockFeatureRequest;
import org.geoserver.wfs.request.Query;
import org.geotools.xsd.EMFUtils;
import org.opengis.filter.FilterFactory;

public class LockFeatureKvpRequestReader extends BaseFeatureKvpRequestReader {

    public LockFeatureKvpRequestReader(GeoServer geoServer, FilterFactory filterFactory) {
        super(LockFeatureType.class, Wfs20Factory.eINSTANCE, geoServer, filterFactory);
    }

    protected void querySet(EObject request, String property, List values) throws WFSException {
        // no values specified, do nothing
        if (values == null) {
            return;
        }

        LockFeatureRequest req = LockFeatureRequest.adapt(request);

        // this is a 2.0 only parser
        if ("typeName".equals(property)) {
            property = "typeNames";
        }

        List query = req.getAdaptedQueries();

        int m = values.size();
        int n = query.size();

        if ((m == 1) && (n > 1)) {
            // apply single value to all queries
            EMFUtils.set(query, property, values.get(0));

            return;
        }

        // WfsFactory wfsFactory = (WfsFactory) getFactory();
        // match up sizes
        if (m > n) {
            if (n == 0) {
                // make same size, with empty objects
                for (int i = 0; i < m; i++) {
                    query.add(req.createQuery().getAdaptee());
                }
            } else if (n == 1) {
                // clone single object up to
                EObject q = (EObject) query.get(0);

                for (int i = 1; i < m; i++) {
                    query.add(EMFUtils.clone(q, req.getFactory(), false));
                }

                return;
            } else {
                // illegal
                String msg = "Specified " + m + " " + property + " for " + n + " queries.";
                throw new WFSException(request, msg);
            }
        }
        if (m < n) {
            // fill the rest with nulls
            List newValues = new ArrayList<>();
            newValues.addAll(values);
            for (int i = 0; i < n - m; i++) {
                newValues.add(null);
            }
            values = newValues;
        }

        EMFUtils.set(query, property, values);
    }

    protected void buildStoredQueries(EObject request, List<URI> storedQueryIds, Map kvp) {
        LockFeatureRequest req = LockFeatureRequest.adapt(request);

        if (!(req instanceof LockFeatureRequest.WFS20)) {
            throw new WFSException(req, "Stored queries only supported in WFS 2.0+");
        }

        StoredQueryProvider sqp =
                new StoredQueryProvider(
                        catalog,
                        getWFS(),
                        geoServer.getGlobal().isAllowStoredQueriesPerWorkspace());
        for (URI storedQueryId : storedQueryIds) {
            StoredQuery sq = sqp.getStoredQuery(storedQueryId.toString());
            if (sq == null) {
                WFSException exception =
                        new WFSException(
                                req,
                                "No such stored query: " + storedQueryId,
                                ServiceException.INVALID_PARAMETER_VALUE);
                exception.setLocator("STOREDQUERY_ID");
                throw exception;
            }

            // JD: since stored queries are 2.0 only we will create 2.0 model objects directly...
            // once
            // the next version of wfs comes out (and if they keep stored queries around) we will
            // have
            // to abstract stored query away with a request object adapter
            Wfs20Factory factory = (Wfs20Factory) req.getFactory();
            StoredQueryType storedQuery = factory.createStoredQueryType();
            storedQuery.setId(storedQueryId.toString());

            // look for parameters in the kvp map
            for (ParameterExpressionType p : sq.getQuery().getParameter()) {
                if (kvp.containsKey(p.getName())) {
                    ParameterType param = factory.createParameterType();
                    param.setName(p.getName());
                    param.setValue(kvp.get(p.getName()).toString());
                    storedQuery.getParameter().add(param);
                }
            }

            req.getAdaptedQueries().add(storedQuery);
        }
    }

    protected List<Query> getQueries(EObject eObject) {
        return LockFeatureRequest.adapt(eObject).getQueries();
    }
}
