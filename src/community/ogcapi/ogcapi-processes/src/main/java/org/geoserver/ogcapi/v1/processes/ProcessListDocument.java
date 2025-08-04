/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.processes;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.geoserver.ogcapi.APIException;
import org.geoserver.ogcapi.AbstractDocument;
import org.geoserver.ogcapi.Link;
import org.geoserver.ogcapi.LinksBuilder;
import org.geoserver.wps.process.GeoServerProcessors;
import org.geotools.api.feature.type.Name;
import org.geotools.process.ProcessFactory;
import org.springframework.http.HttpStatus;

/** List of processes provided by this server (eventually paged) */
public class ProcessListDocument extends AbstractDocument {

    public static final String PROCESSES_PATH = "ogc/processes/v1/processes";
    List<ProcessSummaryDocument> processes;

    public ProcessListDocument(Integer originalOffset, Integer originalLimit) {
        // gather the process list
        processes = new ArrayList<>();
        Set<ProcessFactory> pfs = GeoServerProcessors.getProcessFactories();
        for (ProcessFactory pf : pfs) {
            for (Name name : pf.getNames()) {
                ProcessSummaryDocument summary = new ProcessSummaryDocument(new Process(pf, name));
                processes.add(summary);
            }
        }
        // predictable output order
        processes.sort((a, b) -> {
            String id1 = a.getId();
            String id2 = b.getId();
            return id1.compareTo(id2);
        });

        addSelfLinks(PROCESSES_PATH);

        // paging support
        if (originalOffset != null || originalLimit != null) {
            // offset checks
            int allProcessCount = processes.size();
            int offset;
            if (originalOffset == null) {
                offset = 0;
            } else if (originalOffset > allProcessCount) {
                throw new APIException(
                        APIException.INVALID_PARAMETER_VALUE,
                        "Offset must be less than the total number of processes, which is %d. Offset was: %d"
                                .formatted(allProcessCount, originalOffset),
                        HttpStatus.BAD_REQUEST);
            } else {
                offset = originalOffset;
            }

            // limit checks
            int limit;
            if (originalLimit == null || originalLimit + offset > allProcessCount) {
                limit = allProcessCount - offset;
            } else {
                limit = originalLimit;
            }

            // what's the last process in this page?
            int lastProcess = Math.min(allProcessCount, offset + limit);
            processes = processes.subList(offset, lastProcess);

            // if we have more processes, add a next link
            int remaining = allProcessCount - lastProcess;
            if (remaining > 0) {
                new LinksBuilder(getClass(), PROCESSES_PATH)
                        .param("limit", String.valueOf(originalLimit))
                        .param("offset", String.valueOf(lastProcess))
                        .classification("next") // for HTML usage
                        .rel(Link.REL_NEXT)
                        .build()
                        .forEach(this::addLink);
            }
            if (offset > 0) {
                int newLimit = originalLimit != null ? originalLimit : limit;
                int newOffset = Math.max(0, offset - newLimit);
                // if we have a previous page, add a prev link
                new LinksBuilder(getClass(), PROCESSES_PATH)
                        .param("limit", String.valueOf(newLimit))
                        .param("offset", String.valueOf(newOffset))
                        .classification("prev") // for HTML usage
                        .rel(Link.REL_PREV)
                        .build()
                        .forEach(this::addLink);
            }
        }
    }

    public List<ProcessSummaryDocument> getProcesses() {
        return processes;
    }
}
