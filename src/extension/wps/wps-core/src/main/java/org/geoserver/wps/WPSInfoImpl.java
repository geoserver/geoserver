/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps;

import org.geoserver.config.impl.ServiceInfoImpl;

/**
 * WPS information implementation
 *
 * @author Lucas Reed, Refractions Research Inc
 */
@SuppressWarnings("unchecked")
public class WPSInfoImpl extends ServiceInfoImpl implements WPSInfo {
    @Override
    public String getTitle() {
        return "Prototype GeoServer WPS";
    }
}