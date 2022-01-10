/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.notification.geonode.kombu;

import org.geoserver.notification.common.Bounds;

public abstract class KombuResourceInfo extends KombuWorkspaceItemInfo {
    private String nativeName;

    private String store;

    private Bounds geographicBunds;

    private Bounds bounds;

    public String getNativeName() {
        return nativeName;
    }

    public void setNativeName(String nativeName) {
        this.nativeName = nativeName;
    }

    public String getStore() {
        return store;
    }

    public void setStore(String store) {
        this.store = store;
    }

    public Bounds getGeographicBunds() {
        return geographicBunds;
    }

    public void setGeographicBunds(Bounds geographicBunds) {
        this.geographicBunds = geographicBunds;
    }

    public Bounds getBounds() {
        return bounds;
    }

    public void setBounds(Bounds bounds) {
        this.bounds = bounds;
    }
}
