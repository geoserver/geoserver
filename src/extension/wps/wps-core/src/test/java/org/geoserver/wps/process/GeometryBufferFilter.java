/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.process;

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
