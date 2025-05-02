/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.processes;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.geoserver.ogcapi.AbstractDocument;
import org.geoserver.wps.process.GeoServerProcessors;
import org.geotools.api.feature.type.Name;
import org.geotools.process.ProcessFactory;

/** List of processes provided by this server (eventually paged) */
public class ProcessListDocument extends AbstractDocument {

    List<ProcessSummaryDocument> processes;

    public ProcessListDocument() {
        // gather the process list
        processes = new ArrayList<>();
        Set<ProcessFactory> pfs = GeoServerProcessors.getProcessFactories();
        for (ProcessFactory pf : pfs) {
            for (Name name : pf.getNames()) {
                ProcessSummaryDocument summary = new ProcessSummaryDocument(pf, name);
                processes.add(summary);
            }
        }
        processes.sort((a, b) -> {
            String id1 = a.getId();
            String id2 = b.getId();
            return id1.compareTo(id2);
        });

        addSelfLinks("ogc/processes/v1/processes");

        // TODO: handle paging
    }

    public List<ProcessSummaryDocument> getProcesses() {
        return processes;
    }
}
