/** 
 * Copyright (c) 2001 - 2009 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Arne Kepp / OpenGeo
 */
package org.geoserver.gwc;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geotools.util.logging.Logging;

/**
 * This class implements a GeoWebCache configuration object, i.e. a source of WMS layer definitions,
 * and a GeoServer catalog listener which is loaded on startup and listens for configuration
 * changes.
 * <p>
 * At construction time, registers itself as a {@link Catalog#addListener catalog listener}
 * </p>
 * 
 * @author Arne Kepp
 * @author Gabriel Roldan
 */
public class GWCCatalogListener implements CatalogListener {

    private static Logger log = Logging.getLogger("org.geoserver.gwc.GWCCatalogListener");

    private final Catalog cat;

    private final GWC gwc;

    public GWCCatalogListener(final Catalog cat, final GWC gwc) {

        this.cat = cat;
        this.gwc = gwc;

        cat.addListener(this);

        log.fine("GWCCatalogListener registered with catalog");
    }

    /**
     * @see org.geoserver.catalog.event.CatalogListener#handleAddEvent(org.geoserver.catalog.event.CatalogAddEvent)
     */
    public void handleAddEvent(CatalogAddEvent event) throws CatalogException {
        Object obj = event.getSource();

        // We only handle layers here. Layer groups are initially empty
        if (obj instanceof LayerInfo) {
            LayerInfo layerInfo = (LayerInfo) obj;
            gwc.createLayer(layerInfo);
        } else if (obj instanceof LayerGroupInfo) {
            LayerGroupInfo lgi = (LayerGroupInfo) obj;
            gwc.createLayer(lgi);
        }
    }

    /**
     * 
     * @see org.geoserver.catalog.event.CatalogListener#handleModifyEvent(org.geoserver.catalog.event.CatalogModifyEvent)
     */
    public void handleModifyEvent(CatalogModifyEvent event) throws CatalogException {
        // Not dealing with this one just yet
    }

    /**
     * 
     * @see org.geoserver.catalog.event.CatalogListener#handlePostModifyEvent(org.geoserver.catalog.event.CatalogPostModifyEvent)
     */
    public void handlePostModifyEvent(CatalogPostModifyEvent event) throws CatalogException {
        Object obj = event.getSource();

        if (obj instanceof StyleInfo) {
            // TODO First pass only considers default styles,
            // which is all GWC will accept anyway
            StyleInfo si = (StyleInfo) obj;
            String styleName = si.getName();

            LinkedList<String> layerNameList = new LinkedList<String>();

            // First we collect all the layers that use this style
            Iterator<LayerInfo> liter = cat.getLayers().iterator();
            while (liter.hasNext()) {
                final LayerInfo li = liter.next();
                final StyleInfo defaultStyle = li.getDefaultStyle();
                if (defaultStyle != null && defaultStyle.getName().equals(styleName)) {
                    String prefixedName = li.getResource().getPrefixedName();
                    layerNameList.add(prefixedName);
                    //TODO: truncate/delete only the tileset associated to the style
                    gwc.truncate(prefixedName);
                }
            }

            // Now we check for layer groups that are affected
            Iterator<LayerGroupInfo> lgiter = cat.getLayerGroups().iterator();
            while (lgiter.hasNext()) {
                LayerGroupInfo lgi = lgiter.next();
                boolean truncate = false;

                // First we check for referenced to affected layers
                liter = lgi.getLayers().iterator();
                while (!truncate && liter.hasNext()) {
                    LayerInfo li = liter.next();
                    if (layerNameList.contains(li.getResource().getPrefixedName())) {
                        truncate = true;
                    }
                }

                // Finally we need to check whether the style is used explicitly
                if (!truncate) {
                    Iterator<StyleInfo> siiter = lgi.getStyles().iterator();
                    while (!truncate && siiter.hasNext()) {
                        StyleInfo si2 = siiter.next();
                        if (si2 != null && si2.getName().equals(si.getName())) {
                            truncate = true;
                        }
                    }
                }

                if (truncate) {
                    gwc.truncate(lgi.getName());
                }
                // Next layer group
            }

        } else if (obj instanceof LayerInfo) {
            LayerInfo li = (LayerInfo) obj;
            gwc.createLayer(li);
        } else if (obj instanceof LayerGroupInfo) {
            LayerGroupInfo lgInfo = (LayerGroupInfo) obj;
            gwc.createLayer(lgInfo);
        }

    }

    /**
     * 
     * @see org.geoserver.catalog.event.CatalogListener#handleRemoveEvent(org.geoserver.catalog.event.CatalogRemoveEvent)
     */
    public void handleRemoveEvent(CatalogRemoveEvent event) throws CatalogException {
        Object obj = event.getSource();

        String prefixedName = null;

        if (obj instanceof LayerGroupInfo) {
            LayerGroupInfo lgInfo = (LayerGroupInfo) obj;
            prefixedName = lgInfo.getName();
        } else if (obj instanceof LayerInfo) {
            LayerInfo layerInfo = (LayerInfo) obj;
            prefixedName = layerInfo.getResource().getPrefixedName();
        }

        if (null != prefixedName) {
            gwc.removeLayer(prefixedName);
        }
    }

    /**
     * 
     * @see org.geoserver.catalog.event.CatalogListener#reloaded()
     */
    public void reloaded() {
        gwc.reload();
    }

}
