package org.geoserver.wps.process;

import org.geotools.process.ProcessFactory;
import org.opengis.feature.type.Name;

/**
 * Simple select filter, excludes all the processes in the JTS process factory besides the "buffer"
 * one
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class GeometryBufferFilter extends ProcessSelector {
    
    @Override
    protected boolean allowProcess(Name processName) {
        if (!"JTS".equals(processName.getNamespaceURI())) {
            return true;
        } else {
            return "buffer".equals(processName.getLocalPart());
        }
    }
}
