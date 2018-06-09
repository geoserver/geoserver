/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps.ppio;

import java.util.List;

public interface PPIOFactory {

    /**
     * Returns a list of process parameter IO. This method will be called every time a PPIO is
     * looked up , so implementors are required to implement suitable caching if the creation of
     * these objects is expensive
     */
    List<ProcessParameterIO> getProcessParameterIO();
}
