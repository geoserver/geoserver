/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.vector;

import java.awt.Rectangle;
import java.util.Set;

import org.geotools.geometry.jts.ReferencedEnvelope;

public interface VectorTileBuilderFactory {

    Set<String> getOutputFormats();

    String getMimeType();

    VectorTileBuilder newBuilder(Rectangle screenSize, ReferencedEnvelope mapArea);

}
