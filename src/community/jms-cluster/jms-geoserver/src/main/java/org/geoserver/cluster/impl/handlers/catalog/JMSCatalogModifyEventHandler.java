/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl.handlers.catalog;

import com.thoughtworks.xstream.XStream;
import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.CatalogInfo;
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
import org.geoserver.catalog.event.CatalogEvent;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.cluster.events.ToggleSwitch;
import org.geoserver.cluster.impl.utils.BeanUtils;
import org.geoserver.cluster.server.events.StyleModifyEvent;

/**
 * Handle modify events synchronizing catalog with serialized objects
 *
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 */
public class JMSCatalogModifyEventHandler extends JMSCatalogEventHandler {

    private final Catalog catalog;
    private final ToggleSwitch producer;

    /** */
    public JMSCatalogModifyEventHandler(
            Catalog catalog, XStream xstream, Class clazz, ToggleSwitch producer) {
        super(xstream, clazz);
        this.catalog = catalog;
        this.producer = producer;
    }

    @Override
    public boolean synchronize(CatalogEvent event) throws Exception {
        if (event == null) {
            throw new IllegalArgumentException("Incoming object is null");
        }
        try {
            if (event instanceof CatalogModifyEvent) {
                final CatalogModifyEvent modifyEv = ((CatalogModifyEvent) event);

                producer.disable();
                JMSCatalogModifyEventHandler.modify(catalog, modifyEv);

            } else {
                // incoming object not recognized
                if (LOGGER.isLoggable(java.util.logging.Level.SEVERE))
                    LOGGER.severe("Unrecognized event type");
                return false;
            }

        } catch (Exception e) {
            if (LOGGER.isLoggable(java.util.logging.Level.SEVERE))
                LOGGER.severe(
                        this.getClass() + " is unable to synchronize the incoming event: " + event);
            throw e;
        } finally {
            // re enable the producer
            producer.enable();
        }
        return true;
    }

    /**
     * simulate a catalog.save() rebuilding the EventModify proxy object locally {@link
     * org.geoserver.catalog.impl.DefaultCatalogFacade#saved(CatalogInfo)}
     *
     * <p>TODO synchronization on catalog object
     */
    protected static void modify(final Catalog catalog, CatalogModifyEvent modifyEv)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        final CatalogInfo info = modifyEv.getSource();

        // check if name is changed
        String name = getOldName(catalog, modifyEv);

        if (info instanceof LayerGroupInfo) {

            // check if name is changed
            if (name == null) {
                // name is unchanged
                name = ((LayerGroupInfo) info).getName();
            }

            final LayerGroupInfo localObject = catalog.getLayerGroupByName(name);

            if (localObject == null) {
                throw new CatalogException(
                        "Unable to locate " + info + " named: " + name + " locally.");
            }

            BeanUtils.smartUpdate(
                    localObject, modifyEv.getPropertyNames(), modifyEv.getNewValues());
            catalog.save(localObject);

        } else if (info instanceof LayerInfo) {

            // check if name is changed
            if (name == null) {
                // name is unchanged
                name = ((LayerInfo) info).getName();
            }

            final LayerInfo localObject = catalog.getLayerByName(name);

            if (localObject == null) {
                throw new CatalogException(
                        "Unable to locate " + info + " named: " + name + " locally.");
            }

            BeanUtils.smartUpdate(
                    localObject, modifyEv.getPropertyNames(), modifyEv.getNewValues());
            catalog.save(localObject);

        } else if (info instanceof MapInfo) {

            // check if name is changed
            if (name == null) {
                // name is unchanged
                name = ((MapInfo) info).getName();
            }

            final MapInfo localObject = catalog.getMapByName(name);

            if (localObject == null) {
                throw new CatalogException(
                        "Unable to locate " + info + " named: " + name + " locally.");
            }

            BeanUtils.smartUpdate(
                    localObject, modifyEv.getPropertyNames(), modifyEv.getNewValues());
            catalog.save(localObject);

        } else if (info instanceof NamespaceInfo) {

            final String uri;
            final Object uriObj = getOldValue(catalog, modifyEv, "uRI");
            if (uriObj != null) {
                uri = uriObj.toString();
            } else {
                // uri is unchanged
                uri = ((NamespaceInfo) info).getURI();
            }
            final NamespaceInfo localObject = catalog.getNamespaceByURI(uri);

            if (localObject == null) {
                throw new CatalogException(
                        "Unable to locate " + info + " uri: " + uri + " locally.");
            }

            BeanUtils.smartUpdate(
                    localObject, modifyEv.getPropertyNames(), modifyEv.getNewValues());
            catalog.save(localObject);

        } else if (info instanceof StoreInfo) {

            // check if name is changed
            if (name == null) {
                // name is unchanged
                name = ((StoreInfo) info).getName();
            }
            // check if workspace is changed
            final WorkspaceInfo workspace;
            final Object objWorkpsace = getOldValue(catalog, modifyEv, "workspace");
            if (objWorkpsace != null) {
                workspace = (WorkspaceInfo) objWorkpsace;
            } else {
                // workspace is unchanged
                workspace = ((StoreInfo) info).getWorkspace();
            }

            final StoreInfo localObject;
            if (info instanceof CoverageStoreInfo) {
                localObject = catalog.getStoreByName(workspace, name, CoverageStoreInfo.class);
            } else if (info instanceof DataStoreInfo) {
                localObject = catalog.getStoreByName(workspace, name, DataStoreInfo.class);
            } else if (info instanceof WMSStoreInfo) {
                localObject = catalog.getStoreByName(workspace, name, WMSStoreInfo.class);
            } else {
                throw new IllegalArgumentException(
                        "Unable to provide localization for the passed instance");
            }

            if (localObject == null) {
                throw new CatalogException(
                        "Unable to locate " + info + " named: " + name + " locally.");
            }

            BeanUtils.smartUpdate(
                    localObject, modifyEv.getPropertyNames(), modifyEv.getNewValues());
            catalog.save(localObject);

        } else if (info instanceof ResourceInfo) {

            // check if name is changed
            if (name == null) {
                // name is unchanged
                name = ((ResourceInfo) info).getName();
            }
            // check if namespace is changed
            final NamespaceInfo namespace;
            final Object objWorkpsace = getOldValue(catalog, modifyEv, "namespace");
            if (objWorkpsace != null) {
                namespace = (NamespaceInfo) objWorkpsace;
            } else {
                // workspace is unchanged
                namespace = ((ResourceInfo) info).getNamespace();
            }
            final ResourceInfo localObject;
            if (info instanceof CoverageInfo) {
                // coverage
                localObject = catalog.getCoverageByName(namespace, name);
            } else if (info instanceof FeatureTypeInfo) {
                // feature
                localObject = catalog.getFeatureTypeByName(namespace, name);
            } else if (info instanceof WMSLayerInfo) {
                // wmslayer
                localObject = catalog.getResourceByName(namespace, name, WMSLayerInfo.class);
            } else {
                throw new IllegalArgumentException(
                        "Unable to provide localization for the passed instance");
            }
            if (localObject == null) {
                throw new CatalogException(
                        "Unable to locate " + info + " named: " + name + " locally.");
            }

            BeanUtils.smartUpdate(
                    localObject, modifyEv.getPropertyNames(), modifyEv.getNewValues());
            catalog.save(localObject);

        } else if (info instanceof StyleInfo) {

            // check if name is changed
            if (name == null) {
                // name is unchanged
                name = ((StyleInfo) info).getName();
            }

            final StyleInfo localObject = catalog.getStyleByName(name);

            if (localObject == null) {
                throw new CatalogException(
                        "Unable to locate " + info + " named: " + name + " locally.");
            }

            BeanUtils.smartUpdate(
                    localObject, modifyEv.getPropertyNames(), modifyEv.getNewValues());

            // let's if the style file was provided
            if (modifyEv instanceof StyleModifyEvent) {
                StyleModifyEvent styleModifyEvent = (StyleModifyEvent) modifyEv;
                byte[] fileContent = styleModifyEvent.getFile();
                if (fileContent != null && fileContent.length != 0) {
                    // update the style file using the old style
                    StyleInfo oldStyle = catalog.getStyleByName(name);
                    try {
                        catalog.getResourcePool()
                                .writeStyle(oldStyle, new ByteArrayInputStream(fileContent));
                    } catch (Exception exception) {
                        throw new RuntimeException(
                                String.format(
                                        "Error writing style '%s' file.", localObject.getName()),
                                exception);
                    }
                }
            }

            // update the style in the catalog
            catalog.save(localObject);

        } else if (info instanceof WorkspaceInfo) {

            // check if name is changed
            if (name == null) {
                // name is unchanged
                name = ((WorkspaceInfo) info).getName();
            }

            final WorkspaceInfo localObject = catalog.getWorkspaceByName(name);

            if (localObject == null) {
                throw new CatalogException(
                        "Unable to locate " + info + " named: " + name + " locally.");
            }

            BeanUtils.smartUpdate(
                    localObject, modifyEv.getPropertyNames(), modifyEv.getNewValues());
            catalog.save(localObject);

        } else if (info instanceof CatalogInfo) {

            // change default workspace in the handled catalog
            /**
             * This piece of code was extracted from: {@link
             * org.geoserver.catalog.NamespaceWorkspaceConsistencyListener#handleModifyEvent(CatalogModifyEvent)}
             */
            final List<String> properties = modifyEv.getPropertyNames();
            if (properties.contains("defaultNamespace")) {
                final NamespaceInfo newDefault =
                        (NamespaceInfo)
                                modifyEv.getNewValues().get(properties.indexOf("defaultNamespace"));
                if (newDefault != null) {
                    final WorkspaceInfo ws = catalog.getWorkspaceByName(newDefault.getPrefix());
                    if (ws != null && !catalog.getDefaultWorkspace().equals(ws)) {
                        catalog.setDefaultWorkspace(ws);
                    }
                }
            } else if (properties.contains("defaultWorkspace")) {
                final WorkspaceInfo newDefault =
                        (WorkspaceInfo)
                                modifyEv.getNewValues().get(properties.indexOf("defaultWorkspace"));
                if (newDefault != null) {
                    final NamespaceInfo ns = catalog.getNamespaceByPrefix(newDefault.getName());
                    if (ns != null && !catalog.getDefaultNamespace().equals(ns)) {
                        catalog.setDefaultNamespace(ns);
                    }
                }
            }

        } else {
            if (LOGGER.isLoggable(java.util.logging.Level.WARNING)) {
                LOGGER.warning("info - ID: " + info.getId() + " toString: " + info.toString());
            }
            throw new IllegalArgumentException("Bad incoming object: " + info.toString());
        }
    }

    /**
     * get the local old name for the passed CatalogInfo event
     *
     * @param catalog the catalog
     * @param ev the modify event
     * @return a String representing the old name or null if name is not changed or not exists at
     *     all
     */
    private static String getOldName(final Catalog catalog, final CatalogModifyEvent ev) {
        // try to get the old value for the name
        final Object name = getOldValue(catalog, ev, "name");
        // check return and return a string representation of the name or null
        return name != null ? name.toString() : null;
    }

    /**
     * get the old property for the passed CatalogInfo event
     *
     * @param catalog the catalog
     * @param ev the modify event
     * @param oldProp the name of the old property to search for
     * @return an Object representing the old value of the passed property or null if name is not
     *     changed or not exists at all
     */
    private static Object getOldValue(
            final Catalog catalog, final CatalogModifyEvent ev, final String oldProp) {
        final CatalogInfo service = ev.getSource();
        if (service == null) {
            throw new IllegalArgumentException("passed service is null");
        }
        // check if name is changed
        final List<String> props = ev.getPropertyNames();
        final int index = props.indexOf(oldProp);
        if (index != -1) {
            final List<Object> oldValues = ev.getOldValues();
            // search the Service using the old name
            return oldValues.get(index);
        } else {
            return null;
        }
    }
}
