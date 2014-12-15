/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.GeoServer;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.feature.NameImpl;
import org.geotools.util.Version;
import org.opengis.feature.type.Name;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;
import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.mapper.ClassAliasingMapper;
import com.thoughtworks.xstream.mapper.Mapper;

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
        // Use custom converter to manage previous wps.xml configuration format
        xs.registerConverter(new WPSXStreamConverter(xs.getMapper(), xs.getReflectionProvider()));
        xs.alias("wps", WPSInfo.class, WPSInfoImpl.class);
        xs.alias("processGroup", ProcessGroupInfoImpl.class);
        xs.alias("name", NameImpl.class);
        xs.alias("name", Name.class, NameImpl.class);
        xs.alias("accessInfo", ProcessInfoImpl.class);
        xs.registerConverter(new NameConverter());
        ClassAliasingMapper mapper = new ClassAliasingMapper(xs.getMapper());
        mapper.addClassAlias("role", String.class);
        xs.registerLocalConverter(ProcessGroupInfoImpl.class, "roles", new CollectionConverter(mapper));
        xs.registerLocalConverter(ProcessInfoImpl.class, "roles", new CollectionConverter(mapper));
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

    /**
     * Manages unmarshalling of {@link ProcessGroupInfoImpl} taking into account previous wps.xml
     * format in witch {@link ProcessGroupInfoImpl #getFilteredProcesses()} is a collection of
     * {@link NameImpl}
     */
    public static class WPSXStreamConverter extends ReflectionConverter {

        public WPSXStreamConverter(Mapper mapper, ReflectionProvider reflectionProvider) {
            super(mapper, reflectionProvider);
        }

        @Override
        public boolean canConvert(Class clazz) {
            return ProcessGroupInfoImpl.class == clazz;
        }

        @Override
        public Object doUnmarshal(Object result, HierarchicalStreamReader reader,
                UnmarshallingContext context) {
            ProcessGroupInfo converted = (ProcessGroupInfo) super.doUnmarshal(result, reader,
                    context);

            if (converted.getFilteredProcesses() != null) {
                List<ProcessInfo> newFilteredProcesses = new ArrayList<ProcessInfo>();
                for (Object fp : converted.getFilteredProcesses()) {
                    if (fp instanceof NameImpl) {
                        NameImpl ni = (NameImpl) fp;
                        ProcessInfo pi = new ProcessInfoImpl();
                        pi.setName(ni);
                        pi.setEnabled(false);
                        newFilteredProcesses.add(pi);
                    } else {
                        break;
                    }
                }
                if (!newFilteredProcesses.isEmpty()) {
                    converted.getFilteredProcesses().clear();
                    converted.getFilteredProcesses().addAll(newFilteredProcesses);
                }
            }

            return converted;
        }

    }
}
