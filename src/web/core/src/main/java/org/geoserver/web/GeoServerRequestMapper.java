package org.geoserver.web;

import org.apache.wicket.core.request.mapper.CryptoMapper;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.IRequestMapper;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Url;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.GeoServerSecurityManager;

public class GeoServerRequestMapper implements IRequestMapper {

    IRequestMapper defaultMapper;

    CryptoMapper cryptedMapper;

    GeoServerSecurityManager manager;

    public GeoServerRequestMapper(IRequestMapper defaultMapper, GeoServerApplication app) {
        this.cryptedMapper = new CryptoMapper(defaultMapper, app);
        this.defaultMapper = defaultMapper;
        this.manager = GeoServerExtensions.bean(GeoServerSecurityManager.class, app.getApplicationContext());
    }

    @Override
    public int getCompatibilityScore(Request req) {
        return Integer.MAX_VALUE;
    }

    @Override
    public Url mapHandler(IRequestHandler handler) {
        if(manager.isEncryptingUrlParams()) {
            return cryptedMapper.mapHandler(handler);
        } else {
            return defaultMapper.mapHandler(handler);
        }
    }

    @Override
    public IRequestHandler mapRequest(Request request) {
        if(manager.isEncryptingUrlParams()) {
            return cryptedMapper.mapRequest(request);
        } else {
            return defaultMapper.mapRequest(request);
        }
    }

}
