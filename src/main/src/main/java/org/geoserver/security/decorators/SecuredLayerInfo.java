/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.WrapperPolicy;

public class SecuredLayerInfo extends DecoratingLayerInfo {

    WrapperPolicy policy;

    public SecuredLayerInfo(LayerInfo delegate, WrapperPolicy policy) {
        super(delegate);
        this.policy = policy;
    }

    public WrapperPolicy getWrapperPolicy() {
        return policy;
    }

    @Override
    public ResourceInfo getResource() {
        ResourceInfo r = super.getResource();
        if (r == null) return null;
        else if (r instanceof FeatureTypeInfo)
            return (FeatureTypeInfo) SecuredObjects.secure(r, policy);
        else if (r instanceof CoverageInfo) return (CoverageInfo) SecuredObjects.secure(r, policy);
        else if (r instanceof WMSLayerInfo) return (WMSLayerInfo) SecuredObjects.secure(r, policy);
        else if (r instanceof WMTSLayerInfo)
            return (WMTSLayerInfo) SecuredObjects.secure(r, policy);
        else throw new RuntimeException("Don't know how to make resource of type " + r.getClass());
    }

    @Override
    public void setResource(ResourceInfo resource) {
        if (resource instanceof SecuredFeatureTypeInfo
                || resource instanceof SecuredCoverageInfo
                || resource instanceof SecuredWMSLayerInfo
                || resource instanceof SecuredWMTSLayerInfo) {
            resource = (ResourceInfo) SecureCatalogImpl.unwrap(resource);
        }

        delegate.setResource(resource);
    }
}
