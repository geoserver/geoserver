/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.GeoServer;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.config.util.XStreamPersister.SRSConverter;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.wfs.WFSInfoImpl;
import org.geotools.feature.NameImpl;
import org.geotools.process.ProcessFactory;
import org.geotools.process.Processors;
import org.geotools.referencing.CRS;
import org.geotools.referencing.wkt.Formattable;
import org.geotools.util.Version;
import org.opengis.feature.type.Name;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;

/**
 * Service loader for the Web Processing Service
 *
 * @author Lucas Reed, Refractions Research Inc
 * @author Justin Deoliveira, The Open Planning Project
 */
public class WPSXStreamLoader extends XStreamServiceLoader<WPSInfo> {
    public WPSXStreamLoader(GeoServerResourceLoader resourceLoader) {
        super(resourceLoader, "wps");
    }

    public Class<WPSInfo> getServiceClass() {
        return WPSInfo.class;
    }

    protected WPSInfo createServiceFromScratch(GeoServer gs) {
        WPSInfoImpl wps = new WPSInfoImpl();
        wps.setName("WPS");
        wps.setGeoServer( gs );
        wps.getVersions().add( new Version( "1.0.0") );
        wps.setMaxAsynchronousProcesses(Runtime.getRuntime().availableProcessors() * 2);
        wps.setMaxSynchronousProcesses(Runtime.getRuntime().availableProcessors() * 2);
        return wps;
    }
    
    @Override
    protected void initXStreamPersister(XStreamPersister xp, GeoServer gs) {
        XStream xs = xp.getXStream();
        xs.alias("wps", WPSInfo.class, WPSInfoImpl.class);
        xs.alias("processGroup", ProcessGroupInfoImpl.class);
        xs.alias("name", NameImpl.class);
        xs.registerConverter(new NameConverter());

        xs.allowTypeHierarchy(ProcessGroupInfo.class);
    }
    
    @Override
    protected WPSInfo initialize(WPSInfo service) {
        // TODO: move this code block to the parent class
        if ( service.getKeywords() == null ) {
            ((WPSInfoImpl)service).setKeywords( new ArrayList() );
        }
        if ( service.getExceptionFormats() == null ) {
            ((WPSInfoImpl)service).setExceptionFormats( new ArrayList() );
        }
        if ( service.getMetadata() == null ) {
            ((WPSInfoImpl)service).setMetadata( new MetadataMap() );
        }
        if ( service.getClientProperties() == null ) {
            ((WPSInfoImpl)service).setClientProperties( new HashMap() );
        }
        if ( service.getVersions() == null ) {
            ((WPSInfoImpl)service).setVersions( new ArrayList() );
        }
        if ( service.getVersions().isEmpty() ) {
            service.getVersions().add( new Version( "1.0.0") );
        }
        if (service.getConnectionTimeout() == 0) {
            // timeout has not yet been specified. Use default
            ((WPSInfoImpl)service).setConnectionTimeout(WPSInfoImpl.DEFAULT_CONNECTION_TIMEOUT);
        }
        if (service.getProcessGroups() == null) {
            ((WPSInfoImpl)service).setProcessGroups(new ArrayList());
        }
        if(service.getName() == null) {
            service.setName("WPS");
        }

        return service;
    }

    

    /**
     * Converter for {@link Name} 
     *
     */
    public static class NameConverter extends AbstractSingleValueConverter {

        @Override
        public boolean canConvert(Class type) {
            return Name.class.isAssignableFrom(type);
        }

        @Override
        public String toString(Object obj) {
            Name name = (Name) obj;
            return name.getNamespaceURI() + ":" + name.getLocalPart();
        }
        
        @Override
        public Object fromString(String str) {
            int idx =  str.indexOf(":");
            if(idx == -1) {
                return new NameImpl(str);
            } else {
                String prefix = str.substring(0, idx);
                String local = str.substring(idx + 1);
                return new NameImpl(prefix, local);
            }
        }
        
    }
}
