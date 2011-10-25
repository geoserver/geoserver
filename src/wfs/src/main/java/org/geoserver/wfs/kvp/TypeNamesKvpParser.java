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
