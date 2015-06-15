package org.geoserver.wms.vector;

import java.awt.Rectangle;
import java.util.Set;

import org.geotools.geometry.jts.ReferencedEnvelope;

public interface VectorTileBuilderFactory {

    Set<String> getOutputFormats();

    String getMimeType();

    VectorTileBuilder newBuilder(Rectangle screenSize, ReferencedEnvelope mapArea);

}
