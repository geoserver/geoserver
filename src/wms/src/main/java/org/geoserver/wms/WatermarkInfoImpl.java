/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

public class WatermarkInfoImpl implements WatermarkInfo {

    private static final long serialVersionUID = -3306354572824960991L;

    boolean enabled;

    Position position = Position.BOT_RIGHT;

    int transparency = 100;

    String url;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public int getTransparency() {
        return transparency;
    }

    public void setTransparency(int transparency) {
        this.transparency = transparency;
    }

    public String getURL() {
        return url;
    }

    public void setURL(String url) {
        this.url = url;
    }
}
