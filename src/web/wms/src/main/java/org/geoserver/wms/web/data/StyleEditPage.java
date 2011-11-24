/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.data;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Level;

import org.apache.wicket.PageParameters;
import org.apache.wicket.WicketRuntimeException;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.Styles;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.util.Version;

/**
 * Style edit page
 */
public class StyleEditPage extends AbstractStylePage {
    
    public static final String NAME = "name";

    public StyleEditPage(PageParameters parameters) {
        String name = parameters.getString(NAME);
        StyleInfo si = getCatalog().getStyleByName(name);
        
        if(si == null) {
            error(new ParamResourceModel("StyleEditPage.notFound", this, name).getString());
            setResponsePage(StylePage.class);
            return;
        }
        
        initUI(si);
    }
    
    public StyleEditPage(StyleInfo style) {
        super(style);
        uploadForm.setVisible(false);
    }

    @Override
    protected void onStyleFormSubmit() {
        // write out the file and save name modifications
        try {
            StyleInfo style = (StyleInfo) styleForm.getModelObject();
            Version version = Styles.findVersion(new ByteArrayInputStream(rawSLD.getBytes()));
            style.setSLDVersion(version);
            
            // write out the SLD
            try {
                getCatalog().getResourcePool().writeStyle(style,
                        new ByteArrayInputStream(rawSLD.getBytes()));
            } catch (IOException e) {
                throw new WicketRuntimeException(e);
            }
            getCatalog().save(style);
            setResponsePage( StylePage.class );
        } catch( Exception e ) {
            LOGGER.log(Level.SEVERE, "Error occurred saving the style", e);
            styleForm.error( e );
        }
        
    }
    
}
