/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.kvp.view;

import java.util.ArrayList;
import java.util.List;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class LayerParams {
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "P")
    private List<Parameter> parameters = new ArrayList<>();

    public LayerParams() {}

    public List<Parameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    @Override
    public String toString() {
        return "Params{" + "parameters=" + parameters + '}';
    }
}
