package org.geoserver.wms.vector;

import java.io.IOException;

import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WebMap;
import org.opengis.feature.simple.SimpleFeature;

public interface VectorTileBuilder {

    void addFeature(SimpleFeature feature);

    WebMap build(WMSMapContent mapContent) throws IOException;

}
