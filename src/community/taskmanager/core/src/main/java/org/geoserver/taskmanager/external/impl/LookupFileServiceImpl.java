/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.external.impl;

import org.geoserver.taskmanager.external.FileService;
import org.geoserver.taskmanager.util.LookupServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Find all @FileService instance from the spring context.
 * The Lookup service will also define S3 fileServices for each configuration in the S3-geotiff module.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 */
@Component
public class LookupFileServiceImpl extends LookupServiceImpl<FileService> {

    @Autowired(required = false)
    public void setFileServices(List<FileService> fileServices) {
        setNamed(fileServices);
    }
}
