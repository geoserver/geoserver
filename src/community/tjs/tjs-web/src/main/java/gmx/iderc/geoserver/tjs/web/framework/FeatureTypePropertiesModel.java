/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package gmx.iderc.geoserver.tjs.web.framework;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.web.GeoServerApplication;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple detachable model listing all the available workspaces
 */
@SuppressWarnings("serial")
public class FeatureTypePropertiesModel extends LoadableDetachableModel {

    IModel featureTypeModel;

    public FeatureTypePropertiesModel(IModel featureTypeModel) {
        this.featureTypeModel = featureTypeModel;
    }

    @Override
    protected Object load() {
        FeatureTypeInfo featureTypeInfo = (FeatureTypeInfo) featureTypeModel.getObject();
        if (featureTypeInfo != null) {
            Catalog catalog = GeoServerApplication.get().getCatalog();
            final ResourcePool resourcePool = catalog.getResourcePool();
            List<AttributeTypeInfo> attributes;
            try {
                attributes = resourcePool.getAttributes(featureTypeInfo);
                Collections.sort(attributes, new AttributeTypeComparator());
                return attributes;
            } catch (IOException ex) {
                Logger.getLogger(FeatureTypePropertiesModel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return new ArrayList<AttributeTypeInfo>();
    }


    class AttributeTypeComparator implements Comparator<AttributeTypeInfo> {

        public int compare(AttributeTypeInfo w1, AttributeTypeInfo w2) {
            return w1.getName().compareToIgnoreCase(w2.getName());
        }
    }
}
