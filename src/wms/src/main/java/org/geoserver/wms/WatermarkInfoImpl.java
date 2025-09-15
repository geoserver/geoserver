/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import java.io.Serial;

public class WatermarkInfoImpl implements WatermarkInfo {

    @Serial
    private static final long serialVersionUID = -3306354572824960991L;

    boolean enabled;

    Position position = Position.BOT_RIGHT;

    int transparency = 100;

    String url;

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public Position getPosition() {
        return position;
    }

    @Override
    public void setPosition(Position position) {
        this.position = position;
    }

    @Override
    public int getTransparency() {
        return transparency;
    }

    @Override
    public void setTransparency(int transparency) {
        this.transparency = transparency;
    }

    @Override
    public String getURL() {
        return url;
    }

    @Override
    public void setURL(String url) {
        this.url = url;
    }
}
