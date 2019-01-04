/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl.handlers.catalog;

import com.thoughtworks.xstream.XStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;
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
import org.geoserver.catalog.event.CatalogEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.cluster.events.ToggleSwitch;

/** @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it */
public class JMSCatalogRemoveEventHandler extends JMSCatalogEventHandler {
    private final Catalog catalog;
    private final ToggleSwitch producer;

    public JMSCatalogRemoveEventHandler(
            Catalog catalog, XStream xstream, Class clazz, ToggleSwitch producer) {
        super(xstream, clazz);
        this.catalog = catalog;
        this.producer = producer;
    }

    @Override
    public boolean synchronize(CatalogEvent event) throws Exception {
        if (event == null) {
            throw new NullPointerException("Incoming object is null");
        }
        try {
            if (event instanceof CatalogRemoveEvent) {
                final CatalogRemoveEvent removeEv = ((CatalogRemoveEvent) event);

                // get the source
                final CatalogInfo info = removeEv.getSource();

                // disable the producer to avoid recursion
                producer.disable();
                // remove the selected CatalogInfo
                JMSCatalogRemoveEventHandler.remove(catalog, info, getProperties());

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

    private static void remove(final Catalog catalog, CatalogInfo info, Properties options)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {

        if (info instanceof LayerGroupInfo) {

            final LayerGroupInfo deserObject =
                    CatalogUtils.localizeLayerGroup((LayerGroupInfo) info, catalog);
            catalog.remove(deserObject);
            // catalog.save(CatalogUtils.getProxy(deserObject));
            // info=CatalogUtils.localizeLayerGroup((LayerGroupInfo) info,
            // catalog);

        } else if (info instanceof LayerInfo) {

            final LayerInfo layer = CatalogUtils.localizeLayer((LayerInfo) info, catalog);
            catalog.remove(layer);
            // catalog.save(CatalogUtils.getProxy(layer));
            // info=CatalogUtils.localizeLayer((LayerInfo) info, catalog);

        } else if (info instanceof MapInfo) {

            final MapInfo localObject = CatalogUtils.localizeMapInfo((MapInfo) info, catalog);
            catalog.remove(localObject);
            // catalog.save(CatalogUtils.getProxy(localObject));
            // info= CatalogUtils.localizeMapInfo((MapInfo) info,catalog);

        } else if (info instanceof NamespaceInfo) {

            final NamespaceInfo namespace =
                    CatalogUtils.localizeNamespace((NamespaceInfo) info, catalog);
            catalog.remove(namespace);
            // catalog.save(CatalogUtils.getProxy(namespace));
            // info =CatalogUtils.localizeNamespace((NamespaceInfo) info,
            // catalog);
        } else if (info instanceof StoreInfo) {

            StoreInfo store = CatalogUtils.localizeStore((StoreInfo) info, catalog);
            catalog.remove(store);
            // catalog.save(CatalogUtils.getProxy(store));

            // info=CatalogUtils.localizeStore((StoreInfo)info,catalog);
        } else if (info instanceof ResourceInfo) {

            final ResourceInfo resource =
                    CatalogUtils.localizeResource((ResourceInfo) info, catalog);
            catalog.remove(resource);
            // catalog.save(CatalogUtils.getProxy(resource));
            // info =CatalogUtils.localizeResource((ResourceInfo)info,catalog);
        } else if (info instanceof StyleInfo) {

            final StyleInfo style = CatalogUtils.localizeStyle((StyleInfo) info, catalog);

            catalog.remove(style);

            // check options
            final String purge = (String) options.get("purge");
            if (purge != null && Boolean.parseBoolean(purge)) {
                try {
                    catalog.getResourcePool().deleteStyle(style, true);
                } catch (IOException e) {
                    if (LOGGER.isLoggable(java.util.logging.Level.SEVERE)) {
                        LOGGER.severe(e.getLocalizedMessage());
                    }
                }
            }

            // catalog.detach(CatalogUtils.getProxy(deserializedObject));
            // info = CatalogUtils.localizeStyle((StyleInfo) info, catalog);

        } else if (info instanceof WorkspaceInfo) {

            final WorkspaceInfo workspace =
                    CatalogUtils.localizeWorkspace((WorkspaceInfo) info, catalog);
            catalog.remove(workspace);
            // catalog.detach(workspace);
            // info = CatalogUtils.localizeWorkspace((WorkspaceInfo) info,
            // catalog);
        } else if (info instanceof CatalogInfo) {
            // TODO may we don't want to send this empty message!
            // TODO check the producer
            // DO NOTHING
            if (LOGGER.isLoggable(java.util.logging.Level.WARNING)) {
                LOGGER.warning("info - ID: " + info.getId() + " toString: " + info.toString());
            }
        } else {
            throw new IllegalArgumentException("Bad incoming object: " + info.toString());
        }
    }
}
