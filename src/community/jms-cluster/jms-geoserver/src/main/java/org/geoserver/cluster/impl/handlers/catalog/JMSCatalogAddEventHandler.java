/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl.handlers.catalog;

import com.thoughtworks.xstream.XStream;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogEvent;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.cluster.events.ToggleSwitch;

/**
 * Handler for CatalogAddEvent.
 *
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 */
public class JMSCatalogAddEventHandler extends JMSCatalogEventHandler {
    private final Catalog catalog;
    private final ToggleSwitch producer;

    @SuppressWarnings({"rawtypes", "unchecked"})
    public JMSCatalogAddEventHandler(Catalog catalog, XStream xstream, Class clazz, ToggleSwitch producer) {
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
            if (event instanceof CatalogAddEvent addEvent) {
                final CatalogAddEvent addEv = addEvent;

                // get the source from the incoming event
                final CatalogInfo info = addEv.getSource();
                // disable the producer to avoid recursion
                producer.disable();

                // add the incoming CatalogInfo to the local catalog
                JMSCatalogAddEventHandler.add(catalog, info);
            } else {
                // incoming object not recognized
                if (LOGGER.isLoggable(java.util.logging.Level.SEVERE)) LOGGER.severe("Unrecognized event type");
                return false;
            }

        } catch (Exception e) {
            if (LOGGER.isLoggable(java.util.logging.Level.SEVERE))
                LOGGER.severe(this.getClass() + " is unable to synchronize the incoming event: " + event);
            throw e;
        } finally {
            // re enable the producer
            producer.enable();
        }
        return true;
    }

    private static void add(final Catalog catalog, CatalogInfo info)
            throws IllegalAccessException, InvocationTargetException {

        if (info instanceof LayerGroupInfo groupInfo) {

            final LayerGroupInfo deserObject = CatalogUtils.localizeLayerGroup(groupInfo, catalog);
            catalog.add(ModificationProxy.unwrap(deserObject));

        } else if (info instanceof LayerInfo layerInfo) {

            final LayerInfo layer = CatalogUtils.localizeLayer(layerInfo, catalog);
            catalog.add(ModificationProxy.unwrap(layer));

        } else if (info instanceof MapInfo mapInfo) {

            final MapInfo localObject = CatalogUtils.localizeMapInfo(mapInfo, catalog);
            catalog.add(ModificationProxy.unwrap(localObject));

        } else if (info instanceof NamespaceInfo namespaceInfo) {

            final NamespaceInfo namespace = CatalogUtils.localizeNamespace(namespaceInfo, catalog);
            catalog.add(ModificationProxy.unwrap(namespace));

        } else if (info instanceof StoreInfo storeInfo) {

            StoreInfo store = CatalogUtils.localizeStore(storeInfo, catalog);
            catalog.add(ModificationProxy.unwrap(store));

        } else if (info instanceof ResourceInfo resourceInfo) {

            final ResourceInfo resource = CatalogUtils.localizeResource(resourceInfo, catalog);
            catalog.add(ModificationProxy.unwrap(resource));

        } else if (info instanceof StyleInfo styleInfo) {

            final StyleInfo deserializedObject = CatalogUtils.localizeStyle(styleInfo, catalog);
            catalog.add(ModificationProxy.unwrap(deserializedObject));

        } else if (info instanceof WorkspaceInfo workspaceInfo) {

            final WorkspaceInfo workspace = CatalogUtils.localizeWorkspace(workspaceInfo, catalog);
            catalog.add(ModificationProxy.unwrap(workspace));

        } else if (info instanceof CatalogInfo) {
            // TODO may we don't want to send this empty message!
            // TODO check the producer
            // DO NOTHING
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.severe("info - ID: " + info.getId() + " toString: " + info.toString());
            }
        } else {
            throw new IllegalArgumentException("Bad incoming object: " + info.getClass());
        }
    }
}
