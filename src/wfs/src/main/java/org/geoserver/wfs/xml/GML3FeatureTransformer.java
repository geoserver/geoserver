/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml;

import java.util.Set;
import org.geotools.gml.producer.FeatureTransformer;
import org.geotools.gml.producer.GeometryTransformer.GeometryTranslator;
import org.geotools.gml3.GML;
import org.opengis.feature.simple.SimpleFeature;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

public class GML3FeatureTransformer extends FeatureTransformer {
    protected FeatureTranslator createTranslator(
            ContentHandler handler,
            String prefix,
            String ns,
            FeatureTypeNamespaces featureTypeNamespaces,
            SchemaLocationSupport schemaLocationSupport) {
        return new GML3FeatureTranslator(
                handler, prefix, ns, featureTypeNamespaces, schemaLocationSupport);
    }

    protected void loadGmlAttributes(Set set) {
        set.add("name");
        set.add("description");
    }

    public class GML3FeatureTranslator extends FeatureTranslator {
        public GML3FeatureTranslator(
                ContentHandler handler,
                String prefix,
                String ns,
                FeatureTypeNamespaces featureTypeNamespaces,
                SchemaLocationSupport schemaLocationSupport) {
            super(handler, prefix, ns, featureTypeNamespaces, schemaLocationSupport);
        }

        protected GeometryTranslator createGeometryTranslator(ContentHandler handler) {
            return new GML3GeometryTranslator(handler);
        }

        protected GeometryTranslator createGeometryTranslator(
                ContentHandler handler,
                int numDecimals,
                boolean padWithZeros,
                boolean forceDecimalEncoding) {
            return new GML3GeometryTranslator(
                    handler, numDecimals, padWithZeros, forceDecimalEncoding);
        }

        protected GeometryTranslator createGeometryTranslator(
                ContentHandler handler,
                int numDecimals,
                boolean padWithZeros,
                boolean forceDecimalEncoding,
                boolean useDummyZ) {
            return new GML3GeometryTranslator(
                    handler, numDecimals, padWithZeros, forceDecimalEncoding, useDummyZ);
        }

        protected Attributes encodeFeatureId(SimpleFeature f) {
            AttributesImpl atts = new AttributesImpl();

            if (f.getID() != null) {
                atts.addAttribute(GML.NAMESPACE, "id", "gml:id", null, f.getID());
            }

            return atts;
        }

        protected String boxElement() {
            return "Envelope";
        }
    }
}
