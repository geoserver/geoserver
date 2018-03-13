/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.web.action;

import org.geoserver.taskmanager.web.ConfigurationPage;
import org.springframework.stereotype.Component;

/**
 * 
 * @author Niels Charlier
 *
 */
@Component
public class FileUploadAction implements Action {
    
    private static final long serialVersionUID = 4996136164811697150L;
    
    private final static String NAME = "FileUpload";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String execute(ConfigurationPage onPage, String value) {
        return value;
    }

}
