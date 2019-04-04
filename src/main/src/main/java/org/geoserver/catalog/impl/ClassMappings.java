/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.impl.GeoServerInfoImpl;
import org.geoserver.config.impl.LoggingInfoImpl;
import org.geoserver.config.impl.ServiceInfoImpl;
import org.geoserver.config.impl.SettingsInfoImpl;

public enum ClassMappings {

    //
    // catalog
    //
    WORKSPACE {
        @Override
        public Class getInterface() {
            return WorkspaceInfo.class;
        }

        @Override
        public Class getImpl() {
            return WorkspaceInfoImpl.class;
        };
    },
    NAMESPACE {
        @Override
        public Class getInterface() {
            return NamespaceInfo.class;
        }

        @Override
        public Class getImpl() {
            return NamespaceInfoImpl.class;
        };
    },

    // stores, order matters
    DATASTORE {
        @Override
        public Class getInterface() {
            return DataStoreInfo.class;
        }

        @Override
        public Class getImpl() {
            return DataStoreInfoImpl.class;
        };
    },
    COVERAGESTORE {
        @Override
        public Class getInterface() {
            return CoverageStoreInfo.class;
        }

        @Override
        public Class getImpl() {
            return CoverageStoreInfoImpl.class;
        };
    },
    WMSSTORE {
        @Override
        public Class getInterface() {
            return WMSStoreInfo.class;
        }

        @Override
        public Class getImpl() {
            return WMSStoreInfoImpl.class;
        };
    },
    WMTSSTORE {
        @Override
        public Class getInterface() {
            return WMTSStoreInfo.class;
        }

        @Override
        public Class getImpl() {
            return WMTSStoreInfoImpl.class;
        };
    },
    STORE {
        @Override
        public Class getInterface() {
            return StoreInfo.class;
        }

        @Override
        public Class getImpl() {
            return StoreInfoImpl.class;
        };

        @Override
        public Class<? extends CatalogInfo>[] concreteInterfaces() {
            return new Class[] {
                CoverageStoreInfo.class,
                DataStoreInfo.class,
                WMSStoreInfo.class,
                WMTSStoreInfo.class
            };
        }
    },

    // resources, order matters
    FEATURETYPE {
        @Override
        public Class getInterface() {
            return FeatureTypeInfo.class;
        }

        @Override
        public Class getImpl() {
            return FeatureTypeInfoImpl.class;
        };
    },
    COVERAGE {
        @Override
        public Class getInterface() {
            return CoverageInfo.class;
        }

        @Override
        public Class getImpl() {
            return CoverageInfoImpl.class;
        };
    },
    WMSLAYER {
        @Override
        public Class getInterface() {
            return WMSLayerInfo.class;
        }

        @Override
        public Class getImpl() {
            return WMSLayerInfoImpl.class;
        };
    },
    WMTSLAYER {
        @Override
        public Class getInterface() {
            return WMTSLayerInfo.class;
        }

        @Override
        public Class getImpl() {
            return WMTSLayerInfoImpl.class;
        };
    },
    RESOURCE {
        @Override
        public Class getInterface() {
            return ResourceInfo.class;
        }

        @Override
        public Class getImpl() {
            return ResourceInfoImpl.class;
        };

        @Override
        public Class<? extends CatalogInfo>[] concreteInterfaces() {
            return new Class[] {
                CoverageInfo.class, FeatureTypeInfo.class, WMSLayerInfo.class, WMTSLayerInfo.class
            };
        }
    },
    PUBLISHED {
        @Override
        public Class getInterface() {
            return PublishedInfo.class;
        }

        @Override
        public Class getImpl() {
            return null;
        };

        @Override
        public Class<? extends CatalogInfo>[] concreteInterfaces() {
            return new Class[] {LayerInfo.class, LayerGroupInfo.class};
        }
    },
    LAYER {
        @Override
        public Class getInterface() {
            return LayerInfo.class;
        }

        @Override
        public Class getImpl() {
            return LayerInfoImpl.class;
        };
    },
    LAYERGROUP {
        @Override
        public Class getInterface() {
            return LayerGroupInfo.class;
        }

        @Override
        public Class getImpl() {
            return LayerGroupInfoImpl.class;
        };
    },
    MAP {
        @Override
        public Class getInterface() {
            return MapInfo.class;
        }

        @Override
        public Class getImpl() {
            return MapInfoImpl.class;
        };
    },
    STYLE {
        @Override
        public Class getInterface() {
            return StyleInfo.class;
        }

        @Override
        public Class getImpl() {
            return StyleInfoImpl.class;
        };
    },

    //
    // config
    //
    GLOBAL {
        @Override
        public Class getInterface() {
            return GeoServerInfo.class;
        }

        @Override
        public Class getImpl() {
            return GeoServerInfoImpl.class;
        };
    },

    LOGGING {
        @Override
        public Class getInterface() {
            return LoggingInfo.class;
        }

        @Override
        public Class getImpl() {
            return LoggingInfoImpl.class;
        };
    },

    SETTINGS {
        @Override
        public Class getInterface() {
            return SettingsInfo.class;
        }

        @Override
        public Class getImpl() {
            return SettingsInfoImpl.class;
        };
    },

    // // services, order matters
    // WMS {
    // @Override public Class getInterface() { return WMSInfo.class; }
    // @Override public Class getImpl() { return WMSInfoImpl.class; };
    // },
    //
    // WFS {
    // @Override public Class getInterface() { return WFSInfo.class; }
    // @Override public Class getImpl() { return WFSInfoImpl.class; };
    // },
    //
    // WCS {
    // @Override public Class getInterface() { return WCSInfo.class; }
    // @Override public Class getImpl() { return WCSInfoImpl.class; };
    // },

    SERVICE {
        @Override
        public Class getInterface() {
            return ServiceInfo.class;
        }

        @Override
        public Class getImpl() {
            return ServiceInfoImpl.class;
        };
    };

    public abstract <T> Class<T> getInterface();

    public abstract Class<? extends Info> getImpl();

    @SuppressWarnings({"rawtypes", "unchecked"})
    public Class<? extends Info>[] concreteInterfaces() {
        Class interf = getInterface();
        return new Class[] {interf};
    }

    public static ClassMappings fromInterface(Class<? extends Info> interfce) {
        if (ServiceInfo.class.isAssignableFrom(interfce)) {
            return SERVICE;
        }
        for (ClassMappings cm : values()) {
            if (interfce.equals(cm.getInterface())) {
                return cm;
            }
        }
        return null;
    }

    public static ClassMappings fromImpl(Class<?> clazz) {
        if (ServiceInfo.class.isAssignableFrom(clazz)) {
            return SERVICE;
        }
        for (ClassMappings cm : values()) {
            if (clazz == cm.getImpl()) return cm;
        }
        return null;
    }
}
