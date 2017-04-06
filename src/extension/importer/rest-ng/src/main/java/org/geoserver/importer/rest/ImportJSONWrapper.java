/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest;

import net.sf.json.JSONObject;
import org.geoserver.importer.rest.converters.ImportJSONWriter;

import java.io.IOException;
import java.io.Writer;

public class ImportJSONWrapper implements ImportWrapper {
    JSONObject json;

    public ImportJSONWrapper(JSONObject json) {
        this.json = json;
    }

    @Override
    public void write(Writer writer, ImportJSONWriter converter) throws IOException {
        writer.write(json.toString());
    }
}
