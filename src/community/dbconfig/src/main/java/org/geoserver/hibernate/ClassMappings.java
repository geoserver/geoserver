/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.hibernate;

import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CoverageInfoImpl;
import org.geoserver.catalog.impl.CoverageStoreInfoImpl;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.LayerGroupInfoImpl;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.MapInfoImpl;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.catalog.impl.ResourceInfoImpl;
import org.geoserver.catalog.impl.StoreInfoImpl;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.catalog.impl.WMSLayerInfoImpl;
import org.geoserver.catalog.impl.WMSStoreInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.impl.GeoServerInfoImpl;
import org.geoserver.config.impl.LoggingInfoImpl;
import org.geoserver.config.impl.ServiceInfoImpl;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wcs.WCSInfoImpl;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.WFSInfoImpl;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSInfoImpl;

public enum ClassMappings {
    
    //
    // catalog
    //
    WORKSPACE {
        @Override public Class getInterface() { return WorkspaceInfo.class; }
        @Override public Class getImpl() { return WorkspaceInfoImpl.class; };
    }, 
    NAMESPACE {
        @Override public Class getInterface() { return NamespaceInfo.class; }
        @Override public Class getImpl() { return NamespaceInfoImpl.class; };
    },
    
    //stores, order matters
    DATASTORE {
        @Override public Class getInterface() { return DataStoreInfo.class; }
        @Override public Class getImpl() { return DataStoreInfoImpl.class; };
    },
    COVERAGESTORE {
        @Override public Class getInterface() { return CoverageStoreInfo.class; }
        @Override public Class getImpl() { return CoverageStoreInfoImpl.class; };
    },
    WMSSTORE {
        @Override public Class getInterface() { return WMSStoreInfo.class; }
        @Override public Class getImpl() { return WMSStoreInfoImpl.class; };
    },
    STORE {
        @Override public Class getInterface() { return StoreInfo.class; }
        @Override public Class getImpl() { return StoreInfoImpl.class; };
    },
    
    //resources, order matters
    FEATURETYPE {
        @Override public Class getInterface() { return FeatureTypeInfo.class; }
        @Override public Class getImpl() { return FeatureTypeInfoImpl.class; };
    },
    COVERAGE {
        @Override public Class getInterface() { return CoverageInfo.class; }
        @Override public Class getImpl() { return CoverageInfoImpl.class; };
    },
    WMSLAYER {
        @Override public Class getInterface() { return WMSLayerInfo.class; }
        @Override public Class getImpl() { return WMSLayerInfoImpl.class; };
    },
    RESOURCE {
        @Override public Class getInterface() { return ResourceInfo.class; }
        @Override public Class getImpl() { return ResourceInfoImpl.class; };
    },
    
    LAYER {
        @Override public Class getInterface() { return LayerInfo.class; }
        @Override public Class getImpl() { return LayerInfoImpl.class; };
    },
    LAYERGROUP {
        @Override public Class getInterface() { return LayerGroupInfo.class; }
        @Override public Class getImpl() { return LayerGroupInfoImpl.class; };
    },
    MAP {
        @Override public Class getInterface() { return MapInfo.class; }
        @Override public Class getImpl() { return MapInfoImpl.class; };
    },
    STYLE {
        @Override public Class getInterface() { return StyleInfo.class; }
        @Override public Class getImpl() { return StyleInfoImpl.class; };
    },
    
    //
    // config
    //
    GLOBAL {
        @Override public Class getInterface() { return GeoServerInfo.class; }
        @Override public Class getImpl() { return GeoServerInfoImpl.class; };
    }, 
    
    LOGGING {
        @Override public Class getInterface() { return LoggingInfo.class; }
        @Override public Class getImpl() { return LoggingInfoImpl.class; }; 
    },
    
    // services, order matters
    WMS {
        @Override public Class getInterface() { return WMSInfo.class; }
        @Override public Class getImpl() { return WMSInfoImpl.class; };
    }, 
    
    WFS {
        @Override public Class getInterface() { return WFSInfo.class; }
        @Override public Class getImpl() { return WFSInfoImpl.class; };
    }, 
    
    WCS {
        @Override public Class getInterface() { return WCSInfo.class; }
        @Override public Class getImpl() { return WCSInfoImpl.class; }; 
    }, 
    
    SERVICE {
        @Override public Class getInterface() { return ServiceInfo.class; }
        @Override public Class getImpl() { return ServiceInfoImpl.class; };  
    };
    
    
    public abstract Class getInterface();

    public abstract Class getImpl();

    public static ClassMappings fromInterface(Class interfce) {
        for(ClassMappings cm : values()) {
            if (interfce == cm.getInterface()) return cm;
        }
        return null;
    }
    
    public static ClassMappings fromImpl(Class impl) {
        for(ClassMappings cm : values()) {
            if (impl == cm.getImpl()) return cm;
        }
        return null;
    }
}
