/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.processes;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.geotools.api.data.Parameter;
import org.geotools.util.logging.Logging;
import org.springframework.context.ApplicationContext;

/** Document describing a process, including its inputs and outputs */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProcessDocument extends ProcessSummaryDocument {

    private static final Logger LOGGER = Logging.getLogger(ProcessDocument.class);

    Map<String, ProcessInput> inputs;
    Map<String, ProcessOutput> outputs;

    public ProcessDocument(Process process, ApplicationContext context) {
        super(process);

        // collect the inputs
        inputs = new LinkedHashMap<>();
        Collection<String> outputMimeParameters = process.getOutputParameters().values();
        for (Parameter<?> p : process.getInputList()) {
            // skip the output mime choice params, they will be filled automatically by OGC API processes (TODO!)
            if (outputMimeParameters.contains(p.key)) {
                continue;
            }

            ProcessInput input = new ProcessInput(p, context);
            inputs.put(p.getName(), input);
        }

        // collect the outputs
        Map<String, Parameter<?>> outs = process.getResultInfo();
        outputs = new LinkedHashMap<>();
        for (Parameter p : outs.values()) {
            ProcessOutput output = new ProcessOutput(p, context);
            outputs.put(p.getName(), output);
        }
    }

    public Map<String, ProcessInput> getInputs() {
        return inputs;
    }

    public Map<String, ProcessOutput> getOutputs() {
        return outputs;
    }
}
