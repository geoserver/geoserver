/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.json;

import org.geoserver.platform.NameExclusionFilter;

/**
 * Filter to disable/enable JsonP output format
 *
 * @author carlo cancellieri - GeoSolutions
 */
public class JSONPExtensionFilter extends NameExclusionFilter
        implements org.geoserver.platform.ExtensionFilter {

    public JSONPExtensionFilter(String beanId) {
        super();
        super.setBeanId(beanId);
    }

    @Override
    public boolean exclude(String beanId, Object bean) {
        return !JSONType.isJsonpEnabled() && super.exclude(beanId, bean);
    }
}
