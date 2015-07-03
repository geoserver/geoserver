/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.proxy;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.wicket.model.IModel;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.proxy.ProxyConfig;
import org.geoserver.web.wicket.GeoServerDataProvider;



/**
 * Provides a table model for listing layer groups
 */
//@SuppressWarnings("serial")
/*public class HostRegexProvider extends GeoServerDataProvider<Pattern> {
    
    public static Property<RegexInfo> REGEX = 
        new BeanProperty<RegexInfo>( "value", "value" );

    static List PROPERTIES = Arrays.asList(REGEX);
    
    @Override
    protected List<Pattern> getItems() {
        return ProxyConfig.loadConfFromDisk().hostnameWhitelist;
    }

    @Override
    protected List<Property<RegexInfo>> getProperties() {
        return PROPERTIES;
    }

    public IModel model(Object object) {
        return new RegexDetachableModel( (Pattern) object );
    }

}*/
