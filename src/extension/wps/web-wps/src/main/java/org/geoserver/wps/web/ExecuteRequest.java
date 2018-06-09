/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple bean holding the Execute request parameters, to be used by {@link WPSExecuteTransformer}
 * in order to generate the Execute XML
 */
@SuppressWarnings("serial")
class ExecuteRequest implements Serializable {
    String processName;

    List<InputParameterValues> inputs;

    List<OutputParameter> outputs;

    public ExecuteRequest(
            String processName, List<InputParameterValues> inputs, List<OutputParameter> outputs) {
        this.processName = processName;
        this.inputs = inputs;
        this.outputs = outputs;
    }

    public ExecuteRequest() {
        this.processName = null;
        this.inputs = new ArrayList<InputParameterValues>();
        this.outputs = new ArrayList<OutputParameter>();
    }
}
