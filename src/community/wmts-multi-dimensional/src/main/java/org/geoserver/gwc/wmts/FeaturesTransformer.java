/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts;

import java.util.List;
import javax.xml.namespace.QName;
import org.geoserver.gwc.wmts.dimensions.Dimension;
import org.geoserver.gwc.wmts.dimensions.DimensionsUtils;
import org.geoserver.wms.WMS;
import org.geotools.feature.FeatureIterator;
import org.geotools.gml3.GMLConfiguration;
import org.geotools.xml.transform.TransformerBase;
import org.geotools.xml.transform.Translator;
import org.geotools.xsd.Encoder;
import org.locationtech.jts.geom.*;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.GeometryDescriptor;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;

/** XML transformer for the get feature operation. */
class FeaturesTransformer extends TransformerBase {

    public FeaturesTransformer(WMS wms) {
        setIndentation(2);
        setEncoding(wms.getCharSet());
    }

    @Override
    public Translator createTranslator(ContentHandler handler) {
        return new TranslatorSupport(handler);
    }

    class TranslatorSupport extends TransformerBase.TranslatorSupport {

        public TranslatorSupport(ContentHandler handler) {
            super(handler, null, null);
        }

        @Override
        public void encode(Object object) throws IllegalArgumentException {
            if (!(object instanceof Domains)) {
                throw new IllegalArgumentException(
                        "Expected domains info but instead got: "
                                + object.getClass().getCanonicalName());
            }
            Domains domains = (Domains) object;
            Attributes nameSpaces =
                    createAttributes(
                            new String[] {
                                "xmlns:xs", "http://www.w3.org/2001/XMLSchema",
                                "xmlns:gml", "http://www.opengis.net/gml",
                                "xmlns:wmts", "http://www.opengis.net/wmts/1.0"
                            });
            start("wmts:FeatureCollection", nameSpaces);
            FeatureIterator iterator = domains.getFeatureCollection().features();
            try {
                while (iterator.hasNext()) {
                    SimpleFeature simpleFeature = (SimpleFeature) iterator.next();
                    handleFeature(simpleFeature, domains.getDimensions());
                }
            } finally {
                iterator.close();
            }
            end("wmts:FeatureCollection");
        }

        /** Encodes a feature in the XML. */
        private void handleFeature(SimpleFeature feature, List<Dimension> dimensions) {
            Attributes attributes = createAttributes(new String[] {"gml:id", feature.getID()});
            start("wmts:feature", attributes);
            start("wmts:footprint");
            // encode the geometry
            GeometryDescriptor geometryDescriptor =
                    feature.getFeatureType().getGeometryDescriptor();
            Geometry geometry = (Geometry) feature.getAttribute(geometryDescriptor.getName());
            handleGeometry(geometry);
            // encode the dimensions
            end("wmts:footprint");
            for (Dimension dimension : dimensions) {
                handleDimension(feature, dimension);
            }
            end("wmts:feature");
        }

        /** Encodes a Geometry in GML. */
        private void handleGeometry(Geometry geometry) {
            try {
                QName elementName = org.geotools.gml2.GML._Geometry;
                if (geometry instanceof Point) {
                    elementName = org.geotools.gml2.GML.Point;
                } else if (geometry instanceof LineString) {
                    elementName = org.geotools.gml2.GML.LineString;
                } else if (geometry instanceof Polygon) {
                    elementName = org.geotools.gml2.GML.Polygon;
                } else if (geometry instanceof MultiPoint) {
                    elementName = org.geotools.gml2.GML.MultiPoint;
                } else if (geometry instanceof MultiLineString) {
                    elementName = org.geotools.gml2.GML.MultiLineString;
                } else if (geometry instanceof MultiPolygon) {
                    elementName = org.geotools.gml2.GML.MultiPolygon;
                }
                Encoder encoder = new Encoder(new GMLConfiguration());
                encoder.encode(geometry, elementName, contentHandler);
            } catch (Exception exception) {
                throw new RuntimeException(
                        "Cannot transform the specified geometry in GML.", exception);
            }
            ;
        }

        /** Encodes a dimension extracting the dimension value from the feature. */
        private void handleDimension(SimpleFeature feature, Dimension dimension) {
            Object value = feature.getAttribute(dimension.getAttributes().first);
            Attributes attributes =
                    createAttributes(new String[] {"name", dimension.getDimensionName()});
            element("wmts:dimension", DimensionsUtils.formatDomainValue(value), attributes);
        }
    }
}
