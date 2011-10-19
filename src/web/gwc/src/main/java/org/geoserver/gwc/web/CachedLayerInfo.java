/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web;

import org.geowebcache.diskquota.storage.Quota;
import org.geowebcache.layer.TileLayer;

/**
 * {@link TileLayer} information view for {@link CachedLayerProvider}
 * 
 * @author groldan
 * 
 */
public class CachedLayerInfo {

    public static enum TYPE {
        VECTOR, RASTER, LAYERGROUP, WMS, OTHER;
    }

    private String name;

    private TYPE type;

    private boolean enabled;

    private Quota quotaLimit;

    private Quota quotaUsed;

    private String configErrorMessage;

    public String getName() {
        return name;
    }

    public TYPE getType() {
        return type;
    }

    public void setType(TYPE type) {
        this.type = type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Quota getQuotaLimit() {
        return quotaLimit;
    }

    public void setQuotaLimit(Quota quotaLimit) {
        this.quotaLimit = quotaLimit;
    }

    public Quota getQuotaUsed() {
        return quotaUsed;
    }

    public void setQuotaUsed(Quota quotaUsed) {
        this.quotaUsed = quotaUsed;
    }

    public String getConfigErrorMessage() {
        return this.configErrorMessage;
    }

    public void setConfigErrorMessage(String msg) {
        this.configErrorMessage = msg;
    }
}
