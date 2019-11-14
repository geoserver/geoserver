/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jsonld.request;

import org.geoserver.ows.KvpParser;

public class JsonLdKvpParser extends KvpParser {

    public JsonLdKvpParser() {
        super("jsonld_filter", String.class);
    }

    @Override
    public Object parse(String value) throws Exception {
        return value.replaceAll("\\.", "/");
    }
}
