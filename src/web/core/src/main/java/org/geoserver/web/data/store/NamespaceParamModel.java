/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import java.io.Serializable;
import java.util.Map;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.util.MapModel;

/**
 * Model to wrap and unwrap a {@link NamespaceInfo} to and from a String for the Datastore's
 * "namespace" parameter
 */
public class NamespaceParamModel implements IModel<NamespaceInfo> {

    MapModel<Serializable> delegate;

    public NamespaceParamModel(IModel<Map<String, Serializable>> model, String expression) {
        this.delegate = new MapModel<>(model, expression);
    }

    @Override
    public NamespaceInfo getObject() {
        String nsUri = (String) delegate.getObject();
        NamespaceInfo namespaceInfo =
                GeoServerApplication.get().getCatalog().getNamespaceByURI(nsUri);
        return namespaceInfo;
    }

    @Override
    public void setObject(NamespaceInfo namespaceInfo) {
        String nsUri = namespaceInfo.getURI();
        delegate.setObject(nsUri);
    }

    @Override
    public void detach() {
        delegate.detach();
    }
}
