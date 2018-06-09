/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.wps.ppio.ProcessParameterIO;
import org.geoserver.wps.process.ProcessSelector;
import org.geotools.data.Parameter;
import org.geotools.process.ProcessFactory;
import org.geotools.process.Processors;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.Name;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * A process filter that removes from the supported processes the ones that have inputs of outputs
 * we cannot deal with using the available {@link ProcessParameterIO} objects
 *
 * @author Andrea Aime - GeoSolutions
 */
public class UnsupportedParameterTypeProcessFilter extends ProcessSelector
        implements ApplicationContextAware {

    static final Logger LOGGER = Logging.getLogger(UnsupportedParameterTypeProcessFilter.class);

    private Set<Name> processBlacklist = new HashSet<Name>();

    @Override
    protected boolean allowProcess(Name processName) {
        return !processBlacklist.contains(processName);
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        processBlacklist.clear();

        for (ProcessFactory pf : Processors.getProcessFactories()) {
            int count = 0;
            for (Name name : pf.getNames()) {
                try {
                    // check inputs
                    for (Parameter<?> p : pf.getParameterInfo(name).values()) {
                        List<ProcessParameterIO> ppios = ProcessParameterIO.findAll(p, context);
                        if (ppios.isEmpty()) {
                            LOGGER.log(
                                    Level.INFO,
                                    "Blacklisting process "
                                            + name.getURI()
                                            + " as the input "
                                            + p.key
                                            + " of type "
                                            + p.type
                                            + " cannot be handled");
                            processBlacklist.add(name);
                        }
                    }

                    // check outputs
                    for (Parameter<?> p : pf.getResultInfo(name, null).values()) {
                        List<ProcessParameterIO> ppios = ProcessParameterIO.findAll(p, context);
                        if (ppios.isEmpty()) {
                            LOGGER.log(
                                    Level.INFO,
                                    "Blacklisting process "
                                            + name.getURI()
                                            + " as the output "
                                            + p.key
                                            + " of type "
                                            + p.type
                                            + " cannot be handled");
                            processBlacklist.add(name);
                        }
                    }
                } catch (Throwable t) {
                    processBlacklist.add(name);
                }

                if (!processBlacklist.contains(name)) {
                    count++;
                }
            }
            LOGGER.info("Found " + count + " bindable processes in " + pf.getTitle());
        }
    }
}
