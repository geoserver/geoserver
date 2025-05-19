/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import com.fasterxml.jackson.core.JsonGenerator;
import java.io.IOException;
import org.xml.sax.SAXException;

/** */
public interface JSONEncoderDelegate {

    /** Encodes the object into JSON format using the provided JsonGenerator. */
    public void encode(JsonGenerator generator) throws IOException, SAXException, Exception;
}
