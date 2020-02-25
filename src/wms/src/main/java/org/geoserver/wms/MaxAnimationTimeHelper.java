/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import org.geoserver.platform.ServiceException;

/**
 * Simple support class used to check max rendering time during animation loops
 *
 * @author Andrea Aime - GeoSolutions
 */
class MaxAnimationTimeHelper {

    private int maxRenderingTime;

    private long requestStart;

    public MaxAnimationTimeHelper(WMS wms, GetMapRequest request) {
        this.maxRenderingTime = wms.getMaxAnimationRenderingTime(request);
        if (maxRenderingTime > 0) {
            this.requestStart = System.currentTimeMillis();
        }
    }

    /** If the timeout has been reached, a {@link ServiceException} will be returned instead */
    public void checkTimeout() throws ServiceException {
        if (maxRenderingTime <= 0) {
            return;
        }
        final int elapsed = (int) (System.currentTimeMillis() - requestStart);
        int residual = maxRenderingTime - elapsed;
        if (residual <= 0) {
            throw new ServiceException(
                    "This animation request used more time than allowed and has been forcefully stopped. "
                            + "The max animation rendering time is "
                            + (maxRenderingTime / 1000.0)
                            + "s");
        }
    }
}
