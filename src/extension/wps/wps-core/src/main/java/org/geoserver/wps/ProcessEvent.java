/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import java.util.Map;
import org.geoserver.wps.executor.ExecutionStatus;

/** The context of an event triggered in a {@link ProcessListener} */
public class ProcessEvent implements Cloneable {

    private ExecutionStatus status;

    private Map<String, Object> inputs;

    private Map<String, Object> outputs;

    public ProcessEvent(ExecutionStatus status, Map<String, Object> inputs) {
        this.status = status;
        this.inputs = inputs;
    }

    public ProcessEvent(
            ExecutionStatus status, Map<String, Object> inputs, Map<String, Object> outputs) {
        this.status = status;
        this.inputs = inputs;
        this.outputs = outputs;
    }

    /** The process status. This is always available. */
    public ExecutionStatus getStatus() {
        return status;
    }

    /** The process inputs. This field is available only when the inputs have been parsed already */
    public Map<String, Object> getInputs() {
        return inputs;
    }

    /** The process outputs. The field is available only when the process is complete */
    public Map<String, Object> getOutputs() {
        return outputs;
    }
}
