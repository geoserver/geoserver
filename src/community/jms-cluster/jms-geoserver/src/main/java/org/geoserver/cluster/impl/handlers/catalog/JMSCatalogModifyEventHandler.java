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
import java.util.Objects;
import java.util.function.Function;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
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

    /**
     * @param catalog
     * @param xstream
     * @param clazz
     * @param producer
     */
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
                modify(catalog, modifyEv);
            } else {
                // incoming object not recognized
                LOGGER.severe("Unrecognized event type");
                return false;
            }

        } catch (Exception e) {
            LOGGER.severe(
                    this.getClass().getName()
                            + " is unable to synchronize the incoming event: "
                            + event);
            throw e;
        } finally {
            // re enable the producer
            producer.enable();
        }
        return true;
    }

    private <T extends CatalogInfo> T modifyLocalObject(
            CatalogModifyEvent modifyEv, Function<String, T> findByIdFunction)
            throws CatalogException, IllegalAccessException, InvocationTargetException,
                    NoSuchMethodException {

        final CatalogInfo eventSource = modifyEv.getSource();
        final List<String> propertyNames = modifyEv.getPropertyNames();
        final List<Object> newValues = modifyEv.getNewValues();

        T localObject = findByIdFunction.apply(eventSource.getId());
        if (localObject == null) {
            throw new CatalogException(String.format("Unable to locate %s locally", eventSource));
        }
        BeanUtils.smartUpdate(localObject, propertyNames, newValues);
        return localObject;
    }

    /**
     * simulate a catalog.save() rebuilding the EventModify proxy object locally {@link
     * org.geoserver.catalog.impl.DefaultCatalogFacade#saved(CatalogInfo)}
     *
     * @param catalog
     * @param event
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     *     <p>TODO synchronization on catalog object
     */
    protected void modify(final Catalog catalog, CatalogModifyEvent event)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        final CatalogInfo info = event.getSource();
        Objects.requireNonNull(info, "Event does not provide the modified CatalogInfo instance");

        if (info instanceof Catalog) {
            handleCatalogModification(catalog, event);
            return;
        }

        CatalogInfo localObject;

        if (info instanceof LayerGroupInfo) {
            localObject = modifyLocalObject(event, catalog::getLayerGroup);
            catalog.save((LayerGroupInfo) localObject);
        } else if (info instanceof LayerInfo) {
            localObject = modifyLocalObject(event, catalog::getLayer);
            catalog.save((LayerInfo) localObject);
        } else if (info instanceof MapInfo) {
            localObject = modifyLocalObject(event, catalog::getMap);
            catalog.save((MapInfo) localObject);
        } else if (info instanceof NamespaceInfo) {
            localObject = modifyLocalObject(event, catalog::getNamespace);
            catalog.save((NamespaceInfo) localObject);
        } else if (info instanceof StoreInfo) {
            localObject = modifyLocalObject(event, id -> catalog.getStore(id, StoreInfo.class));
            catalog.save((StoreInfo) localObject);
        } else if (info instanceof ResourceInfo) {
            localObject =
                    modifyLocalObject(event, id -> catalog.getResource(id, ResourceInfo.class));
            catalog.save((ResourceInfo) localObject);
        } else if (info instanceof StyleInfo) {
            localObject = modifyLocalObject(event, catalog::getStyle);
            catalog.save((StyleInfo) localObject);
            // let's if the style file was provided
            if (event instanceof StyleModifyEvent) {
                StyleModifyEvent styleModifyEvent = (StyleModifyEvent) event;
                byte[] fileContent = styleModifyEvent.getFile();
                if (fileContent != null && fileContent.length != 0) {
                    // update the style file using the old style, in case name changed
                    StyleInfo currentStyle = catalog.getStyle(info.getId());
                    try {
                        catalog.getResourcePool()
                                .writeStyle(currentStyle, new ByteArrayInputStream(fileContent));
                    } catch (Exception exception) {
                        throw new RuntimeException(
                                String.format(
                                        "Error writing style '%s' file.",
                                        ((StyleInfo) localObject).getName()),
                                exception);
                    }
                }
            }
        } else if (info instanceof WorkspaceInfo) {
            localObject = modifyLocalObject(event, catalog::getWorkspace);
            catalog.save((WorkspaceInfo) localObject);
        } else {
            final String stringRepresentation = info.toString();
            LOGGER.warning(
                    "Unknown CatalogInfo object type. ID: "
                            + info.getId()
                            + ": "
                            + stringRepresentation);
            throw new IllegalArgumentException("Bad incoming object: " + stringRepresentation);
        }
    }

    private void handleCatalogModification(final Catalog catalog, CatalogModifyEvent event) {
        final List<String> propertyNames = event.getPropertyNames();
        final List<Object> newValues = event.getNewValues();

        // change default workspace in the handled catalog
        /**
         * This piece of code was inspired on: {@link
         * org.geoserver.catalog.NamespaceWorkspaceConsistencyListener#handleModifyEvent(CatalogModifyEvent)}
         */
        final int defWsIndex;
        if ((defWsIndex = propertyNames.indexOf("defaultWorkspace")) > -1) {
            final WorkspaceInfo newDefaultWS = (WorkspaceInfo) newValues.get(defWsIndex);
            final WorkspaceInfo ws =
                    newDefaultWS == null
                            ? null
                            : catalog.getWorkspaceByName(newDefaultWS.getName());
            catalog.setDefaultWorkspace(ws);
        }
        final int defNsIndex;
        if ((defNsIndex = propertyNames.indexOf("defaultNamespace")) > -1) {
            final NamespaceInfo newDefaultNS = (NamespaceInfo) newValues.get(defNsIndex);
            final NamespaceInfo ns =
                    newDefaultNS == null
                            ? null
                            : catalog.getNamespaceByPrefix(newDefaultNS.getPrefix());
            catalog.setDefaultNamespace(ns);
        }
    }
}
