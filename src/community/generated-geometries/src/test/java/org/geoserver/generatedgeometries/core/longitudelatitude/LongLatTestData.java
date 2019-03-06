/*
 *  (c) 2019 Open Source Geospatial Foundation - all rights reserved
 *  This code is licensed under the GPL 2.0 license, available at the root
 *  application directory.
 */

/*
 *  (c) 2019 Open Source Geospatial Foundation - all rights reserved
 *  This code is licensed under the GPL 2.0 license, available at the root
 *  application directory.
 */

package org.geoserver.generatedgeometries.core.longitudelatitude;

import static java.lang.String.format;
import static org.geoserver.data.test.MockData.DEFAULT_PREFIX;
import static org.geoserver.data.test.MockData.DEFAULT_URI;
import static org.geoserver.generatedgeometries.core.GeometryGenerationStrategy.STRATEGY_METADATA_KEY;
import static org.geoserver.generatedgeometries.core.longitudelatitude.LongLatGeometryGenerationStrategy.GEOMETRY_ATTRIBUTE_NAME;
import static org.geoserver.generatedgeometries.core.longitudelatitude.LongLatGeometryGenerationStrategy.GEOMETRY_CRS;
import static org.geoserver.generatedgeometries.core.longitudelatitude.LongLatGeometryGenerationStrategy.LATITUDE_ATTRIBUTE_NAME;
import static org.geoserver.generatedgeometries.core.longitudelatitude.LongLatGeometryGenerationStrategy.LONGITUDE_ATTRIBUTE_NAME;

import com.google.common.base.Joiner;
import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.MetadataMap;

public class LongLatTestData {

    public static final String LONG_LAT_NO_GEOM_ON_THE_FLY_LAYER = "LongLatBasicLayer";
    public static final QName LONG_LAT_NO_GEOM_ON_THE_FLY_QNAME =
            new QName(DEFAULT_URI, LONG_LAT_NO_GEOM_ON_THE_FLY_LAYER, DEFAULT_PREFIX);
    public static final String LONG_LAT_LAYER = "LongLatLayer";
    public static final QName LONG_LAT_QNAME =
            new QName(DEFAULT_URI, LONG_LAT_LAYER, DEFAULT_PREFIX);
    static final int STD_WIDTH = 100;
    static final int STD_HEIGH = 100;

    static void setupXMLNamespaces() {
        // init xmlunit
        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("wfs", "http://www.opengis.net/wfs");
        namespaces.put("ows", "http://www.opengis.net/ows");
        namespaces.put("ogc", "http://www.opengis.net/ogc");
        namespaces.put("xs", "http://www.w3.org/2001/XMLSchema");
        namespaces.put("xsd", "http://www.w3.org/2001/XMLSchema");
        namespaces.put("gml", "http://www.opengis.net/gml");
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        namespaces.put("gs", "http://geoserver.org");
        namespaces.put("soap12", "http://www.w3.org/2003/05/soap-envelope");
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
    }

    static String bbox(int minX, int minY, int maxX, int maxY) {
        return Joiner.on(",").join(minX, minY, maxX, maxY);
    }

    static String wholeWorld() {
        return bbox(-180, -90, 180, 90);
    }

    public static String filenameOf(String layerName) {
        return layerName + ".properties";
    }

    static String wfsUrl(String layer) {
        return format(
                "ows?service=WFS&version=1.0.0&request=GetFeature&typeName=%s&maxFeatures=50",
                layer);
    }

    static String wmsUrl(String layer, String bbox, int width, int height) {
        return format(
                "wms?service=WMS&version=1.1.0&request=GetMap&layers=%s&bbox=%s&width=%d&height=%d&srs=EPSG:4326&format=image/png",
                layer, bbox, width, height);
    }

    static String wmsUrlStdSize(String layer, String bbox) {
        return wmsUrl(layer, bbox, STD_WIDTH, STD_HEIGH);
    }

    public static void enableGeometryGenerationStrategy(
            Catalog catalog, FeatureTypeInfo featureTypeInfo) {
        MetadataMap metadata = featureTypeInfo.getMetadata();
        metadata.put(STRATEGY_METADATA_KEY, LongLatGeometryGenerationStrategy.NAME);
        metadata.put(GEOMETRY_ATTRIBUTE_NAME, "geom");
        metadata.put(LONGITUDE_ATTRIBUTE_NAME, "lon");
        metadata.put(LATITUDE_ATTRIBUTE_NAME, "lat");
        metadata.put(GEOMETRY_CRS, "EPSG:4326");
        catalog.save(featureTypeInfo);
    }
}
