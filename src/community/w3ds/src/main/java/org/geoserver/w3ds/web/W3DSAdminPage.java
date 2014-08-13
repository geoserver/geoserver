/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.w3ds.web;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.geoserver.w3ds.service.W3DSInfo;
import org.geoserver.web.services.BaseServiceAdminPage;
import org.geoserver.web.util.MapModel;

@SuppressWarnings("serial")
public class W3DSAdminPage extends BaseServiceAdminPage<W3DSInfo> {

	protected Class<W3DSInfo> getServiceClass() {
		return W3DSInfo.class;
	}

	protected void build(IModel info, Form form) {

	}

	MapModel defaultedModel(IModel baseModel, String key, Object defaultValue) {
		MapModel model = new MapModel(baseModel, key);
		if (model.getObject() == null)
			model.setObject(defaultValue);
		return model;
	}

	protected String getServiceName() {
		return "W3DS";
	}

}
