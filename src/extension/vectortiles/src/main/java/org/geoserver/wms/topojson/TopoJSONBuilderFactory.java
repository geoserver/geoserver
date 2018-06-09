/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.topojson;

import com.google.common.collect.ImmutableSet;
import java.awt.Rectangle;
import java.util.Set;
import org.geoserver.wms.vector.VectorTileBuilder;
import org.geoserver.wms.vector.VectorTileBuilderFactory;
import org.geotools.geometry.jts.ReferencedEnvelope;

public class TopoJSONBuilderFactory implements VectorTileBuilderFactory {

    public static final String MIME_TYPE = "application/json;type=topojson";

    public static final Set<String> OUTPUT_FORMATS = ImmutableSet.of(MIME_TYPE, "topojson");

    @Override
    public Set<String> getOutputFormats() {
        return OUTPUT_FORMATS;
    }

    @Override
    public String getMimeType() {
        return MIME_TYPE;
    }

    @Override
    public VectorTileBuilder newBuilder(Rectangle screenSize, ReferencedEnvelope mapArea) {
        return new TopologyBuilder(screenSize, mapArea);
    }
}
