/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import net.sf.json.JSONObject;

/** @author ian */
public class JSONLegendGraphic implements LegendGraphic {

    private JSONObject legend;

    /** @param legendGraphic */
    public JSONLegendGraphic(JSONObject legendGraphic) {
        legend = legendGraphic;
    }

    @Override
    public JSONObject getLegend() {
        return legend;
    }
}
