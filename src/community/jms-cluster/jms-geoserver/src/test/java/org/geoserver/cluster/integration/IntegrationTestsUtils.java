/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.integration;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.Serializable;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.SerializationUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.ows.util.ClassProperties;
import org.geoserver.ows.util.OwsUtils;

/** Contains some util methods used in JMS integration tests. */
public final class IntegrationTestsUtils {

    private static final Logger LOGGER = Logger.getLogger(IntegrationTestsUtils.class.getName());

    private IntegrationTestsUtils() {}

    /** Equalizes all GeoServer instances to the first instance. */
    public static void equalizeInstances(GeoServerInstance... instances) {
        if (instances.length <= 1) {
            // only one instance, so nothing to equalize
            return;
        }
        // get the instance that will be used as reference
        GeoServerInstance instance = instances[0];
        for (int i = 1; i < instances.length; i++) {
            // equalize instance to the reference instance
            equalize(instance, instances[i]);
        }
    }

    /** Equalizes two GeoServer instances, the first instance will be used as reference. */
    public static void equalize(GeoServerInstance instanceA, GeoServerInstance instanceB) {
        // get the differences between the two instances
        List<InfoDiff> differences = differences(instanceA, instanceB);
        // equalize each difference
        for (InfoDiff difference : differences) {
            Info infoA = difference.getInfoA();
            Info infoB = difference.getInfoB();
            if (infoA == null) {
                // this info doesn't exists in the reference instance, so it needs to be removed
                remove(instanceB.getGeoServer(), instanceB.getCatalog(), infoB);
            } else if (infoB == null) {
                // this info exists only in the reference instance so it needs to be added
                add(
                        instanceB.getGeoServer(),
                        instanceB.getCatalog(),
                        (Info) SerializationUtils.clone(infoA));
            } else {
                // this info exists in both instances but is different
                save(
                        instanceB.getGeoServer(),
                        instanceB.getCatalog(),
                        ModificationProxy.unwrap(infoA),
                        ModificationProxy.unwrap(infoB));
            }
        }
    }

    /**
     * Helper method that checks that provided instances are the same. The first instance is used as
     * reference.
     */
    public static void checkNoDifferences(GeoServerInstance... instances) {
        if (instances.length <= 1) {
            // only one instance, so noting to check
            return;
        }
        // get the reference instance
        GeoServerInstance instance = instances[0];
        for (int i = 1; i < instances.length; i++) {
            // get differences between this instance and the reference instance
            List<InfoDiff> differences = differences(instance, instances[i]);
            assertThat(differences.size(), is(0));
        }
    }

    /** Returns the catalog and configuration differences between two GeoServer instance. */
    public static List<InfoDiff> differences(
            GeoServerInstance instanceA, GeoServerInstance instanceB) {
        List<InfoDiff> differences = new ArrayList<>();
        // get catalog differences
        differences.addAll(catalogDifferences(instanceA, instanceB));
        // get configuration differences
        differences.addAll(configurationDifferences(instanceA, instanceB));
        return differences;
    }

    /** Returns the catalog differences between two GeoServer instances. */
    public static List<InfoDiff> catalogDifferences(
            GeoServerInstance instanceA, GeoServerInstance instanceB) {
        // instantiate the differences visitor
        CatalogDiffVisitor visitor =
                new CatalogDiffVisitor(
                        instanceB.getCatalog(),
                        instanceA.getDataDirectory(),
                        instanceB.getDataDirectory());
        // visit the two catalogs
        instanceA.getCatalog().accept(visitor);
        // return the found differences
        return visitor.differences();
    }

    /** Return the configuration differences between two GeoServer instances. */
    public static List<InfoDiff> configurationDifferences(
            GeoServerInstance instanceA, GeoServerInstance instanceB) {
        ConfigurationDiffVisitor visitor =
                new ConfigurationDiffVisitor(instanceA.getGeoServer(), instanceB.getGeoServer());
        return visitor.differences();
    }

    /** Helper method that just reset the JMS configuration of the provided GeoServe instances. */
    public static void resetJmsConfiguration(GeoServerInstance... instances) {
        Arrays.stream(instances).forEach(GeoServerInstance::setJmsDefaultConfiguration);
    }

    /**
     * Helper method that just reset the count of consumed events of the provided GeoServe
     * instances.
     */
    public static void resetEventsCount(GeoServerInstance... instances) {
        Arrays.stream(instances).forEach(GeoServerInstance::resetConsumedEventsCount);
    }

    /** Equalizes the provided infos using info A as reference. */
    private static void save(GeoServer geoServer, Catalog catalog, Info infoA, Info infoB) {
        if (infoA instanceof WorkspaceInfo) {
            catalog.save(updateInfoImpl(infoA, infoB, WorkspaceInfo.class));
        } else if (infoA instanceof NamespaceInfo) {
            catalog.save(updateInfoImpl(infoA, infoB, NamespaceInfo.class));
        } else if (infoA instanceof DataStoreInfo) {
            catalog.save(updateInfoImpl(infoA, infoB, DataStoreInfo.class));
        } else if (infoA instanceof CoverageStoreInfo) {
            catalog.save(updateInfoImpl(infoA, infoB, CoverageStoreInfo.class));
        } else if (infoA instanceof WMSStoreInfo) {
            catalog.save(updateInfoImpl(infoA, infoB, WMSStoreInfo.class));
        } else if (infoA instanceof FeatureTypeInfo) {
            catalog.save(updateInfoImpl(infoA, infoB, FeatureTypeInfo.class));
        } else if (infoA instanceof CoverageInfo) {
            catalog.save(updateInfoImpl(infoA, infoB, CoverageInfo.class));
        } else if (infoA instanceof LayerInfo) {
            catalog.save(updateInfoImpl(infoA, infoB, LayerInfo.class));
        } else if (infoA instanceof StyleInfo) {
            catalog.save(updateInfoImpl(infoA, infoB, StyleInfo.class));
        } else if (infoA instanceof LayerGroupInfo) {
            catalog.save(updateInfoImpl(infoA, infoB, LayerGroupInfo.class));
        } else if (infoA instanceof WMSLayerInfo) {
            catalog.save(updateInfoImpl(infoA, infoB, WMSLayerInfo.class));
        } else if (infoA instanceof SettingsInfo) {
            geoServer.save(updateInfoImpl(infoA, infoB, SettingsInfo.class));
        } else if (infoA instanceof ServiceInfo) {
            geoServer.save(updateInfoImpl(infoA, infoB, ServiceInfo.class));
        } else if (infoA instanceof LoggingInfo) {
            geoServer.save(updateInfoImpl(infoA, infoB, LoggingInfo.class));
        } else if (infoA instanceof GeoServerInfo) {
            geoServer.save(updateInfoImpl(infoA, infoB, GeoServerInfo.class));
        } else {
            throw new RuntimeException(
                    String.format(
                            "Don't know how to handle info of type '%s'.",
                            infoA.getClass().getSimpleName()));
        }
    }

    /**
     * Updates the second info values using the first info values. Values are cloned when possible.
     */
    private static <U> U updateInfoImpl(Info infoA, Info infoB, Class<U> type) {
        // make sure that we are dealing with infos that are compatible
        if (!type.isAssignableFrom(infoA.getClass()) || !type.isAssignableFrom(infoB.getClass())) {
            throw new RuntimeException(
                    String.format(
                            "Info objects should be of type '%s', but are of types '%s' and '%s'.",
                            type.getSimpleName(),
                            infoA.getClass().getSimpleName(),
                            infoB.getClass().getSimpleName()));
        }
        // create a modification proxy for the second info
        U proxy = ModificationProxy.create(type.cast(infoB), type);
        // get infos properties
        ClassProperties properties = OwsUtils.getClassProperties(type);
        // update every property of the second info using first info value
        for (String propertyName : properties.properties()) {
            try {
                // get first info value for the current property
                Object propertyValue = OwsUtils.get(infoA, propertyName);
                if (propertyValue instanceof Info) {
                    // we are dealing with an info object, check that both properties are compatible
                    Object otherPropertyValue = OwsUtils.get(infoB, propertyName);
                    if (otherPropertyValue instanceof Info) {
                        // recursively update this info
                        propertyValue =
                                updateInfoImpl(
                                        (Info) propertyValue,
                                        (Info) otherPropertyValue,
                                        getInfoInterface(propertyValue.getClass()));
                    }
                }
                // if the property value is not a info clone it if possible
                if (propertyValue instanceof Serializable && !(propertyValue instanceof Proxy)) {
                    propertyValue = SerializationUtils.clone((Serializable) propertyValue);
                }
                // update second info value
                OwsUtils.set(proxy, propertyName, propertyValue);
            } catch (IllegalArgumentException exception) {
                // ignore non existing property
                LOGGER.log(
                        Level.FINE,
                        String.format("Error setting property '%s'.", propertyName),
                        exception);
            }
        }
        // return modification proxy of second info
        return proxy;
    }

    /** Helper method that find the interface of an implementation. */
    private static Class<?> getInfoInterface(Class<?> type) {
        Class<?>[] classInterfaces = type.getInterfaces();
        for (Class<?> classInterface : classInterfaces) {
            if (Info.class.isAssignableFrom(classInterface)) {
                // compatible interface found, let's use this one
                return classInterface;
            }
        }
        // no compatible interface found
        return Info.class;
    }

    /** Add info object to the provided GeoServer instance. */
    private static void add(GeoServer geoServer, Catalog catalog, Info info) {
        if (info instanceof WorkspaceInfo) {
            catalog.add((WorkspaceInfo) info);
        } else if (info instanceof NamespaceInfo) {
            catalog.add((NamespaceInfo) info);
        } else if (info instanceof DataStoreInfo) {
            catalog.add((DataStoreInfo) info);
        } else if (info instanceof CoverageStoreInfo) {
            catalog.add((CoverageStoreInfo) info);
        } else if (info instanceof WMSStoreInfo) {
            catalog.add((WMSStoreInfo) info);
        } else if (info instanceof FeatureTypeInfo) {
            catalog.add((FeatureTypeInfo) info);
        } else if (info instanceof CoverageInfo) {
            catalog.add((CoverageInfo) info);
        } else if (info instanceof LayerInfo) {
            catalog.add((LayerInfo) info);
        } else if (info instanceof StyleInfo) {
            catalog.add((StyleInfo) info);
        } else if (info instanceof LayerGroupInfo) {
            catalog.add((LayerGroupInfo) info);
        } else if (info instanceof WMSLayerInfo) {
            catalog.add((WMSLayerInfo) info);
        } else if (info instanceof SettingsInfo) {
            geoServer.add((SettingsInfo) info);
        } else if (info instanceof ServiceInfo) {
            geoServer.add((ServiceInfo) info);
        } else {
            throw new RuntimeException(
                    String.format(
                            "Don't know how to handle info of type '%s'.",
                            info.getClass().getSimpleName()));
        }
    }

    /** Remove info object from the provided GeoServer instance. */
    private static void remove(GeoServer geoServer, Catalog catalog, Info info) {
        if (info instanceof WorkspaceInfo) {
            catalog.remove((WorkspaceInfo) info);
        } else if (info instanceof NamespaceInfo) {
            catalog.remove((NamespaceInfo) info);
        } else if (info instanceof DataStoreInfo) {
            catalog.remove((DataStoreInfo) info);
        } else if (info instanceof CoverageStoreInfo) {
            catalog.remove((CoverageStoreInfo) info);
        } else if (info instanceof WMSStoreInfo) {
            catalog.remove((WMSStoreInfo) info);
        } else if (info instanceof FeatureTypeInfo) {
            catalog.remove((FeatureTypeInfo) info);
        } else if (info instanceof CoverageInfo) {
            catalog.remove((CoverageInfo) info);
        } else if (info instanceof LayerInfo) {
            catalog.remove((LayerInfo) info);
        } else if (info instanceof StyleInfo) {
            catalog.remove((StyleInfo) info);
        } else if (info instanceof LayerGroupInfo) {
            catalog.remove((LayerGroupInfo) info);
        } else if (info instanceof WMSLayerInfo) {
            catalog.remove((WMSLayerInfo) info);
        } else if (info instanceof SettingsInfo) {
            geoServer.remove((SettingsInfo) info);
        } else if (info instanceof ServiceInfo) {
            geoServer.remove((ServiceInfo) info);
        } else {
            throw new RuntimeException(
                    String.format(
                            "Don't know how to handle info of type '%s'.",
                            info.getClass().getSimpleName()));
        }
    }
}
