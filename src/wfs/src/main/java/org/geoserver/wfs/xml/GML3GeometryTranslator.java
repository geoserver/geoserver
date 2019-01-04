/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml;

import org.geotools.gml.producer.GeometryTransformer.GeometryTranslator;
import org.xml.sax.ContentHandler;

public class GML3GeometryTranslator extends GeometryTranslator {
    public GML3GeometryTranslator(ContentHandler handler) {
        super(handler);
    }

    public GML3GeometryTranslator(ContentHandler handler, int numDecimals, boolean useDummyZ) {
        super(handler, numDecimals, useDummyZ);
    }

    public GML3GeometryTranslator(ContentHandler handler, int numDecimals) {
        super(handler, numDecimals);
    }

    public GML3GeometryTranslator(
            ContentHandler handler,
            int numDecimals,
            boolean padWithZeros,
            boolean forceDecimalEncoding,
            boolean useDummyZ) {
        super(handler, numDecimals, padWithZeros, forceDecimalEncoding, useDummyZ);
    }

    public GML3GeometryTranslator(
            ContentHandler handler,
            int numDecimals,
            boolean padWithZeros,
            boolean forceDecimalEncoding) {
        super(handler, numDecimals, padWithZeros, forceDecimalEncoding);
    }

    protected String boxName() {
        return "Envelope";
    }

    protected void encodeNullBounds() {
        element("Null", null);
    }
}
