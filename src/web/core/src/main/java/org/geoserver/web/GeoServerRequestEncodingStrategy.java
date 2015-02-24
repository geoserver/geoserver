/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.web;

import org.apache.wicket.IRequestTarget;
import org.apache.wicket.Request;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.protocol.http.request.CryptedUrlWebRequestCodingStrategy;
import org.apache.wicket.protocol.http.request.WebRequestCodingStrategy;
import org.apache.wicket.request.IRequestCodingStrategy;
import org.apache.wicket.request.RequestParameters;
import org.apache.wicket.request.target.coding.IRequestTargetUrlCodingStrategy;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.GeoServerSecurityManager;

/**
 * Implementation of {@link IRequestCodingStrategy} multiplexing
 * between {@link WebRequestCodingStrategy} and {@link CryptedUrlWebRequestCodingStrategy}
 * 
 * @author christian
 *
 */
public class GeoServerRequestEncodingStrategy implements IRequestCodingStrategy {
    WebRequestCodingStrategy strategy;
    CryptedUrlWebRequestCodingStrategy cryptedStrategy;
    GeoServerSecurityManager manager; 
    
    public GeoServerRequestEncodingStrategy() {
        
        strategy=new WebRequestCodingStrategy();
        cryptedStrategy = new CryptedUrlWebRequestCodingStrategy(strategy);
        manager= GeoServerExtensions.bean(GeoServerSecurityManager.class);
    }


    @Override
    public void mount(IRequestTargetUrlCodingStrategy urlCodingStrategy) {
        
        if (manager.isEncryptingUrlParams())
           cryptedStrategy.mount(urlCodingStrategy);
        else
           strategy.mount(urlCodingStrategy);    
    }

    @Override
    public void addIgnoreMountPath(String path) {
        
        if (manager.isEncryptingUrlParams())
            cryptedStrategy.addIgnoreMountPath(path);
         else
            strategy.addIgnoreMountPath(path);    

    }

    @Override
    public CharSequence pathForTarget(IRequestTarget requestTarget) {
        if (manager.isEncryptingUrlParams())
            return cryptedStrategy.pathForTarget(requestTarget);
         else
            return strategy.pathForTarget(requestTarget);    
    }

    @Override
    public IRequestTarget targetForRequest(RequestParameters requestParameters) {
        if (manager.isEncryptingUrlParams())
            return cryptedStrategy.targetForRequest(requestParameters);
         else
            return strategy.targetForRequest(requestParameters);    

    }

    @Override
    public void unmount(String path) {
        if (manager.isEncryptingUrlParams())
            cryptedStrategy.unmount(path);
         else
            strategy.unmount(path);    
    }

    @Override
    public IRequestTargetUrlCodingStrategy urlCodingStrategyForPath(String path) {
        if (manager.isEncryptingUrlParams())
            return cryptedStrategy.urlCodingStrategyForPath(path);
         else
            return strategy.urlCodingStrategyForPath(path);    
    }

    @Override
    public RequestParameters decode(Request request) {
        if (manager.isEncryptingUrlParams())
            return cryptedStrategy.decode(request);
         else
            return strategy.decode(request);    
    }

    @Override
    public CharSequence encode(RequestCycle requestCycle, IRequestTarget requestTarget) {
        if (manager.isEncryptingUrlParams())
            return cryptedStrategy.encode(requestCycle, requestTarget);
         else
            return strategy.encode(requestCycle, requestTarget);    

    }

    @Override
    public String rewriteStaticRelativeUrl(String string) {
        if (manager.isEncryptingUrlParams())
            return cryptedStrategy.rewriteStaticRelativeUrl(string);
         else
            return strategy.rewriteStaticRelativeUrl(string);    

    }

}
