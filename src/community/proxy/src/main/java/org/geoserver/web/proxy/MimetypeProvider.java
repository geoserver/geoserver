/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.proxy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.geoserver.proxy.ProxyConfig;
import org.geoserver.web.wicket.GeoServerDataProvider;



/**
 * Provides a table model for listing layer groups
 */
@SuppressWarnings("serial")
public class MimetypeProvider extends GeoServerDataProvider<String> {
    
    public static Property<String> mimetypeProp = 
        new AbstractProperty<String>("mimetypeProp"){
        public String getPropertyValue(String string)
        {
            return string;
        }
    };

    static List<Property<String>> PROPERTIES = Arrays.asList(mimetypeProp);
    
    @Override
    protected List<String> getItems() {
        return new ArrayList<String>(ProxyConfig.loadConfFromDisk().mimetypeWhitelist);
    }

    @Override
    protected List<Property<String>> getProperties() {
        return PROPERTIES;
    }

}
