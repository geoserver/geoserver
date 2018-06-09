/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.process;

import java.util.Set;
import org.geotools.process.ProcessFactory;
import org.opengis.feature.type.Name;

public abstract class ProcessSelector implements ProcessFilter {

    @Override
    public ProcessFactory filterFactory(ProcessFactory pf) {
        // first check, do we want to return this factory at all?
        Set<Name> processNames = pf.getNames();
        int count = 0;
        for (Name processName : processNames) {
            if (allowProcess(processName)) {
                count++;
            }
        }

        if (count == 0) {
            // does it generate at least one process we are going to actually produce?
            // if not the factory itself is going to be filtered out
            return null;
        } else if (count == processNames.size()) {
            return pf;
        }

        return new SelectingProcessFactory(pf, this);
    }

    protected abstract boolean allowProcess(Name processName);
}
