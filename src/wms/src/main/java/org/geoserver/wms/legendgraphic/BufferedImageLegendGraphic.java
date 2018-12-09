/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import java.awt.image.BufferedImage;

public class BufferedImageLegendGraphic implements LegendGraphic {

    private BufferedImage legendGraphic;

    public BufferedImageLegendGraphic(final BufferedImage legendGraphic) {
        this.legendGraphic = legendGraphic;
    }

    @Override
    public BufferedImage getLegend() {
        return legendGraphic;
    }
}
