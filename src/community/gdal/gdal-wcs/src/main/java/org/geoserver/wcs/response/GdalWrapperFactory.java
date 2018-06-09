/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.response;

import java.util.Map;
import org.geoserver.ogr.core.ToolWrapper;
import org.geoserver.ogr.core.ToolWrapperFactory;

/**
 * Factory to create {@link GdalWrapper} instances.
 *
 * @author Stefano Costa, GeoSolutions
 */
public class GdalWrapperFactory implements ToolWrapperFactory {

    @Override
    public ToolWrapper createWrapper(String executable, Map<String, String> environment) {
        return new GdalWrapper(executable, environment);
    }
}
