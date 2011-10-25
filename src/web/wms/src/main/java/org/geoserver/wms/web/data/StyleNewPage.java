/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.data;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Level;

import org.apache.wicket.WicketRuntimeException;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.Styles;
import org.geotools.util.Version;

/**
 * Allows for editing a new style, includes file upload
 */
public class StyleNewPage extends AbstractStylePage {
    
    public StyleNewPage() {
        initUI(null);
    }
    
    @Override
    protected void onStyleFormSubmit() {
        // add the style
        Catalog catalog = getCatalog();
        StyleInfo s = (StyleInfo) styleForm.getModelObject();

        // write out the SLD before creating the style
        try {
            catalog.getResourcePool().writeStyle(s,
                    new ByteArrayInputStream(rawSLD.getBytes()));
        } catch (IOException e) {
            throw new WicketRuntimeException(e);
        }
        
        // store in the catalog
        try {
            Version version = Styles.findVersion(new ByteArrayInputStream(rawSLD.getBytes()));
            s.setSLDVersion(version);
            if (s.getFilename() == null) {
                // TODO: check that this does not overriDe any existing files
                s.setFilename(s.getName() + ".sld");
            }
            getCatalog().add(s);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error occurred saving the style", e);
            error(e);
            return;
        }

        setResponsePage(StylePage.class);

    }
}
