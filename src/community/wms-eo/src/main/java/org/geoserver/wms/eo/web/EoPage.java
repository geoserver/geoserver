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

    protected final ResourceModel OUTLINE = new ResourceModel("outline", "Outline");
    protected final ResourceModel PRODUCT = new ResourceModel("product", "Product");
    protected final ResourceModel BAND = new ResourceModel("band", "Band Coverage");
    protected final ResourceModel GEOPHYSICAL_PARAMETER = new ResourceModel("parameter", "Geophysical Parameter");
    protected final ResourceModel BITMASK = new ResourceModel("bitmask", "Bitmask");    
    

    protected TextParamPanel getTextParamPanel(String name, String label, IModel model, boolean required) {
        return new TextParamPanel(name, new PropertyModel<String>(model, name),
                new ResourceModel(name, label), required);
    }    
    
    protected DirectoryParamPanel getDirectoryPanel(String name, String label, IModel model, boolean required) {
        return new DirectoryParamPanel(name, new PropertyModel<String>(model, name), 
                new ResourceModel(name, label), required, new FileExistsValidator());
    }
}