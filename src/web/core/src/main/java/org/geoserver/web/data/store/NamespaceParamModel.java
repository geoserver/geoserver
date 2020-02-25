/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import org.apache.wicket.model.IModel;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.util.MapModel;

/**
 * Model to wrap and unwrap a {@link NamespaceInfo} to and from a String for the Datastore's
 * "namespace" parameter
 */
public class NamespaceParamModel extends MapModel {
    public NamespaceParamModel(IModel model, String expression) {
        super(model, expression);
    }

    @Override
    public Object getObject() {
        String nsUri = (String) super.getObject();
        NamespaceInfo namespaceInfo =
                GeoServerApplication.get().getCatalog().getNamespaceByURI(nsUri);
        return namespaceInfo;
    }

    @Override
    public void setObject(Object object) {
        NamespaceInfo namespaceInfo = (NamespaceInfo) object;
        String nsUri = namespaceInfo.getURI();
        super.setObject(nsUri);
    }
}
