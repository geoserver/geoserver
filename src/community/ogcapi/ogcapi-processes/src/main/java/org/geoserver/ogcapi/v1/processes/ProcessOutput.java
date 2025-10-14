/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.processes;

import org.geotools.api.data.Parameter;
import org.springframework.context.ApplicationContext;

/** Represents the output of a process in the OGC API Processes service. */
public class ProcessOutput extends AbstractProcessIO {

    public ProcessOutput(Parameter p, ApplicationContext context) {
        super(p, context);
    }
}
