/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Service;
import org.geoserver.wfs.xml.v1_1_0.WFSConfiguration;

public abstract class WFS1XTestSupport extends WFSTestSupport {

    protected WFSConfiguration getXmlConfiguration11() {
        return (WFSConfiguration) applicationContext.getBean("wfsXmlConfiguration-1.1");
    }

    /** @return The 1.0 service descriptor. */
    protected Service getServiceDescriptor10() {
        return (Service) GeoServerExtensions.bean("wfsService-1.0.0");
    }

    /** @return The 1.1 service descriptor. */
    protected Service getServiceDescriptor11() {
        return (Service) GeoServerExtensions.bean("wfsService-1.1.0");
    }
}
