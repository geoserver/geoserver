/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.kvp;

import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.opengis.wfs.WfsFactory;
import net.opengis.wfs20.ParameterExpressionType;
import net.opengis.wfs20.ParameterType;
import net.opengis.wfs20.StoredQueryType;
import net.opengis.wfs20.Wfs20Factory;
import org.eclipse.emf.ecore.EFactory;
import org.eclipse.emf.ecore.EObject;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.ows.util.NumericKvpParser;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.StoredQuery;
import org.geoserver.wfs.StoredQueryProvider;
import org.geoserver.wfs.WFSException;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geotools.xsd.EMFUtils;
import org.opengis.filter.FilterFactory;

/** @author Niels Charlier : added 3D BBOX support */
public class GetFeatureKvpRequestReader extends BaseFeatureKvpRequestReader {

    public GetFeatureKvpRequestReader(
            Class requestBean, GeoServer geoServer, FilterFactory filterFactory) {
        this(requestBean, WfsFactory.eINSTANCE, geoServer, filterFactory);
    }

    public GetFeatureKvpRequestReader(
            Class requestBean, EFactory factory, GeoServer geoServer, FilterFactory filterFactory) {
        super(requestBean, factory, geoServer, filterFactory);
    }

    protected WFSInfo getWFS() {
        return geoServer.getService(WFSInfo.class);
    }

    /** Performs additional GetFeature/GetFeatureWithLock kvp parsing requirements */
    public Object read(Object request, Map kvp, Map rawKvp) throws Exception {
        // hack but startIndex conflicts with WMS startIndex... which parses to different type, so
        // we just parse manually
        if (rawKvp.containsKey("startIndex")) {
            kvp.put(
                    "startIndex",
                    new NumericKvpParser(null, BigInteger.class)
                            .parse((String) rawKvp.get("startIndex")));
        }

        request = super.read(request, kvp, rawKvp);

        // get feature has some additional parsing requirements
        EObject eObject = (EObject) request;

        // outputFormat
        if (!EMFUtils.isSet(eObject, "outputFormat")) {
            // set the default
            String version = (String) EMFUtils.get(eObject, "version");
            switch (WFSInfo.Version.negotiate(version)) {
                case V_10:
                    EMFUtils.set(eObject, "outputFormat", "GML2");
                    break;
                case V_11:
                    EMFUtils.set(eObject, "outputFormat", "text/xml; subtype=gml/3.1.1");
                    break;
                case V_20:
                default:
                    EMFUtils.set(eObject, "outputFormat", "application/gml+xml; version=3.2");
            }
        }

        // aliases
        if (kvp.containsKey("aliases")) {
            querySet(eObject, "aliases", (List) kvp.get("aliases"));
        }

        // propertyName
        if (kvp.containsKey("propertyName")) {
            List<String> propertyNames = new ArrayList<String>();
            if (kvp.get("propertyName") != null && kvp.get("propertyName") instanceof List) {
                propertyNames = (List) kvp.get("propertyName");
            } else if (kvp.get("propertyName") != null
                    && kvp.get("propertyName") instanceof String) {
                propertyNames.addAll(KvpUtils.readFlat((String) kvp.get("propertyName")));
            }
            querySet(eObject, "propertyName", propertyNames);
        }

        // sortBy
        if (kvp.containsKey("sortBy")) {
            querySet(eObject, "sortBy", (List) kvp.get("sortBy"));
        }

        // srsName
        if (kvp.containsKey("srsName")) {
            querySet(eObject, "srsName", Collections.singletonList((URI) kvp.get("srsName")));
        }

        // featureversion
        if (kvp.containsKey("featureVersion")) {
            querySet(
                    eObject,
                    "featureVersion",
                    Collections.singletonList((String) kvp.get("featureVersion")));
        }

        GetFeatureRequest req = GetFeatureRequest.adapt(request);
        if (kvp.containsKey("format_options")) {
            req.getFormatOptions().putAll((Map) kvp.get("format_options"));
        }

        // sql view params
        if (kvp.containsKey("viewParams")) {

            if (req.getViewParams() == null) {
                req.setViewParams(new ArrayList<Map<String, String>>());
            }

            // fan out over all layers if necessary
            List<Map<String, String>> viewParams =
                    (List<Map<String, String>>) kvp.get("viewParams");
            if (viewParams.size() > 0) {
                int layerCount = req.getQueries().size();

                // if we have just one replicate over all layers
                if (viewParams.size() == 1 && layerCount > 1) {
                    List<Map<String, String>> replacement = new ArrayList<Map<String, String>>();
                    for (int i = 0; i < layerCount; i++) {
                        replacement.add(viewParams.get(0));
                    }
                    viewParams = replacement;
                } else if (viewParams.size() != layerCount) {
                    String msg =
                            layerCount
                                    + " feature types requested, but found "
                                    + viewParams.size()
                                    + " view params specified. ";
                    throw new WFSException(eObject, msg, getClass().getName());
                }
            }

            req.setViewParams(viewParams);
        }

        return request;
    }

    protected void querySet(EObject request, String property, List values) throws WFSException {
        // no values specified, do nothing
        if (values == null) {
            return;
        }

        GetFeatureRequest req = GetFeatureRequest.adapt(request);

        // handle the name differences in property names between 1.1 and 2.0
        if (req instanceof GetFeatureRequest.WFS20) {
            if ("typeName".equals(property)) {
                property = "typeNames";
            }
            if ("propertyName".equals(property)) {
                property = "abstractProjectionClause";
            }
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
        GetFeatureRequest req = GetFeatureRequest.adapt(request);

        if (!(req instanceof GetFeatureRequest.WFS20)) {
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
}
