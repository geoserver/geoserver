/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.publish;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.LayerInfo;

/**
 * A panel created to configure one aspect of a {@link LayerInfo} object.
 * <p>
 * Typically there will be one panel dealing generically with
 * {@link LayerInfo} and one extra panel to deal with the specifics of each
 * service publishing the data (WMS, WCS, WFS, ...).
 * <p>
 * All the components in the panel must be contained in a {@link Form} to make
 * sure the whole tab switch and page submit workflow function properly
 * 
 * @see LayerGroupConfigurationPanel
 */
public class LayerConfigurationPanel extends Panel {
	private static final long serialVersionUID = 4881474189619124359L;

	public LayerConfigurationPanel(String id, IModel<LayerInfo> model){
		super(id, model);
	}
	
	public LayerInfo getLayerInfo(){
		return (LayerInfo)getDefaultModelObject();
	}
}
