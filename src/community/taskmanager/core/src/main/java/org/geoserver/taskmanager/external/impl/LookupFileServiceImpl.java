/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.external.impl;

import java.util.List;
import org.geoserver.taskmanager.external.FileService;
import org.geoserver.taskmanager.util.SecuredLookupServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Find all @FileService instance from the spring context. The Lookup service will also define S3
 * fileServices for each configuration in the S3-geotiff module.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 */
@Component
public class LookupFileServiceImpl extends SecuredLookupServiceImpl<FileService> {

    @Autowired(required = false)
    public void setFileServices(List<FileService> fileServices) {
        setNamed(fileServices);
    }
}
