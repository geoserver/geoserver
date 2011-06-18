/*
 */

package org.geoserver.config.hib.types;

import org.geoserver.hibernate.types.EnumUserType;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.WFSInfo.Version;

/**
 * Hibernate user type for {@link WFSInfo.Version}.
 * @author ETj <etj at geo-solutions.it>
 */
public class WFSVersionType extends EnumUserType<WFSInfo.Version> {

    public WFSVersionType() {
        super(WFSInfo.Version.class);
    }

}
