/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.process;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.process.Process;
import org.geotools.process.ProcessFactory;
import org.geotools.process.Processors;
import org.opengis.feature.type.Name;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * GeoServer replacement for GeoTools's {@link Processors} class, it allow {@link ProcessFilter} to
 * be taken into account before creating factories and processes
 *
 * @author Andrea Aime - GeoSolutions
 */
public class GeoServerProcessors implements ApplicationContextAware {

    private static List<ProcessFilter> filters;

    @Override
    public void setApplicationContext(ApplicationContext appContext) throws BeansException {
        filters = GeoServerExtensions.extensions(ProcessFilter.class, appContext);
    }

    /**
     * Set of available ProcessFactory, each eventually wrapped or filtered out by the registered
     * {@link ProcessFilter}
     *
     * @return Set of ProcessFactory
     */
    public static Set<ProcessFactory> getProcessFactories() {
        Set<ProcessFactory> factories = Processors.getProcessFactories();
        Set<ProcessFactory> result = new LinkedHashSet<ProcessFactory>();

        // scan filters and let them wrap and exclude as necessary
        for (ProcessFactory pf : factories) {
            pf = applyFilters(pf);
            if (pf != null) {
                result.add(pf);
            }
        }

        return result;
    }

    private static ProcessFactory applyFilters(ProcessFactory pf) {
        if (pf == null) {
            return null;
        }
        if (filters != null) {
            for (ProcessFilter filter : filters) {
                pf = filter.filterFactory(pf);
                if (pf == null) {
                    break;
                }
            }
        }
        return pf;
    }

    /**
     * Look up a Factory by name of a process it supports.
     *
     * @param name Name of the Process you wish to work with
     * @param applyFilters Whether to apply the available {@link ProcessFilter} to the returned
     *     factory, or not (if the code needs to check the original process factory by class name
     *     for example, better not to apply the filters, which often wrap the factories to add extra
     *     functionality)
     * @return ProcessFactory capable of creating an instanceof the named process
     */
    public static ProcessFactory createProcessFactory(Name name, boolean applyFilters) {
        ProcessFactory pf = Processors.createProcessFactory(name);
        if (applyFilters) {
            pf = applyFilters(pf);
        }
        // JD: also check the names, this could be a filtered process factory with only a subset
        // disabled
        if (pf != null && !pf.getNames().contains(name)) {
            pf = null;
        }
        return pf;
    }

    /**
     * Look up an implementation of the named process.
     *
     * @param name Name of the Process to create
     * @return created process or null if not found
     */
    public static Process createProcess(Name name) {
        ProcessFactory factory = createProcessFactory(name, false);
        if (factory == null) return null;

        return factory.create(name);
    }

    /**
     * Returns the process factory instance corresponding to the specified class.
     *
     * @param factoryClass The factory to look for
     * @param applyFilters Whether to apply the registered {@link ProcessFilter} instances, or not
     */
    public static ProcessFactory getProcessFactory(Class factoryClass, boolean applyFilters) {
        Set<ProcessFactory> factories = Processors.getProcessFactories();
        for (ProcessFactory pf : factories) {
            if (factoryClass.equals(pf.getClass())) {
                if (!applyFilters) {
                    return pf;
                } else {
                    // scan filters and let them wrap as necessary
                    pf = applyFilters(pf);

                    return pf;
                }
            }
        }

        // not found
        return null;
    }
}
