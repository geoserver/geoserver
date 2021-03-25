/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.data;

import org.geoserver.web.ComponentInfo;

/**
 * Extend this class in your extensions applicationContext to get instantiated in the
 * `AbstractStylePage` and get access to the style edit pages.
 */
public class StyleComponentInfo extends ComponentInfo {
    public StyleComponentInfo(String id, AbstractStylePage parent) {}

    public StyleComponentInfo(String id) {}

    public StyleComponentInfo() {}
}
