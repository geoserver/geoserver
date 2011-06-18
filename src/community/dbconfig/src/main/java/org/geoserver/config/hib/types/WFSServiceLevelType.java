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
