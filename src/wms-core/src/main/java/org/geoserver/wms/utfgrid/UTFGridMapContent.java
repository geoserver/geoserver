/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.utfgrid;

import java.awt.Color;
import org.geoserver.wms.WMSMapContent;

class UTFGridMapContent extends WMSMapContent {

    UTFGridEntries entries;
    WMSMapContent other;

    public UTFGridMapContent(WMSMapContent other, UTFGridEntries entries, int downscaleFactor) {
        // do not copy the layers, as we need to replace them (once we have a deep layer copy
        // we actually might not need this)
        super(other, false);
        this.entries = entries;
        this.other = other;

        // clean up the bits we surely don't want
        this.setMapWidth(this.getMapWidth() / downscaleFactor);
        this.setMapHeight(this.getMapHeight() / downscaleFactor);
        this.setBgColor(Color.BLACK);
        this.setTransparent(false);
        this.setPalette(null);
    }

    UTFGridEntries getEntries() {
        return entries;
    }

    @Override
    public void dispose() {
        try {
            // make it stop bitching about layers that would need to be disposed of
            // (which we don't need, and does not cause any leak)
            other.dispose();
        } finally {
            super.dispose();
        }
    }
}
