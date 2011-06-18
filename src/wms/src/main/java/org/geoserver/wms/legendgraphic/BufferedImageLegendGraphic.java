/* Copyright (c) 2010 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import java.awt.image.BufferedImage;

public class BufferedImageLegendGraphic {

    private BufferedImage legendGraphic;

    public BufferedImageLegendGraphic(final BufferedImage legendGraphic) {
        this.legendGraphic = legendGraphic;
    }

    public BufferedImage getLegend() {
        return legendGraphic;
    }
}
