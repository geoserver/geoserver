/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.kvp;

import org.geoserver.config.GeoServer;

public class TypeNamesKvpParser extends QNameNestedKvpParser {

    TypeNameKvpParser delegate;

    public TypeNamesKvpParser(String key, GeoServer gs) {
        super(key, gs.getCatalog());
        delegate = new TypeNameKvpParser(key, gs, gs.getCatalog());
    }

    @Override
    protected Object parseToken(String token) throws Exception {
        return delegate.parseToken(token);
    }
}
