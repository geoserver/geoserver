package org.geoserver.importer.rest;

import net.sf.json.JSONObject;
import org.geoserver.importer.rest.converters.ImportContextJSONConverterWriter;

import java.io.IOException;
import java.io.Writer;

public class ImportJSONWrapper implements ImportWrapper {
    JSONObject json;

    public ImportJSONWrapper(JSONObject json) {
        this.json = json;
    }

    @Override
    public void write(Writer writer, ImportContextJSONConverterWriter converter) throws IOException {
        writer.write(json.toString());
    }
}
