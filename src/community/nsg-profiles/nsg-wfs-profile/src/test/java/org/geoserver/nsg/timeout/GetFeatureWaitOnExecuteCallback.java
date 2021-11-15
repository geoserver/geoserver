/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.nsg.timeout;

import java.io.IOException;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.GetFeatureCallback;
import org.geoserver.wfs.GetFeatureContext;

public class GetFeatureWaitOnExecuteCallback implements GetFeatureCallback {

    long delaySeconds = 0;

    @Override
    public void beforeQuerying(GetFeatureContext context) throws IOException, ServiceException {
        if (delaySeconds > 0) {
            try {
                System.out.println("Delaying response before querying");
                Thread.sleep(delaySeconds * 1000);
            } catch (InterruptedException e) {
                throw new RuntimeException("Unexpected wake up", e);
            }
        }
    }
}
