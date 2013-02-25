/* Copyright (c) 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.eo.web;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.store.panel.DirectoryParamPanel;
import org.geoserver.web.data.store.panel.TextParamPanel;
import org.geoserver.web.wicket.FileExistsValidator;


/**
 * Base class for wms-eo web pages.
 * 
 * @author Davide Savazzi - geo-solutions.it
 */
public abstract class EoPage extends GeoServerSecuredPage {

    protected static final ResourceModel BROWSE_IMAGE = new ResourceModel("browseImage", "Browse Image");
    protected static final ResourceModel BAND = new ResourceModel("band", "Band Coverage");
    protected static final ResourceModel GEOPHYSICAL_PARAMETER = new ResourceModel("parameter", "Geophysical Parameter");
    protected static final ResourceModel BITMASK = new ResourceModel("bitmask", "Bitmask");    
    

    protected static TextParamPanel getTextParamPanel(String name, String label, IModel model, boolean required) {
        return new TextParamPanel(name, new PropertyModel<String>(model, name),
                new ResourceModel(name, label), required);
    }    
    
    protected static DirectoryParamPanel getDirectoryPanel(String name, String label, IModel model, boolean required) {
        return new DirectoryParamPanel(name, new PropertyModel<String>(model, name), 
                new ResourceModel(name, label), required, new FileExistsValidator());
    }
}