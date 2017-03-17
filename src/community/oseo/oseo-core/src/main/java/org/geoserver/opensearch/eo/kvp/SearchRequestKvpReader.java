/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.kvp;

import static org.geoserver.opensearch.eo.OpenSearchParameters.GEO_BOX;
import static org.geoserver.opensearch.eo.OpenSearchParameters.GEO_UID;
import static org.geoserver.opensearch.eo.OpenSearchParameters.SEARCH_TERMS;
import static org.geoserver.opensearch.eo.OpenSearchParameters.START_INDEX;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.geoserver.catalog.Predicates;
import org.geoserver.config.GeoServer;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.geoserver.opensearch.eo.OpenSearchEoService;
import org.geoserver.opensearch.eo.SearchRequest;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geoserver.ows.KvpRequestReader;
import org.geoserver.platform.OWS20Exception;
import org.geoserver.platform.OWS20Exception.OWSExceptionCode;
import org.geoserver.wfs.kvp.BBoxKvpParser;
import org.geotools.data.Parameter;
import org.geotools.data.Query;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.NameImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.Converters;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.MultiValuedFilter.MatchAction;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Reads a "description" request
 *
 * @author Andrea Aime - GeoSolutions
 */
public class SearchRequestKvpReader extends KvpRequestReader {

    static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    public static final String COUNT_KEY = "count";

    public static final String PARENT_ID_KEY = "parentId";

    private Set<String> NOT_FILTERS = new HashSet<>(Arrays.asList(START_INDEX.key, COUNT_KEY));

    private OpenSearchEoService oseo;

    private GeoServer gs;

    public SearchRequestKvpReader(GeoServer gs, OpenSearchEoService service) {
        super(SearchRequest.class);
        this.oseo = service;
        this.gs = gs;
    }

    @Override
    public Object read(Object requestObject, Map kvp, Map rawKvp) throws Exception {
        SearchRequest request = (SearchRequest) super.read(requestObject, kvp, rawKvp);

        // collect the valid search parameters
        Collection<Parameter<?>> parameters = getSearchParameters(request);
        Map<Parameter, String> parameterValues = getSearchParameterValues(rawKvp, parameters);
        request.setSearchParameters(parameterValues);

        // prepare query
        Query query = new Query();
        request.setQuery(query);

        // get filters
        Filter filter = readFilter(rawKvp, parameters);
        query.setFilter(filter);

        // look at paging
        Integer count = getParameter(COUNT_KEY, rawKvp, Integer.class);
        if (count != null) {
            int ic = count.intValue();
            if (ic < 0) {
                throw new OWS20Exception("Invalid 'count' value, should be positive or zero",
                        OWSExceptionCode.InvalidParameterValue);
            }
            int configuredMaxFeatures = getConfiguredMaxFeatures();
            if (ic > configuredMaxFeatures) {
                throw new OWS20Exception("Invalid 'count' value, should not be greater than "
                        + configuredMaxFeatures, OWSExceptionCode.InvalidParameterValue);
            }
            query.setMaxFeatures(ic);
        } else {
            query.setMaxFeatures(getDefaultRecords());
        }
        Integer startIndex = getParameter(START_INDEX.key, rawKvp, Integer.class);
        if (startIndex != null) {
            int is = startIndex.intValue();
            if (is <= 0) {
                throw new OWS20Exception("Invalid 'startIndex' value, should be positive or zero",
                        OWSExceptionCode.InvalidParameterValue);
            }
            query.setStartIndex(is - 1); // OS is 1 based, GeoTools is 0 based
        }

        return request;
    }

    private int getDefaultRecords() {
        OSEOInfo info = gs.getService(OSEOInfo.class);
        if (info == null) {
            return OSEOInfo.DEFAULT_RECORDS_PER_PAGE;
        } else {
            return info.getRecordsPerPage();
        }
    }

    private int getConfiguredMaxFeatures() {
        OSEOInfo info = gs.getService(OSEOInfo.class);
        if (info == null) {
            return OSEOInfo.DEFAULT_MAXIMUM_RECORDS;
        } else {
            return info.getMaximumRecordsPerPage();
        }
    }

    private Map<Parameter, String> getSearchParameterValues(Map rawKvp,
            Collection<Parameter<?>> parameters) {
        Map<Parameter, String> result = new LinkedHashMap<>();
        for (Parameter<?> parameter : parameters) {
            Object value = rawKvp.get(parameter.key);
            if (value != null) {
                final String sv = Converters.convert(value, String.class);
                result.put(parameter, sv);
            }
        }

        return result;
    }

    private Filter readFilter(Map rawKvp, Collection<Parameter<?>> parameters) throws Exception {
        List<Filter> filters = new ArrayList<>();
        for (Parameter<?> parameter : parameters) {
            Object value = rawKvp.get(parameter.key);
            if (value != null && !NOT_FILTERS.contains(parameter.key)) {
                Filter filter;
                if (SEARCH_TERMS.key.equals(parameter.key)) {
                    filter = buildSearchTermsFilter(value);
                } else if (GEO_UID.key.equals(parameter.key)) {
                    filter = buildUidFilter(value);
                } else if (GEO_BOX.key.equals(parameter.key)) {
                    filter = buildBoundingBoxFilter(value);
                } else {
                    filter = buildGenericFilter(parameter, value);
                }
                filters.add(filter);
            }
        }

        Filter filter = Predicates.and(filters);
        return filter;
    }

    private Filter buildBoundingBoxFilter(Object value) throws Exception {
        Filter filter;
        ReferencedEnvelope box = (ReferencedEnvelope) new BBoxKvpParser().parse((String) value);
        final CoordinateReferenceSystem crs = box.getCoordinateReferenceSystem();
        if (crs != null && !CRS.equalsIgnoreMetadata(DefaultGeographicCRS.WGS84, crs)) {
            throw new OWS20Exception(
                    "OpenSearch for EO requests only support boundig boxes in WGS84",
                    OWS20Exception.OWSExceptionCode.InvalidParameterValue, "box");
        }
        filter = FF.bbox(FF.property(""), box, MatchAction.ANY);
        return filter;
    }

    private PropertyIsEqualTo buildUidFilter(Object value) {
        return FF.equals(FF.property(new NameImpl(OpenSearchAccess.EO_NAMESPACE, "identifier")),
                FF.literal(value));
    }

    private Filter buildSearchTermsFilter(Object value) {
        String converted = getParameter(SEARCH_TERMS.key, value, String.class);
        // split into parts separated by spaces, but not bits in double quotes
        Pattern MATCH_TERMS_SPLITTER = Pattern.compile("([^\"]\\S*|\".+?\")\\s*");
        Matcher m = MATCH_TERMS_SPLITTER.matcher(converted);
        List<String> keywords = new ArrayList<>();
        while (m.find()) {
            String group = m.group(1);
            if (group.startsWith("\"") && group.endsWith("\"") && group.length() > 1) {
                group = group.substring(1, group.length() - 1);
            }
            keywords.add(group);
        }
        // turn into a list of Like filters
        // TODO: actually implement a full text search function
        List<Filter> filters = keywords.stream()
                .map(s -> FF.like(FF.property("htmlDescription"), "%" + s + "%"))
                .collect(Collectors.toList());
        // combine and return
        Filter result = Predicates.or(filters);
        return result;
    }

    private <T> T getParameter(String key, Map rawKvp, Class<T> targetClass) {
        Object value = rawKvp.get(key);
        if (value == null) {
            return null;
        } else {
            return getParameter(key, value, targetClass);
        }
    }

    private <T> T getParameter(String key, Object value, Class<T> targetClass) {
        T converted = Converters.convert(value, targetClass);
        if (converted == null) {
            throw new OWS20Exception(
                    key + " is empty of cannot be converted to a " + targetClass.getSimpleName(),
                    OWSExceptionCode.InvalidParameterValue);
        }
        return converted;
    }

    private Filter buildGenericFilter(Parameter<?> parameter, Object value) {
        return Filter.INCLUDE;
    }

    private Collection<Parameter<?>> getSearchParameters(SearchRequest request) throws IOException {
        String parentId = request.getParentId();
        if (parentId == null) {
            return oseo.getCollectionSearchParameters();
        } else {
            return oseo.getProductSearchParameters(parentId);
        }
    }

}
