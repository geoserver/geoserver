/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.gwc.layer.CatalogConfiguration;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.gwc.wmts.dimensions.Dimension;
import org.geoserver.gwc.wmts.dimensions.DimensionsUtils;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.ows.util.KvpMap;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.WMS;
import org.geoserver.wms.dimension.DimensionFilterBuilder;
import org.geotools.data.Query;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.SchemaException;
import org.geotools.filter.visitor.SimplifyingFilterVisitor;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.conveyor.Conveyor;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.io.XMLBuilder;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.layer.TileLayerDispatcher;
import org.geowebcache.service.OWSException;
import org.geowebcache.service.wmts.WMTSExtensionImpl;
import org.geowebcache.storage.StorageBroker;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.sort.SortOrder;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

/**
 * WMTS extension that provides the necessary metadata and operations for handling multidimensional
 * requests.
 */
public final class MultiDimensionalExtension extends WMTSExtensionImpl {

    private static final FilterFactory2 FILTER_FACTORY = CommonFactoryFinder.getFilterFactory2();

    private static final Logger LOGGER = Logging.getLogger(MultiDimensionalExtension.class);
    public static final String SPACE_DIMENSION = "bbox";
    public static final Set<String> ALL_DOMAINS = Collections.emptySet();
    /** Configuration key for the maximum allowed value of expand_limit */
    public static final String EXPAND_LIMIT_MAX_KEY = "MD_DOMAIN_EXPAND_LIMIT_MAX";

    public static final String DOMAIN_VALUES_LIMIT_MAX_KEY = "MD_GET_DOMAIN_VALUES_LIMIT_MAX";
    /** Default value for the maximum allowed value of expand_limit, if not configured */
    private static final int EXPAND_LIMIT_MAX_DEFAULT =
            Integer.getInteger(EXPAND_LIMIT_MAX_KEY, 100000);
    /** Default value for the maximum allowed value of expand_limit, if not configured */
    private static final int DOMAIN_VALUES_LIMIT_MAX_DEFAULT =
            Integer.getInteger(DOMAIN_VALUES_LIMIT_MAX_KEY, 100000);

    /** Configuration key for expand_limit, if none is provided */
    public static final String EXPAND_LIMIT_KEY = "MD_DOMAIN_EXPAND_LIMIT";

    public static final String DOMAIN_VALUES_LIMIT_KEY = "MD_GET_DOMAIN_VALUES_LIMIT";
    /** Default value for the maximum allowed value of expand_limit, if not configured */
    private static final int EXPAND_LIMIT_DEFAULT = Integer.getInteger(EXPAND_LIMIT_KEY, 1000);

    private static final int DOMAIN_VALUES_LIMIT_DEFAULT =
            Integer.getInteger(DOMAIN_VALUES_LIMIT_KEY, 1000);

    public static final String EXPAND_LIMIT = "expandLimit";
    public static final String DOMAIN_VALUES_LIMIT = "limit";

    private final FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory();
    private final GeoServer geoServer;

    private TileLayerDispatcher tileLayerDispatcher;

    private final WMS wms;
    private final Catalog catalog;

    public MultiDimensionalExtension(
            GeoServer geoServer,
            WMS wms,
            Catalog catalog,
            TileLayerDispatcher tileLayerDispatcher) {
        this.geoServer = geoServer;
        this.wms = wms;
        this.catalog = catalog;
        this.tileLayerDispatcher = tileLayerDispatcher;
    }

    private final List<OperationMetadata> extraOperations = new ArrayList<>();

    {
        extraOperations.add(new OperationMetadata("DescribeDomains"));
        extraOperations.add(new OperationMetadata("GetDomainValues"));
        extraOperations.add(new OperationMetadata("GetFeature"));
        extraOperations.add(new OperationMetadata("GetHistogram"));
    }

    @Override
    public List<OperationMetadata> getExtraOperationsMetadata() throws IOException {
        return extraOperations;
    }

    @Override
    public Conveyor getConveyor(
            HttpServletRequest request, HttpServletResponse response, StorageBroker storageBroker)
            throws GeoWebCacheException, OWSException {
        // parse the request parameters converting string raw values to java objects
        KvpMap parameters = KvpUtils.normalize(request.getParameterMap());
        KvpUtils.parse(parameters);
        // let's see if we can handle this request
        String operationName = (String) parameters.get("request");
        return Operation.match(operationName, request, response, storageBroker, parameters);
    }

    @Override
    public boolean handleRequest(Conveyor candidateConveyor) throws OWSException {
        if (!(candidateConveyor instanceof SimpleConveyor)) {
            return false;
        }
        SimpleConveyor conveyor = (SimpleConveyor) candidateConveyor;
        switch (conveyor.getOperation()) {
            case DESCRIBE_DOMAINS:
                try {
                    executeDescribeDomainsOperation(conveyor);
                } catch (Exception exception) {
                    LOGGER.log(
                            Level.SEVERE, "Error executing describe domains operation.", exception);
                    return rethrowException(
                            exception, "Error executing describe domains operation:");
                }
                break;
            case GET_DOMAIN_VALUES:
                try {
                    executeGetDomainValuesOperation(conveyor);
                } catch (Exception exception) {
                    LOGGER.log(
                            Level.SEVERE,
                            "Error executing get domains values operation.",
                            exception);
                    return rethrowException(
                            exception, "Error executing get domain values operation:");
                }
                break;
            case GET_HISTOGRAM:
                try {
                    executeGetHistogramOperation(conveyor);
                } catch (Exception exception) {
                    LOGGER.log(Level.SEVERE, "Error executing get histogram operation.", exception);
                    rethrowException(exception, "Error executing get histogram operation:");
                }
                break;
            case GET_FEATURE:
                try {
                    executeGetFeatureOperation(conveyor);
                } catch (Exception exception) {
                    LOGGER.log(Level.SEVERE, "Error executing get feature operation.", exception);
                    rethrowException(exception, "Error executing get feature operation:");
                }
                break;
            default:
                return false;
        }
        return true;
    }

    public boolean rethrowException(Exception exception, String s) throws OWSException {
        if (exception instanceof OWSException) {
            throw (OWSException) exception;
        }
        throw new OWSException(500, "NoApplicableCode", "", s + exception.getMessage());
    }

    @Override
    public void encodeLayer(XMLBuilder xmlBuilder, TileLayer tileLayer) throws IOException {
        LayerInfo layerInfo = getLayerInfo(tileLayer, tileLayer.getName());
        if (layerInfo == null) {
            // dimension are not supported for this layer (maybe is a layer group)
            return;
        }
        try {
            List<Dimension> dimensions =
                    DimensionsUtils.extractDimensions(wms, layerInfo, ALL_DOMAINS);
            encodeLayerDimensions(xmlBuilder, dimensions);
        } catch (OWSException e) {
            // should not happen
            throw new RuntimeException(e);
        }
    }

    private Domains getDomains(SimpleConveyor conveyor) throws Exception {
        // getting and parsing the mandatory parameters
        String layerName = (String) conveyor.getParameter("layer", true);
        TileLayer tileLayer = tileLayerDispatcher.getTileLayer(layerName);
        LayerInfo layerInfo = getLayerInfo(tileLayer, layerName);
        Set<String> requestedDomains = getRequestedDomains(conveyor.getParameter("domains", false));
        // getting this layer dimensions along with its values
        List<Dimension> dimensions =
                DimensionsUtils.extractDimensions(wms, layerInfo, requestedDomains);
        ReferencedEnvelope boundingBox = getRequestedBoundingBox(conveyor, tileLayer);
        // add any domain provided restriction and set the bounding box
        ResourceInfo resource = layerInfo.getResource();
        Filter filter = getDomainRestrictions(conveyor, dimensions, boundingBox, resource);
        // compute the bounding box
        ReferencedEnvelope spatialDomain = null;
        if (requestedDomains == ALL_DOMAINS || requestedDomains.contains(SPACE_DIMENSION)) {
            spatialDomain = DimensionsUtils.getBounds(resource, filter);
        }
        // get the threshold
        int expandLimit =
                getExpandlimit(
                        resource,
                        conveyor.getParameter(EXPAND_LIMIT, false),
                        conveyor.getResponse());
        // encode the domains
        return new Domains(
                        dimensions,
                        layerInfo,
                        spatialDomain,
                        SimplifyingFilterVisitor.simplify(filter))
                .withExpandLimit(expandLimit);
    }

    private Domains getDomainValues(SimpleConveyor conveyor) throws Exception {
        // getting and parsing the mandatory parameters
        String layerName = (String) conveyor.getParameter("layer", true);
        TileLayer tileLayer = tileLayerDispatcher.getTileLayer(layerName);
        LayerInfo layerInfo = getLayerInfo(tileLayer, layerName);
        Set<String> requestedDomains = getRequestedDomains(conveyor.getParameter("domain", true));
        // getting this layer dimensions along with its values
        List<Dimension> dimensions =
                DimensionsUtils.extractDimensions(wms, layerInfo, requestedDomains);
        Dimension dimension = dimensions.get(0);
        ReferencedEnvelope boundingBox = getRequestedBoundingBox(conveyor, tileLayer);

        // add any domain provided restriction and set the bounding box
        ResourceInfo resource = layerInfo.getResource();
        Filter filter = getDomainRestrictions(conveyor, dimensions, boundingBox, resource);

        // add the filter and the sorting
        SortOrder sortOrder = getSortOrder(conveyor);
        String fromValue = (String) conveyor.getParameter("fromValue", false);
        Filter fromValueFilter = getStartValueFilter(fromValue, dimension, sortOrder, resource);
        if (!Filter.INCLUDE.equals(fromValueFilter)) {
            filter = filterFactory.and(filter, fromValueFilter);
        }

        // get the limit, if any
        int limit =
                getLimit(
                        resource,
                        conveyor.getParameter(DOMAIN_VALUES_LIMIT, false),
                        conveyor.getResponse());

        // encode the domains
        return new Domains(dimensions, layerInfo, null, SimplifyingFilterVisitor.simplify(filter))
                .withMaxReturnedValues(limit)
                .withSortOrder(sortOrder)
                .withFromValue(fromValue);
    }

    private SortOrder getSortOrder(SimpleConveyor conveyor) throws OWSException {
        String sort = (String) conveyor.getParameter("sort", false);
        if (sort == null || "ASC".equalsIgnoreCase(sort)) {
            return SortOrder.ASCENDING;
        } else if ("DESC".equalsIgnoreCase(sort)) {
            return SortOrder.DESCENDING;
        }
        throw new OWSException(
                HttpStatus.BAD_REQUEST.value(),
                "InvalidParameterValue",
                "sort",
                "Invalid sort value, but be either asc or desc");
    }

    private Filter getStartValueFilter(
            String fromValue, Dimension dimension, SortOrder sortOrder, ResourceInfo resource)
            throws OWSException {
        if (fromValue == null) {
            return Filter.INCLUDE;
        }

        Object converted = Converters.convert(fromValue, dimension.getDimensionType());
        if (converted == null) {
            throw new OWSException(
                    HttpStatus.BAD_REQUEST.value(),
                    "InvalidParameterValue",
                    "fromValue",
                    "Invalid fromValue, could not be converted to target type "
                            + dimension.getDimensionType().getSimpleName());
        }
        Tuple<String, String> attributes = DimensionsUtils.getAttributes(resource, dimension);
        String attribute = attributes.first; // getDomain only uses first attribute to list values
        if (sortOrder == SortOrder.ASCENDING) {
            return filterFactory.greater(
                    filterFactory.property(attribute), filterFactory.literal(converted));
        } else {
            return filterFactory.less(
                    filterFactory.property(attribute), filterFactory.literal(converted));
        }
    }

    private Filter getDomainRestrictions(
            SimpleConveyor conveyor,
            List<Dimension> dimensions,
            ReferencedEnvelope boundingBox,
            ResourceInfo resource)
            throws IOException, TransformException, SchemaException, FactoryException {
        Filter filter = DimensionsUtils.getBoundingBoxFilter(resource, boundingBox, filterFactory);
        for (Dimension dimension : dimensions) {
            Object restriction = conveyor.getParameter(dimension.getDimensionName(), false);
            if (restriction != null) {
                Tuple<String, String> attributes =
                        DimensionsUtils.getAttributes(resource, dimension);
                filter =
                        appendDomainRestrictionsFilter(
                                filter, attributes.first, attributes.second, restriction);
            }
        }

        return filter;
    }

    private ReferencedEnvelope getRequestedBoundingBox(SimpleConveyor conveyor, TileLayer tileLayer)
            throws FactoryException {
        // let's see if we have a spatial limitation
        ReferencedEnvelope boundingBox = (ReferencedEnvelope) conveyor.getParameter("bbox", false);
        // if we have a bounding box we need to set the crs based on the tile matrix set
        if (boundingBox != null && boundingBox.getCoordinateReferenceSystem() == null) {
            String providedTileMatrixSet = (String) conveyor.getParameter("tileMatrixSet", true);
            // getting the layer grid set corresponding to the provided tile matrix set
            GridSubset gridSubset = tileLayer.getGridSubset(providedTileMatrixSet);
            if (gridSubset == null) {
                // the provided tile matrix set is not supported by this layer
                throw new RuntimeException(
                        String.format("Unknown grid set '%s'.", providedTileMatrixSet));
            }
            // set bounding box crs base on tile matrix tile set srs
            boundingBox =
                    new ReferencedEnvelope(boundingBox, CRS.decode(gridSubset.getSRS().toString()));
        }
        return boundingBox;
    }

    private Set<String> getRequestedDomains(SimpleConveyor conveyor, Operation operation) {
        Set<String> requestedDomains;
        if (operation == Operation.GET_DOMAIN_VALUES) {
            requestedDomains =
                    Collections.singleton((String) conveyor.getParameter("domain", true));
        } else {
            requestedDomains = getRequestedDomains(conveyor.getParameter("domains", false));
        }
        return requestedDomains;
    }

    private int getExpandlimit(
            ResourceInfo resource, Object clientExpandLimit, HttpServletResponse response)
            throws OWSException {
        WMTSInfo wmts = geoServer.getService(WMTSInfo.class);
        int expandLimitMax =
                getConfiguredExpansionLimit(
                        resource, wmts, EXPAND_LIMIT_MAX_KEY, EXPAND_LIMIT_MAX_DEFAULT);
        int expandLimitDefault =
                getConfiguredExpansionLimit(resource, wmts, EXPAND_LIMIT_KEY, EXPAND_LIMIT_DEFAULT);

        if (clientExpandLimit != null) {
            Integer value = Converters.convert(clientExpandLimit, Integer.class);
            if (value == null) {
                throw new OWSException(
                        400,
                        "InvalidParameterValue",
                        EXPAND_LIMIT,
                        "Invalid "
                                + EXPAND_LIMIT
                                + " "
                                + "value "
                                + clientExpandLimit
                                + ", expected an integer value");
            }
            if (value < expandLimitMax) {
                return value;
            } else {
                String message =
                        "Client provided a expand value higher than the configured max, using the "
                                + "internal value (provided/max): "
                                + value
                                + "/"
                                + expandLimitMax;
                LOGGER.log(Level.INFO, message);
                response.addHeader(HttpHeaders.WARNING, "299 " + message);
                return expandLimitMax;
            }
        } else {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                        Level.FINE,
                        "Client did not provide a expansion limit, using the internal default value: "
                                + expandLimitDefault);
            }
            return expandLimitDefault;
        }
    }

    private int getLimit(ResourceInfo resource, Object clientLimit, HttpServletResponse response)
            throws OWSException {
        if (clientLimit != null) {
            Integer value = Converters.convert(clientLimit, Integer.class);
            if (value == null) {
                throw new OWSException(
                        400,
                        "InvalidParameterValue",
                        DOMAIN_VALUES_LIMIT,
                        "Invalid "
                                + DOMAIN_VALUES_LIMIT
                                + " "
                                + "value "
                                + clientLimit
                                + ", expected an integer value");
            }
            int limitMax = DOMAIN_VALUES_LIMIT_MAX_DEFAULT;
            if (value < limitMax) {
                return value;
            } else {
                String message =
                        "Client provided a limit value higher than the configured max, using the "
                                + "internal value (provided/max): "
                                + value
                                + "/"
                                + limitMax;
                LOGGER.log(Level.INFO, message);
                response.addHeader(HttpHeaders.WARNING, "299 " + message);
                return limitMax;
            }
        } else {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                        Level.FINE,
                        "Client did not provide a limit, using the internal default value: "
                                + DOMAIN_VALUES_LIMIT_DEFAULT);
            }
            return DOMAIN_VALUES_LIMIT_DEFAULT;
        }
    }

    private Integer getConfiguredExpansionLimit(
            ResourceInfo resource, WMTSInfo wmts, String limitKey, int defaultValue) {
        MetadataMap resourceMetadata = resource.getMetadata();
        Integer limit = resourceMetadata.get(limitKey, Integer.class);
        if (limit == null) {
            MetadataMap serviceMetadata = wmts.getMetadata();
            limit =
                    Optional.ofNullable(serviceMetadata.get(limitKey, Integer.class))
                            .orElse(defaultValue);
        }
        return limit;
    }

    /**
     * Helper method that will build a dimension domain values filter based on this dimension start
     * and end attributes. The created filter will be merged with the provided filter.
     */
    protected Filter appendDomainRestrictionsFilter(
            Filter filter, String startAttribute, String endAttribute, Object domainRestrictions) {
        DimensionFilterBuilder dimensionFilterBuilder = new DimensionFilterBuilder(filterFactory);
        List<Object> restrictionList =
                domainRestrictions instanceof Collection
                        ? new ArrayList<>((Collection) domainRestrictions)
                        : Arrays.asList(domainRestrictions);
        dimensionFilterBuilder.appendFilters(startAttribute, endAttribute, restrictionList);
        return filterFactory.and(filter, dimensionFilterBuilder.getFilter());
    }

    private Set<String> getRequestedDomains(Object domains) {
        if (domains == null) {
            return ALL_DOMAINS;
        }

        String[] domainNames = domains.toString().trim().split("\\s*,\\s*");
        return new LinkedHashSet<>(Arrays.asList(domainNames));
    }

    private void executeDescribeDomainsOperation(SimpleConveyor conveyor) throws Exception {
        Domains domains = getDomains(conveyor);
        DescribeDomainsTransformer transformer = new DescribeDomainsTransformer(wms);
        transformer.transform(domains, conveyor.getResponse().getOutputStream());
        conveyor.getResponse().setContentType("text/xml");
    }

    private void executeGetDomainValuesOperation(SimpleConveyor conveyor) throws Exception {
        Domains domains = getDomainValues(conveyor);
        GetDomainValuesTransformer transformer = new GetDomainValuesTransformer(wms);
        transformer.transform(domains, conveyor.getResponse().getOutputStream());
        conveyor.getResponse().setContentType("text/xml");
    }

    private void executeGetHistogramOperation(SimpleConveyor conveyor) throws Exception {
        Domains domains = getDomains(conveyor);
        domains.setHistogram((String) conveyor.getParameter("histogram", true));
        domains.setResolution((String) conveyor.getParameter("resolution", false));
        HistogramTransformer transformer = new HistogramTransformer(wms);
        transformer.transform(domains, conveyor.getResponse().getOutputStream());
        conveyor.getResponse().setContentType("text/xml");
    }

    private void executeGetFeatureOperation(SimpleConveyor conveyor) throws Exception {
        Domains domains = getDomains(conveyor);
        FeaturesTransformer transformer = new FeaturesTransformer(wms);
        transformer.transform(domains, conveyor.getResponse().getOutputStream());
        // right now we only support gml in the the future we may need to support other formats
        conveyor.getResponse().setContentType("text/xml; subtype=gml/3.1.1");
    }

    private LayerInfo getLayerInfo(TileLayer tileLayer, String layerName) {
        // let's see if we can get the layer info from the tile layer
        if (tileLayer != null && tileLayer instanceof GeoServerTileLayer) {
            PublishedInfo publishedInfo = ((GeoServerTileLayer) tileLayer).getPublishedInfo();
            if (!(publishedInfo instanceof LayerInfo)) {
                // dimensions are not supported for layers groups
                return null;
            }
            // go through the catalog to make sure we get all the wrapping necessary, including
            // security
            return catalog.getLayer(publishedInfo.getId());
        }
        // let's see if we are in the context of a virtual service
        WorkspaceInfo localWorkspace = LocalWorkspace.get();
        if (localWorkspace != null) {
            // we need to make sure that the layer name is prefixed with the local workspace
            layerName = CatalogConfiguration.removeWorkspacePrefix(layerName, catalog);
            layerName = localWorkspace.getName() + ":" + layerName;
        }
        LayerInfo layerInfo = catalog.getLayerByName(layerName);
        if (layerInfo == null) {
            // the catalog is not aware of this layer, there is nothing we can do
            throw new ServiceException(String.format("Unknown layer '%s'.", layerName));
        }
        return layerInfo;
    }

    /**
     * Helper method that will encode a layer dimensions, if the layer dimension are NULL or empty
     * nothing will be done.
     */
    private void encodeLayerDimensions(XMLBuilder xml, List<Dimension> dimensions)
            throws IOException {
        for (Dimension dimension : dimensions) {
            // encode each dimension as top element
            encodeLayerDimension(xml, dimension);
        }
    }

    /**
     * Helper method that will encode a dimension, if the dimension is NULL nothing will be done.
     * All optional attributes that are NULL will be ignored.
     */
    private void encodeLayerDimension(XMLBuilder xml, Dimension dimension) throws IOException {
        xml.indentElement("Dimension");
        // identifier is mandatory
        xml.simpleElement("ows:Identifier", dimension.getDimensionName(), true);
        // default value is mandatory
        xml.simpleElement("Default", dimension.getDefaultValueAsString(), true);
        int limit = DimensionsUtils.NO_LIMIT;
        if (dimension.getDimensionInfo().getPresentation() != DimensionPresentation.LIST) {
            // force it to return a min/max representation
            limit = 0;
        }
        List<String> values = dimension.getDomainValuesAsStrings(Query.ALL, limit).second;
        for (String value : values) {
            xml.simpleElement("Value", value, true);
        }
        xml.endElement("Dimension");
    }
}
