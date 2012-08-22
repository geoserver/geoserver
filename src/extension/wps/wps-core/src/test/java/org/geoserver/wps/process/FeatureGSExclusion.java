package org.geoserver.wps.process;

import org.geotools.process.ProcessFactory;
import org.geotools.process.vector.VectorProcessFactory;

/**
 * Simple filter, excludes all the processes in the {@link FeatureGSProcessFactory}
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class FeatureGSExclusion implements ProcessFilter {

    @Override
    public ProcessFactory filterFactory(ProcessFactory pf) {
        if (pf instanceof VectorProcessFactory
                || (pf instanceof DelegatingProcessFactory && ((DelegatingProcessFactory) pf)
                        .getInnermostDelegate() instanceof VectorProcessFactory)) {
            return null;
        }
        return pf;
    }

}
