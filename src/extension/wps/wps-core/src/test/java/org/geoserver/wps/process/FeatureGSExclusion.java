/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.process;

import java.util.HashSet;
import java.util.Set;
import org.geoserver.wps.DeprecatedProcessFactory;
import org.geoserver.wps.DisabledProcessesSelector;
import org.geotools.process.ProcessFactory;
import org.geotools.process.vector.VectorProcessFactory;
import org.opengis.feature.type.Name;

/**
 * Simple filter, excludes all the processes in the {@link FeatureGSProcessFactory}
 *
 * @author Andrea Aime - GeoSolutions
 */
public class FeatureGSExclusion implements ProcessFilter {

    @Override
    public ProcessFactory filterFactory(ProcessFactory pf) {
        if (pf instanceof VectorProcessFactory
                || (pf instanceof DelegatingProcessFactory
                        && ((DelegatingProcessFactory) pf).getInnermostDelegate()
                                instanceof VectorProcessFactory)) {
            return null;
        }
        if (pf instanceof DeprecatedProcessFactory) {
            // strip out all the "gs" processes

            Set<Name> disabled = new HashSet();
            for (Name n : pf.getNames()) {
                if ("gs".equals(n.getNamespaceURI())) {
                    disabled.add(n);
                }
            }

            return new SelectingProcessFactory(pf, new DisabledProcessesSelector(disabled));
        }
        return pf;
    }
}
