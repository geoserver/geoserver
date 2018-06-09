/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.gwc.layer.CatalogConfiguration;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.gwc.wmts.dimensions.Dimension;
import org.geoserver.gwc.wmts.dimensions.DimensionsUtils;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.ows.util.KvpMap;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.WMS;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.visitor.SimplifyingFilterVisitor;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
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

/**
 * WMTS extension that provides the necessary metadata and operations for handling multidimensional
 * requests.
 */
public final class MultiDimensionalExtension extends WMTSExtensionImpl {

    private static final Logger LOGGER = Logging.getLogger(MultiDimensionalExtension.class);

    private final FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory();

    private TileLayerDispatcher tileLayerDispatcher;

    private final WMS wms;
    private final Catalog catalog;

    public MultiDimensionalExtension(
            WMS wms, Catalog catalog, TileLayerDispatcher tileLayerDispatcher) {
        this.wms = wms;
        this.catalog = catalog;
        this.tileLayerDispatcher = tileLayerDispatcher;
    }

    private final List<OperationMetadata> extraOperations = new ArrayList<>();

    {
        extraOperations.add(new OperationMetadata("DescribeDomains"));
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
                    throw new OWSException(
                            500,
                            "NoApplicableCode",
                            "",
                            "Error executing describe domains operation:" + exception.getMessage());
                }
                break;
            case GET_HISTOGRAM:
                try {
                    executeGetHistogramOperation(conveyor);
                } catch (Exception exception) {
                    LOGGER.log(Level.SEVERE, "Error executing get histogram operation.", exception);
                    throw new OWSException(
                            500,
                            "NoApplicableCode",
                            "",
                            "Error executing get histogram operation:" + exception.getMessage());
                }
                break;
            case GET_FEATURE:
                try {
                    executeGetFeatureOperation(conveyor);
                } catch (Exception exception) {
                    LOGGER.log(Level.SEVERE, "Error executing get feature operation.", exception);
                    throw new OWSException(
                            500,
                            "NoApplicableCode",
                            "",
                            "Error executing get feature operation:" + exception.getMessage());
                }
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    public void encodeLayer(XMLBuilder xmlBuilder, TileLayer tileLayer) throws IOException {
        LayerInfo layerInfo = getLayerInfo(tileLayer, tileLayer.getName());
        if (layerInfo == null) {
            // dimension are not supported for this layer (maybe is a layer group)
            return;
        }
        List<Dimension> dimensions = DimensionsUtils.extractDimensions(wms, layerInfo);
        encodeLayerDimensions(xmlBuilder, dimensions);
    }

    private Domains getDomains(SimpleConveyor conveyor) throws Exception {
        // getting and parsing the mandatory parameters
        String layerName = (String) conveyor.getParameter("layer", true);
        TileLayer tileLayer = tileLayerDispatcher.getTileLayer(layerName);
        LayerInfo layerInfo = getLayerInfo(tileLayer, layerName);
        // getting this layer dimensions along with its values
        List<Dimension> dimensions = DimensionsUtils.extractDimensions(wms, layerInfo);
        // let's see if we have a spatial limitation
        ReferencedEnvelope boundingBox = (ReferencedEnvelope) conveyor.getParameter("bbox", false);
        // if we have a bounding box we need to set the crs based on the tile matrix set
        if (boundingBox != null) {
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
        // add any domain provided restriction and set the bounding box
        Filter filter = Filter.INCLUDE;
        for (Dimension dimension : dimensions) {
            Object restriction = conveyor.getParameter(dimension.getDimensionName(), false);
            dimension.setBoundingBox(boundingBox);
            dimension.addDomainRestriction(restriction);
            filter = filterFactory.and(filter, dimension.getFilter());
        }
        // encode the domains
        return new Domains(
                dimensions, layerInfo, boundingBox, SimplifyingFilterVisitor.simplify(filter));
    }

    private void executeDescribeDomainsOperation(SimpleConveyor conveyor) throws Exception {
        Domains domains = getDomains(conveyor);
        DescribeDomainsTransformer transformer = new DescribeDomainsTransformer(wms);
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
            return (LayerInfo) publishedInfo;
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
        for (String value : dimension.getDomainValuesAsStrings(Filter.INCLUDE).second.second) {
            xml.simpleElement("Value", value, true);
        }
        xml.endElement("Dimension");
    }
}
