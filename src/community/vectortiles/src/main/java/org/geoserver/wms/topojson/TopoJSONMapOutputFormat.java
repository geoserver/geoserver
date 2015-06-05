/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.topojson;

import java.awt.geom.AffineTransform;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.Set;

import org.geoserver.platform.ServiceException;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.map.AbstractMapOutputFormat;
import org.geoserver.wms.map.RawMap;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.Layer;
import org.geotools.referencing.operation.projection.ProjectionException;
import org.opengis.feature.Feature;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.spatial.BBOX;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;

public class TopoJSONMapOutputFormat extends AbstractMapOutputFormat {

    public static final String MIME_TYPE = "application/json;type=topojson";

    public static final Set<String> OUTPUT_FORMATS = ImmutableSet.of(MIME_TYPE, "topojson");

    public TopoJSONMapOutputFormat() {
        super(MIME_TYPE, OUTPUT_FORMATS);
    }

    /**
     * @return {@code null}
     */
    @Override
    public MapProducerCapabilities getCapabilities(final String format) {
        return null;
    }

    @Override
    public RawMap produceMap(final WMSMapContent mapContent) throws ServiceException, IOException {

        final ReferencedEnvelope renderingArea = mapContent.getRenderingArea();
        final AffineTransform worldToScreen = mapContent.getRenderingTransform();

        final FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

        TopologyBuilder topologyBuilder = new TopologyBuilder(worldToScreen);

        List<Layer> layers = mapContent.layers();
        for (Layer layer : layers) {
            FeatureSource<?, ?> featureSource = layer.getFeatureSource();
            GeometryDescriptor geometryDescriptor = featureSource.getSchema()
                    .getGeometryDescriptor();
            if (null == geometryDescriptor) {
                continue;
            }
            String geomName = geometryDescriptor.getLocalName();
            BBOX bboxFilter = ff.bbox(ff.property(geomName), renderingArea);

            Query query = new Query(featureSource.getName().getLocalPart(), bboxFilter);
            query.setCoordinateSystem(renderingArea.getCoordinateReferenceSystem());
            query.setCoordinateSystemReproject(renderingArea.getCoordinateReferenceSystem());
            query.setMaxFeatures(10_000);

            FeatureCollection<?, ?> features = featureSource.getFeatures(query);
            FeatureIterator<?> it = features.features();
            Feature next;
            while (it.hasNext()) {
                try {
                    next = it.next();
                } catch (IllegalStateException e) {
                    if (e.getCause() instanceof ProjectionException) {
                        continue;
                    }
                    throw e;
                }
                topologyBuilder.addFeature(next);
            }
        }

        // do something.... and then:
        Topology topology = topologyBuilder.build();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TopoJSONEncoder encoder = new TopoJSONEncoder();

        Writer writer = new OutputStreamWriter(out, Charsets.UTF_8);
        encoder.encode(topology, writer);
        writer.flush();
        byte[] mapContents = out.toByteArray();
        RawMap map = new RawMap(mapContent, mapContents, MIME_TYPE);
        map.setResponseHeader("Content-Length", String.valueOf(mapContents.length));
        return map;
    }

}
