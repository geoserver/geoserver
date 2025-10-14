/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.geoserver.wps.process.GeoServerProcessors;
import org.geotools.api.feature.type.Name;
import org.geotools.process.ProcessFactory;
import org.junit.Test;

/**
 * Tests that the set of registered processes has unique names.
 *
 * <p>Duplicate names can happen if a process is defined in both the GeoServer wps-core applicationContext.xml and in a
 * GeoTools factory. (This isn't a functional problem, but is confusing when displayed in the UI and listed in
 * GetCapabilities).
 *
 * @author Martin Davis, OpenGeo
 */
public class UniqueProcessNamesTest extends WPSTestSupport {

    @Test
    public void testNamesUnique() throws Exception {
        List<String> procs = new ArrayList<>();
        Set<String> uniqueProcs = new HashSet<>();

        for (ProcessFactory pf : GeoServerProcessors.getProcessFactories()) {
            for (Name name : pf.getNames()) {
                String procName = name.getURI();
                procs.add(procName);
                uniqueProcs.add(procName);
            }
        }

        // remove duplicate names
        removeSingle(procs, uniqueProcs);
        assertEquals(0, procs.size());
    }

    private static void removeSingle(Collection<?> target, Collection<?> toRemove) {
        for (Object o : toRemove) {
            target.remove(o);
        }
    }
}
