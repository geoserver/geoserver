/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest;

import org.geoserver.importer.rest.converters.ImportContextJSONConverterWriter;

import java.io.IOException;
import java.io.Writer;

/**
 * Wrapper used by {@link ImportContextJSONConverterWriter} to write custom json content
 */
public interface ImportWrapper {
    void write(Writer writer, ImportContextJSONConverterWriter converter) throws IOException;
}
