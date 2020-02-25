/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.process;

import java.io.IOException;
import java.io.InputStream;

/**
 * Represent a raw input or a raw output for a process
 *
 * @author Andrea Aime - GeoSolutions
 */
public interface RawData {

    /** Returns the mime type of the stream's contents */
    public String getMimeType();

    /**
     * Gives access to the raw data contents. TODO: decide if this one may be called only once, or
     * if the code should make it possible to extract the stream multiple times
     */
    public InputStream getInputStream() throws IOException;

    /** Optional field for output raw data, used by WPS to generate a file extension */
    public String getFileExtension();
}
