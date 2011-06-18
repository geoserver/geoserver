/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Holds the pre-validated parameters of a <code>DescribeLayer</code> request.
 * 
 * <p>
 * This pre-validation must to be done by the request reader, so the content of this object is
 * assumed to be valid.
 * </p>
 * 
 * @author Gabriel Roldan
 * @version $Id$
 */
public class DescribeLayerRequest extends WMSRequest {

    /**
     * Holds the FeatureTypes parsed from the request's <code>LAYERS</code> parameter.
     */
    private List<MapLayerInfo> layers = new ArrayList<MapLayerInfo>(2);

    public DescribeLayerRequest() {
        super("DescribeLayer");
    }

    public void addLayer(MapLayerInfo layer) {
        if (layer == null) {
            throw new NullPointerException();
        }

        layers.add(layer);
    }

    public List<MapLayerInfo> getLayers() {
        return new ArrayList<MapLayerInfo>(layers);
    }

    public void setLayers(List<MapLayerInfo> layers) {
        this.layers = layers;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("DescribeLayerRequest[layers=");

        for (Iterator<MapLayerInfo> it = layers.iterator(); it.hasNext();) {
            sb.append(((MapLayerInfo) it.next()).getName());

            if (it.hasNext()) {
                sb.append(',');
            }
        }

        sb.append(']');

        return sb.toString();
    }
}
