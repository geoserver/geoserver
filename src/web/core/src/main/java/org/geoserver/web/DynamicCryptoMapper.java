/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.web;

import org.apache.wicket.core.request.mapper.CryptoMapper;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.IRequestMapper;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.mapper.IRequestMapperDelegate;
import org.geoserver.security.GeoServerSecurityManager;

/**
 * Switches between a normal mapper that does not add hash segments at the end of the url making
 * bookmarkable url actually stateless, and a crypto one that does it all
 *
 * @author Andrea Aime - GeoSolutions
 */
class DynamicCryptoMapper implements IRequestMapperDelegate {

    private IRequestMapper plainMapper;
    private CryptoMapper cryptoMapper;
    private GeoServerSecurityManager securityManager;

    public DynamicCryptoMapper(
            IRequestMapper plainMapper,
            GeoServerSecurityManager securityManager,
            GeoServerApplication application) {
        this.securityManager = securityManager;
        this.plainMapper = plainMapper;
        this.securityManager = securityManager;
        // GeoServerCryptProvider cryptProvider = new GeoServerCryptProvider(securityManager);
        this.cryptoMapper = new CryptoMapper(plainMapper, application);
    }

    IRequestMapper getMapper() {
        if (securityManager.isEncryptingUrlParams()) {
            return cryptoMapper;
        } else {
            return plainMapper;
        }
    }

    public IRequestHandler mapRequest(Request request) {
        return getMapper().mapRequest(request);
    }

    public int getCompatibilityScore(Request request) {
        return getMapper().getCompatibilityScore(request);
    }

    public Url mapHandler(IRequestHandler requestHandler) {
        return getMapper().mapHandler(requestHandler);
    }

    @Override
    public IRequestMapper getDelegateMapper() {
        return getMapper();
    }
}
