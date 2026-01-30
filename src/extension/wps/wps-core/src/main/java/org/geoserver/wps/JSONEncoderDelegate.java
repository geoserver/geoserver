/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import java.io.IOException;
import org.xml.sax.SAXException;
import tools.jackson.core.JsonGenerator;

/** */
public interface JSONEncoderDelegate {

    /** Encodes the object into JSON format using the provided JsonGenerator. */
    public void encode(JsonGenerator generator) throws IOException, SAXException, Exception;
}
