/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.external;

import java.util.List;

import org.geoserver.taskmanager.util.LookupServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Lookup service for external geoservers.
 * 
 * @author Niels Charlier
 *
 */
@Service
public class LookupGSServiceImpl extends LookupServiceImpl<ExternalGS> {
    
    @Autowired
    public void setExternalGS(List<ExternalGS> externalGS) {
        setNamed(externalGS);
    }
}
