/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2013, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geoserver.wms.eo;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.util.logging.Logging;


public class EoStyleCatalogListener implements CatalogListener {

    private final Catalog catalog;
    private final GeoServerResourceLoader resourceLoader;
    private static final Logger LOGGER = Logging.getLogger(EoStyleCatalogListener.class);    
    public static final String[] STYLE_NAMES = new String[] {
        "eo-point",
        "eo-lines"
    };
    private static final String[] STYLE_FILES = new String[] {
        "eo_point.sld",
        "eo_line.sld"
    };
    
    
    public EoStyleCatalogListener(Catalog catalog, GeoServerResourceLoader resourceLoader) throws IOException {
        this.catalog = catalog;
        this.resourceLoader = resourceLoader;
        initializeStyles();
        catalog.addListener(this);
    }
    
    
    private void initializeStyles() throws IOException {
        for (int i = 0; i < STYLE_NAMES.length; i++) {
            if (catalog.getStyleByName(STYLE_NAMES[i]) == null) {
                initializeStyle(STYLE_NAMES[i], STYLE_FILES[i]);
            }
        }
    }
    
    /**
     * Copies a WMS-EO style to the data directory and adds a catalog entry for it.
     * 
     * See org.geoserver.config.GeoServerLoader.initializeStyle.
     */
    private void initializeStyle(String styleName, String sld) throws IOException {
        // copy the file out to the data directory if necessary
        if (resourceLoader.find("styles", sld) == null) {
            FileUtils.copyURLToFile(EoStyleCatalogListener.class.getResource(sld), 
                    new File(resourceLoader.findOrCreateDirectory("styles"), sld));
        }
        
        // create a style for it
        StyleInfo s = catalog.getFactory().createStyle();
        s.setName(styleName);
        s.setFilename(sld);
        catalog.add(s);
    }

    @Override
    public void handleRemoveEvent(CatalogRemoveEvent event) throws CatalogException {
        if (event.getSource() instanceof StyleInfo) {
            // when this event has been fired the style has already been removed
            try {
                // try to find if a required style has been deleted and recreate it
                initializeStyles();                
            } catch (RuntimeException e) {
                // style creation could fail with a RuntimeExecption 
                // in the remote possibility that style has already been recreated
                LOGGER.warning(e.getMessage());
            } catch (Exception e) {
                LOGGER.warning(e.getMessage());                
            }
        }
    }

    @Override
    public void handleModifyEvent(CatalogModifyEvent event) throws CatalogException {
        if (event.getSource() instanceof StyleInfo) {
            StyleInfo style = (StyleInfo) event.getSource();
            for (String styleName : STYLE_NAMES) {
                if (styleName.equals(style.getName())) {
                    throw new CatalogException("Style " + styleName + " is used by module WMS-EO and is read-only");
                }
            }
        }
    }

    @Override
    public void handlePostModifyEvent(CatalogPostModifyEvent event) throws CatalogException {
    }

    @Override
    public void handleAddEvent(CatalogAddEvent event) throws CatalogException {
    }
    
    @Override
    public void reloaded() {
    }
}