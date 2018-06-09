/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.mapbox;

import com.google.common.collect.ImmutableSet;
import java.awt.Rectangle;
import java.util.Set;
import org.geoserver.wms.vector.VectorTileBuilderFactory;
import org.geotools.geometry.jts.ReferencedEnvelope;

/** @author Niels Charlier */
public class MapBoxTileBuilderFactory implements VectorTileBuilderFactory {

    public static final String MIME_TYPE = "application/x-protobuf;type=mapbox-vector";

    public static final Set<String> OUTPUT_FORMATS = ImmutableSet.of(MIME_TYPE, "pbf");

    @Override
    public Set<String> getOutputFormats() {
        return OUTPUT_FORMATS;
    }

    @Override
    public String getMimeType() {
        return MIME_TYPE;
    }

    @Override
    public MapBoxTileBuilder newBuilder(Rectangle screenSize, ReferencedEnvelope mapArea) {
        return new MapBoxTileBuilder(screenSize, mapArea);
    }
}
