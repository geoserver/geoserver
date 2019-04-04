/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogr.core;

import java.util.Map;

/**
 * Interface for tool wrapper factories.
 *
 * <p>Modules providing a {@link ToolWrapper} implementation, should also implement this interface.
 *
 * @author Stefano Costa, GeoSolutions
 */
public interface ToolWrapperFactory {

    /**
     * Creates a {@link ToolWrapper} instance.
     *
     * @param executable the wrapped executable
     * @param environment the environment variables that should be set prior to invoking the
     *     executable
     * @return a {@link ToolWrapper} instance
     */
    public ToolWrapper createWrapper(String executable, Map<String, String> environment);
}
