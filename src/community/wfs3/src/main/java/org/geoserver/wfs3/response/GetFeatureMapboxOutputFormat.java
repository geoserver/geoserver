package org.geoserver.wfs3.response;

import java.awt.Rectangle;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import no.ecc.vectortile.VectorTileEncoder;
import no.ecc.vectortile.VectorTileEncoderNoClip;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geoserver.wfs3.GetFeatureType;
import org.geoserver.wfs3.NCNameResourceCodec;
import org.geoserver.wfs3.TileDataRequest;
import org.geoserver.wfs3.WebFeatureService30;
import org.geoserver.wms.mapbox.MapBoxTileBuilderFactory;
import org.geoserver.wms.vector.Pipeline;
import org.geoserver.wms.vector.PipelineBuilder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.Version;
import org.geowebcache.config.DefaultGridsets;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSet;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.Attribute;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.Name;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/** Mapbox protobuf WFS3 output format */
public class GetFeatureMapboxOutputFormat extends WFSGetFeatureOutputFormat {

    private static final double OVER_SAMPLING_FACTOR = 2.0;
    private TileDataRequest tileData;
    private DefaultGridsets gridSets;

    public GetFeatureMapboxOutputFormat(GeoServer gs) {
        super(
                gs,
                new LinkedHashSet<String>() {
                    {
                        add(MapBoxTileBuilderFactory.MIME_TYPE);
                        add(MapBoxTileBuilderFactory.LEGACY_MIME_TYPE);
                    }
                });
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return MapBoxTileBuilderFactory.MIME_TYPE;
    }

    @Override
    protected void write(
            FeatureCollectionResponse featureCollection, OutputStream output, Operation getFeature)
            throws IOException, ServiceException {
        GetFeatureType getFeatureType = (GetFeatureType) getFeature.getParameters()[0];

        Integer resolution =
                getFeatureType.getResolution() != null ? getFeatureType.getResolution() : 256;
        FeatureCollection collection = featureCollection.getFeatures().get(0);
        // paint area, default 256x256
        final Rectangle paintArea = new Rectangle(resolution, resolution);
        // get CRS
        CoordinateReferenceSystem refSys =
                collection
                        .getSchema()
                        .getGeometryDescriptor()
                        .getType()
                        .getCoordinateReferenceSystem();
        // get area envelope from GridSet if tile request data, else from collection
        ReferencedEnvelope area = getArea(collection);
        // Build the Pipeline (sort of coordinates transformer)
        Pipeline pipeline = getPipeline(area, paintArea, refSys, resolution / 32);
        // setup the vector tile encoder
        VectorTileEncoder encoder = new VectorTileEncoderNoClip(resolution, resolution / 32, false);
        // encode every feature
        try (FeatureIterator<SimpleFeature> features = collection.features()) {
            while (features.hasNext()) {
                // get Feature and its attributes
                SimpleFeature feature = features.next();
                Geometry geom = (Geometry) feature.getDefaultGeometryProperty().getValue();
                Geometry finalGeom = pipeline.execute(geom);
                final String layerName = getFeatureName(feature);
                final Map<String, Object> properties = getProperties(feature);
                // add to encoder (if have at least a coordinate to encode)
                if (finalGeom.getCoordinates().length > 0)
                    encoder.addFeature(layerName, properties, finalGeom);
            }
            // write encoded stream
            output.write(encoder.encode());
        } catch (Exception e) {
            throw new ServiceException("Failed to build MVT output", e);
        }
    }

    private String getFeatureName(SimpleFeature feature) {
        Name name = feature.getFeatureType().getName();
        String ns = name.getNamespaceURI();
        String localName = name.getLocalPart();

        NamespaceInfo nsInfo = gs.getCatalog().getNamespaceByURI(ns);
        if (nsInfo != null) {
            String encodedName = NCNameResourceCodec.encode(nsInfo.getPrefix(), localName);
            return encodedName;
        }

        return localName;
    }

    private Map<String, Object> getProperties(SimpleFeature feature) {
        Map<String, Object> props = new LinkedHashMap<>();
        for (Property p : feature.getProperties()) {
            if (!(p instanceof Attribute) || (p instanceof GeometryAttribute)) {
                continue;
            }
            String name = p.getName().getLocalPart();
            Object value = p.getValue();
            if (value != null) {
                props.put(name, value);
            }
        }
        return props;
    }

    /**
     * Builds the Pipeline (sort of coordinates transformer)
     *
     * @param renderingArea area coordinates to render
     * @param paintArea pixel rectangle
     * @param sourceCrs CRS for the geometries
     * @param buffer pixel buffer t add
     * @return Pipeline builded object
     */
    protected Pipeline getPipeline(
            final ReferencedEnvelope renderingArea,
            final Rectangle paintArea,
            CoordinateReferenceSystem sourceCrs,
            int buffer) {
        Pipeline pipeline;
        try {
            final PipelineBuilder builder =
                    PipelineBuilder.newBuilder(
                            renderingArea, paintArea, sourceCrs, OVER_SAMPLING_FACTOR, buffer);

            pipeline =
                    builder.preprocess()
                            .transform(true)
                            .simplify(true)
                            .clip(true, true)
                            .collapseCollections()
                            .build();
        } catch (FactoryException e) {
            throw new ServiceException(e);
        }
        return pipeline;
    }

    @Override
    protected boolean canHandleInternal(Operation operation) {
        return WebFeatureService30.V3.compareTo(operation.getService().getVersion()) <= 0;
    }

    @Override
    public boolean canHandle(Version version) {
        return WebFeatureService30.V3.compareTo(version) <= 0;
    }

    @Override
    public boolean canHandle(Operation operation) {
        if ("GetFeature".equalsIgnoreCase(operation.getId())
                || "GetFeatureWithLock".equalsIgnoreCase(operation.getId())
                || "getTile".equalsIgnoreCase(operation.getId())) {
            // also check that the resultType is "results"
            GetFeatureRequest req = GetFeatureRequest.adapt(operation.getParameters()[0]);
            if (req.isResultTypeResults()) {
                // call subclass hook
                return canHandleInternal(operation);
            }
        }
        return false;
    }

    /** obtains the ReferencedEnvelope for the current tile */
    private ReferencedEnvelope envelopeFromTileRequestData() {
        GridSet gridset = gridSets.getGridSet(tileData.getTilingScheme()).get();
        BoundingBox bbox =
                gridset.boundsFromIndex(
                        new long[] {tileData.getCol(), tileData.getRow(), tileData.getLevel()});
        try {
            return ReferencedEnvelope.create(
                    new Envelope(bbox.getMinX(), bbox.getMaxX(), bbox.getMinY(), bbox.getMaxY()),
                    CRS.decode(gridset.getSrs().toString()));
        } catch (FactoryException e) {
            throw new RuntimeException(e);
        }
    }

    private ReferencedEnvelope getArea(FeatureCollection collection) {
        if (tileData.getBboxEnvelope() != null) {
            return tileData.getBboxEnvelope();
        }
        CoordinateReferenceSystem refSys =
                collection
                        .getSchema()
                        .getGeometryDescriptor()
                        .getType()
                        .getCoordinateReferenceSystem();
        // get area envelope from GridSet if tile request data, else from collection
        ReferencedEnvelope area =
                ReferencedEnvelope.create(
                        tileData.isTileRequest()
                                ? envelopeFromTileRequestData()
                                : collection.getBounds(),
                        refSys);
        return area;
    }

    public TileDataRequest getTileData() {
        return tileData;
    }

    public void setTileData(TileDataRequest tileData) {
        this.tileData = tileData;
    }

    public DefaultGridsets getGridSets() {
        return gridSets;
    }

    public void setGridSets(DefaultGridsets gridSets) {
        this.gridSets = gridSets;
    }

    @Override
    protected String getExtension(FeatureCollectionResponse response) {
        return "mbtile";
    }
}
