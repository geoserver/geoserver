/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.kvp.view;

import java.util.ArrayList;
import java.util.List;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "VP")
public class ViewParamsRoot {
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "PS")
    private List<LayerParams> layerParams = new ArrayList<>();

    public ViewParamsRoot() {}

    public List<LayerParams> getLayerParams() {
        return layerParams;
    }

    public void setLayerParams(List<LayerParams> layerParams) {
        this.layerParams = layerParams;
    }

    @Override
    public String toString() {
        return "ViewParamsRoot{" + "layerParams=" + layerParams + '}';
    }
}
