/*
 *  Copyright (C) 2007 - 2014 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 *
 *  GPLv3 + Classpath exception
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
