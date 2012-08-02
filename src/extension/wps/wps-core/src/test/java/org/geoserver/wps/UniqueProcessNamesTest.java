package org.geoserver.wps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.geoserver.test.GeoServerTestSupport;
import org.geotools.process.ProcessFactory;
import org.geotools.process.Processors;
import org.opengis.feature.type.Name;

/**
 * Tests that the set of registered processes has unique names.
 * <p>
 * Duplicate names can happen if a process is defined in both the GeoServer wps-core
 * applicationContext.xml and in a GeoTools factory. (This isn't a functional problem, but is
 * confusing when displayed in the UI and listed in GetCapabilities).
 * 
 * 
 * @author Martin Davis, OpenGeo
 * 
 */
public class UniqueProcessNamesTest extends GeoServerTestSupport {

    public void testNamesUnique() throws Exception {
        List<String> procs = new ArrayList<String>();
        Set<String> uniqueProcs = new HashSet<String>();

        for (ProcessFactory pf : Processors.getProcessFactories()) {
            for (Name name : pf.getNames()) {
                String procName = name.getURI();
                procs.add(procName);
                uniqueProcs.add(procName);
            }
        }

        // remove duplicate names
        removeSingle(procs, uniqueProcs);
        if (procs.size() > 0) {
            System.out.println("Duplicate process names: " + procs);
        }
        assertTrue(procs.size() == 0);
    }

    private static void removeSingle(Collection<?> target, Collection<?> toRemove) {
        for (Object o : toRemove) {
            target.remove(o);
        }
    }
}
