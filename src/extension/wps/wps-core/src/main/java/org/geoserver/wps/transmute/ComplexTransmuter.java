/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps.transmute;

import java.io.InputStream;
import org.geoserver.wps.WPSException;
import org.geotools.xsd.Configuration;
import org.geotools.xsd.Parser;
import org.locationtech.jts.geom.Geometry;

/**
 * ComplexTransmuter interface
 *
 * @author Lucas Reed, Refractions Research Inc
 */
public abstract class ComplexTransmuter implements Transmuter {
    /** Returns absolute URL to the schema which defines the in */
    public abstract String getSchema(String urlBase);

    /** Returns the class of the XMLConfiguration used to parse/encode */
    public abstract Class<?> getXMLConfiguration();

    /** Returns mime-type of encoded data */
    public abstract String getMimeType();

    /** Used to decode external XML documents for use as process inputs */
    public Object decode(InputStream stream) {
        Object decoded = null;
        Configuration config = null;

        try {
            config = (Configuration) (this.getXMLConfiguration().getConstructor().newInstance());
        } catch (Exception e) {
            throw new WPSException("NoApplicableCode", "Failed to initialize XMLConfiguration");
        }

        Parser parser = new Parser(config);

        try {
            decoded = (Geometry) parser.parse(stream);
        } catch (Exception e) {
            throw new WPSException("NoApplicableCode", "Parsing error " + e);
        }

        return decoded;
    }

    /** Used to encode document for server storage */
    public Object encode(Object input) {
        throw new WPSException("NoApplicableCode", "Unimplemented encoder for ComplexTransmuter.");
    }
}
