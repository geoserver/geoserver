/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.hib.types;

import org.geoserver.hibernate.types.EnumUserType;
import org.geoserver.wfs.WFSInfo.ServiceLevel;

/**
 * Hibernate user type for {@link WFSInfo.ServiceLevel}.
 * @author ETj <etj at geo-solutions.it>
 */
public class WFSServiceLevelType 
        extends EnumUserType<ServiceLevel>  {

    public WFSServiceLevelType() {
        super(ServiceLevel.class);
    }
}
