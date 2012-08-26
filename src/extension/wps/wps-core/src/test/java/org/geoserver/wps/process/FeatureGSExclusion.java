package org.geoserver.wps.process;

import org.geotools.process.ProcessFactory;
import org.geotools.process.feature.gs.FeatureGSProcessFactory;

/**
 * Simple filter, excludes all the processes in the {@link FeatureGSProcessFactory}
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class FeatureGSExclusion implements ProcessFilter {

    @Override
    public ProcessFactory filterFactory(ProcessFactory pf) {
        if (pf instanceof FeatureGSProcessFactory
                || (pf instanceof DelegatingProcessFactory && ((DelegatingProcessFactory) pf)
                        .getInnermostDelegate() instanceof FeatureGSProcessFactory)) {
            return null;
        }
        return pf;
    }

}
