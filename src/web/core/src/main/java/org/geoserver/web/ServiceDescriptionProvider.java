/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import org.geoserver.web.ServicesPanel.ServiceDescription;
import org.geoserver.web.ServicesPanel.ServiceLinkDescription;

import java.util.Collections;
import java.util.List;

/**
 * Contributes service description and link information for global, workspace and layer services.
 *
 * @author Jody Garnett
 * @see ServicesPanel
 */
public interface ServiceDescriptionProvider {

    /**
     * Provides service descriptions, optionally filtered by workspace and layer.
     * <p>
     * Filtering is forgiving: provide the global services unless the workspace exactly matches;
     * provide workspace services unless the layer exactly matches.
     * </p>
     * @return service descriptions, may be empty if none available.
     */
    default List<ServiceDescription> getServices(String workspace, String layer){
        return Collections.emptyList();
    }

    /**
     * Provides service links, optionally filtered by workspace and layer.
     * <p>
     * Filtering is forgiving: provide the global services unless the workspace exactly matches;
     * provide workspace services unless the layer exactly matches.
     * </p>
     * @return service links, may be empty if none available.
     */
    default public List<ServiceLinkDescription> getServiceLinks(String workspace, String layer){
        return Collections.emptyList();
    }

}
