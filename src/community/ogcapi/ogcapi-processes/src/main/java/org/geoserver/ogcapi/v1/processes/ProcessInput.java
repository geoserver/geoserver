/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.processes;

import org.geotools.api.data.Parameter;
import org.springframework.context.ApplicationContext;

/** A class representing the input of a process in the OGC API Processes specification. */
public class ProcessInput extends AbstractProcessIO {

    public static final String UNBOUNDED = "unbounded";
    int minOccurs = 1;
    Object maxOccurs = UNBOUNDED; // can be either an integer or "unbounded"

    public ProcessInput(Parameter<?> p, ApplicationContext context) {
        super(p, context);
        this.minOccurs = p.getMinOccurs();
        this.maxOccurs = p.getMaxOccurs() == Integer.MAX_VALUE ? UNBOUNDED : p.getMaxOccurs();
    }

    public int getMinOccurs() {
        return minOccurs;
    }

    public void setMinOccurs(int minOccurs) {
        this.minOccurs = minOccurs;
    }

    public Object getMaxOccurs() {
        return maxOccurs;
    }

    public void setMaxOccurs(Object maxOccurs) {
        this.maxOccurs = maxOccurs;
    }
}
