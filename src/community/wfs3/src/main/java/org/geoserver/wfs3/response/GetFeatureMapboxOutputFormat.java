package org.geoserver.wfs3.response;

import java.awt.Rectangle;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.TreeMap;
import no.ecc.vectortile.VectorTileEncoder;
import no.ecc.vectortile.VectorTileEncoderNoClip;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs3.GetFeatureType;
import org.geoserver.wfs3.WebFeatureService30;
import org.geoserver.wms.mapbox.MapBoxTileBuilderFactory;
import org.geoserver.wms.vector.Pipeline;
import org.geoserver.wms.vector.PipelineBuilder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.Version;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.Attribute;
import org.opengis.feature.ComplexAttribute;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/** Mapbox protobuf WFS3 output format */
public class GetFeatureMapboxOutputFormat extends WFSGetFeatureOutputFormat {

    private double overSamplingFactor = 2.0;

    public GetFeatureMapboxOutputFormat(GeoServer gs) {
        super(gs, MapBoxTileBuilderFactory.MIME_TYPE);
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
        Integer resolution = 256;
        try {
            resolution = Integer.parseInt(getFeatureType.getResolution());
        } catch (NumberFormatException e) {
            // continue with default
        }
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
        ReferencedEnvelope area = ReferencedEnvelope.create(collection.getBounds(), refSys);
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
                Geometry finalGeom = finalGeom = pipeline.execute(geom);
                final String layerName = feature.getName().getLocalPart();
                final Map<String, Object> properties = getProperties(feature);
                // add to encoder (if have at least a coordinate to encode)
                if (finalGeom.getCoordinates().length > 0)
                    encoder.addFeature(layerName, properties, finalGeom);
            }
            // write encoded stream
            output.write(encoder.encode());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Map<String, Object> getProperties(ComplexAttribute feature) {
        Map<String, Object> props = new TreeMap<>();
        for (Property p : feature.getProperties()) {
            if (!(p instanceof Attribute) || (p instanceof GeometryAttribute)) {
                continue;
            }
            String name = p.getName().getLocalPart();
            Object value;
            if (p instanceof ComplexAttribute) {
                value = getProperties((ComplexAttribute) p);
            } else {
                value = p.getValue();
            }
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
                            renderingArea, paintArea, sourceCrs, overSamplingFactor, buffer);

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
}
