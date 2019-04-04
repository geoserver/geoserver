/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest;

import java.io.IOException;
import java.io.Writer;
import org.geoserver.importer.rest.converters.ImportJSONWriter;
import org.geoserver.importer.rest.converters.ImportJSONWriter.FlushableJSONBuilder;

/** Wrapper used by {@link ImportJSONWriter} to write custom json content */
public interface ImportWrapper {
    void write(Writer writer, FlushableJSONBuilder json, ImportJSONWriter builder)
            throws IOException;
}
