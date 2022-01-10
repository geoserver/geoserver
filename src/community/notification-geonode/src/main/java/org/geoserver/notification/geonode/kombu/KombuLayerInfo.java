/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.notification.geonode.kombu;

import org.geoserver.notification.common.Bounds;

public class KombuLayerInfo extends KombuPublishedInfo {

    Bounds geographicBunds;

    Bounds bounds;

    String defaultStyle;

    String styles;

    String resourceType;

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

    public String getDefaultStyle() {
        return defaultStyle;
    }

    public void setDefaultStyle(String defaultStyle) {
        this.defaultStyle = defaultStyle;
    }

    public String getStyles() {
        return styles;
    }

    public void setStyles(String styles) {
        this.styles = styles;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }
}
