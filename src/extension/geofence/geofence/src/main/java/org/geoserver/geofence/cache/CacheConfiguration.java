/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.cache;

import com.google.common.base.Ticker;
import java.io.Serializable;

/** @author "Mauro Bartolomeoli - mauro.bartolomeoli@geo-solutions.it" */
public class CacheConfiguration implements Serializable, Cloneable {

    private long size = 100;

    private long refreshMilliSec = 15000;

    private long expireMilliSec = 30000;

    private volatile Ticker customTicker = null; // testing only

    public long getExpireMilliSec() {
        return expireMilliSec;
    }

    public void setExpireMilliSec(long expireMilliSec) {
        this.expireMilliSec = expireMilliSec;
    }

    public long getRefreshMilliSec() {
        return refreshMilliSec;
    }

    public void setRefreshMilliSec(long refreshMilliSec) {
        this.refreshMilliSec = refreshMilliSec;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public Ticker getCustomTicker() {
        return customTicker;
    }

    public void setCustomTicker(Ticker customTicker) {
        this.customTicker = customTicker;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
                + "[size="
                + size
                + " refrMsec="
                + refreshMilliSec
                + ", expMsec="
                + expireMilliSec
                + ']';
    }

    /** Creates a copy of the configuration object. */
    @Override
    public CacheConfiguration clone() {
        try {
            return (CacheConfiguration) super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new UnknownError("Unexpected exception: " + ex.getMessage());
        }
    }
}
