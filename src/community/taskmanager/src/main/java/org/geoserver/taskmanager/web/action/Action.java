/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.web.action;

import org.geoserver.taskmanager.util.Named;
import org.geoserver.taskmanager.web.ConfigurationPage;

/**
 * 
 * @author Niels Charlier
 *
 */
public interface Action extends Named {
    
    void execute(ConfigurationPage onPage, String value);

}
