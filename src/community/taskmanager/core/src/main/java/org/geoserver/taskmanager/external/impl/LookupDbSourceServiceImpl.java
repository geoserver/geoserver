/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.external.impl;

import java.util.List;
import org.geoserver.taskmanager.external.DbSource;
import org.geoserver.taskmanager.util.SecuredLookupServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Look-up service for database sources.
 *
 * @author Niels Charlier
 */
@Service
public class LookupDbSourceServiceImpl extends SecuredLookupServiceImpl<DbSource> {

    @Autowired(required = false)
    public void setDbSources(List<DbSource> dbSources) {
        setNamed(dbSources);
    }
}
