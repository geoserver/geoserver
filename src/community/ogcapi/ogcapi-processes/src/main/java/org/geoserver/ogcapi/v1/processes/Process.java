/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.processes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.geoserver.ogcapi.APIException;
import org.geoserver.platform.ServiceException;
import org.geoserver.wps.process.AbstractRawData;
import org.geoserver.wps.process.GeoServerProcessors;
import org.geotools.api.data.Parameter;
import org.geotools.api.feature.type.Name;
import org.geotools.feature.NameImpl;
import org.geotools.process.ProcessFactory;
import org.springframework.http.HttpStatus;

/** Represents a process in OGC API Processes. It's the combination of a process factory and a process name */
public class Process {
    private Name name;
    private ProcessFactory pf;

    public Process(String processId) {
        this.name = toName(processId);
        this.pf = GeoServerProcessors.createProcessFactory(name, true);
        if (pf == null) {
            throw new APIException(
                    ServiceException.INVALID_PARAMETER_VALUE, "Process not found: " + processId, HttpStatus.NOT_FOUND);
        }
    }

    public Process(ProcessFactory pf, Name name) {
        this.name = name;
        this.pf = pf;
    }

    private static NameImpl toName(String processId) {
        int idx = processId.indexOf(':');
        if (idx < 0) return new NameImpl(processId);
        String namespace = processId.substring(0, idx);
        String localPart = processId.substring(idx + 1);
        return new NameImpl(namespace, localPart);
    }

    public Name getName() {
        return name;
    }

    public ProcessFactory getPf() {
        return pf;
    }

    public Map<String, String> getOutputParameters() {
        return AbstractRawData.getOutputMimeParameters(name, pf);
    }

    public Map<String, Parameter<?>> getInputMap() {
        return pf.getParameterInfo(name);
    }

    public List<Parameter<?>> getInputList() {
        return new ArrayList<>(pf.getParameterInfo(name).values());
    }

    public Map<String, Parameter<?>> getResultInfo() {
        return pf.getResultInfo(name, null);
    }

    public String getVersion() {
        return pf.getVersion(name);
    }

    public String getTitle() {
        return pf.getTitle(name).toString();
    }

    public String getDescription() {
        return pf.getDescription(name).toString();
    }
}
