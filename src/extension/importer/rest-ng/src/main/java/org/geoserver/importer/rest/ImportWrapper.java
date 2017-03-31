package org.geoserver.importer.rest;

import org.geoserver.importer.rest.converters.ImportContextJSONConverterWriter;

import java.io.IOException;
import java.io.Writer;

/**
 * Created by tbarsballe on 2017-03-31.
 */
public interface ImportWrapper {
    void write(Writer writer, ImportContextJSONConverterWriter converter) throws IOException;
}
