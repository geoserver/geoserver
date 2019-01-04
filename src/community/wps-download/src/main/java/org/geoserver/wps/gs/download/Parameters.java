/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.apache.commons.lang3.builder.ToStringBuilder;

@XmlRootElement(name = "Parameters")
public class Parameters {

    public static final String NAMESPACE = "http://geoserver.org/wps/download";

    List<Parameter> parameters = new ArrayList<>();

    @XmlElement(name = "Parameter")
    public List<Parameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    public Map<String, String> getParametersMap() {
        Map<String, String> result = new HashMap<>();
        for (Parameter parameter : parameters) {
            result.put(parameter.key, parameter.value);
        }

        return result;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
