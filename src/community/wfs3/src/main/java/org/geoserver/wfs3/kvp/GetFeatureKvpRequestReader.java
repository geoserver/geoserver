/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3.kvp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import org.eclipse.emf.ecore.EObject;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.wfs3.GetFeatureType;
import org.geotools.util.DateRange;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.identity.FeatureId;
import org.opengis.filter.spatial.BBOX;
import org.opengis.geometry.Envelope;

public class GetFeatureKvpRequestReader extends org.geoserver.wfs.kvp.GetFeatureKvpRequestReader {

    public GetFeatureKvpRequestReader(GeoServer geoServer, FilterFactory filterFactory) {
        super(GetFeatureType.class, null, geoServer, filterFactory);
    }

    @Override
    public Object read(Object request, Map kvp, Map rawKvp) throws Exception {
        GetFeatureType gf = (GetFeatureType) super.read(request, kvp, rawKvp);
        Filter filter = getFullFilter(kvp);
        querySet(gf, "filter", Collections.singletonList(filter));
        // reset the default, we need to do negotiation, there is generic code to do that
        // inside WFS3DispatcherCallback
        if (!kvp.containsKey("outputFormat")) {
            gf.setOutputFormat(null);
        }
        return gf;
    }

    @Override
    public Object createRequest() {
        return new org.geoserver.wfs3.GetFeatureType();
    }

    /**
     * Finds all filter expressions and combines them in "AND"
     *
     * @param kvp
     * @return
     */
    private Filter getFullFilter(Map kvp) throws IOException {
        List<Filter> filters = new ArrayList<>();
        // check the various filters, considering that only one feature type at a time can be
        // used in WFS3
        if (kvp.containsKey("filter")) {
            List<Filter> list = (List) kvp.get("filter");
            filters.add(list.get(0));
        }
        if (kvp.containsKey("cql_filter")) {
            List<Filter> list = (List) kvp.get("cql_filter");
            filters.add(list.get(0));
        }
        if (kvp.containsKey("featureId") || kvp.containsKey("resourceId")) {
            List<String> featureIdList = (List) kvp.get("featureId");
            Set<FeatureId> ids =
                    featureIdList
                            .stream()
                            .map(id -> filterFactory.featureId(id))
                            .collect(Collectors.toSet());
            filters.add(filterFactory.id(ids));
        }
        if (kvp.containsKey("bbox")) {
            Envelope bbox = (Envelope) kvp.get("bbox");
            BBOX bboxFilter = bboxFilter((org.locationtech.jts.geom.Envelope) bbox);
            filters.add(bboxFilter);
        }
        if (kvp.containsKey("time")) {
            Object timeSpecification = kvp.get("time");
            QName typeName = (QName) ((List) ((List) kvp.get("typeName")).get(0)).get(0);
            List<String> timeProperties = getTimeProperties(typeName);
            Filter filter = buildTimeFilter(timeSpecification, timeProperties);
            filters.add(filter);
        }
        return mergeFiltersAnd(filters);
    }

    private Filter buildTimeFilter(Object timeSpec, List<String> timeProperties) {
        List<Filter> filters = new ArrayList<>();
        for (String timeProperty : timeProperties) {
            PropertyName property = filterFactory.property(timeProperty);
            Filter filter;
            if (timeSpec instanceof Date) {
                filter = filterFactory.equals(property, filterFactory.literal(timeSpec));
            } else if (timeSpec instanceof DateRange) {
                Literal before = filterFactory.literal(((DateRange) timeSpec).getMinValue());
                Literal after = filterFactory.literal(((DateRange) timeSpec).getMaxValue());
                filter = filterFactory.between(property, before, after);
            } else {
                throw new IllegalArgumentException("Cannot build time filter out of " + timeSpec);
            }

            filters.add(filter);
        }

        return mergeFiltersOr(filters);
    }

    private Filter mergeFiltersAnd(List<Filter> filters) {
        if (filters.isEmpty()) {
            return Filter.INCLUDE;
        } else if (filters.size() == 1) {
            return filters.get(0);
        } else {
            return filterFactory.and(filters);
        }
    }

    private Filter mergeFiltersOr(List<Filter> filters) {
        if (filters.isEmpty()) {
            return Filter.EXCLUDE;
        } else if (filters.size() == 1) {
            return filters.get(0);
        } else {
            return filterFactory.or(filters);
        }
    }

    private List<String> getTimeProperties(QName typeName) throws IOException {
        Catalog catalog = geoServer.getCatalog();
        NamespaceInfo ns = catalog.getNamespaceByURI(typeName.getNamespaceURI());
        FeatureTypeInfo ft = catalog.getFeatureTypeByName(ns, typeName.getLocalPart());
        if (ft == null) {
            return Collections.emptyList();
        }
        FeatureType schema = ft.getFeatureType();
        return schema.getDescriptors()
                .stream()
                .filter(pd -> Date.class.isAssignableFrom(pd.getType().getBinding()))
                .map(pd -> pd.getName().getLocalPart())
                .collect(Collectors.toList());
    }

    /**
     * In WFS3 it's possible to have multiple filter KVPs, and they have to be combined in AND
     *
     * @param kvp
     * @param keys
     * @param request
     */
    @Override
    protected void ensureMutuallyExclusive(Map kvp, String[] keys, EObject request) {
        // no op, we actually want to handle multiple filters
    }
}
