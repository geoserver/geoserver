/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.publish;

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;

public class BasicLayerConfig extends LayerConfigurationPanel {
	
	public BasicLayerConfig(String id, IModel model) {
		super(id, model);
		init();
		// atm the layer follows the resource name, so it's not editable
		TextField name = new TextField("name");
		name.setEnabled(false);
		
        add(name);
		add(new CheckBox("enabled"));
		add(new CheckBox("advertised"));
	}
	
	private void init(){
	}
	
	private static final long serialVersionUID = 677955476932894110L;
}
