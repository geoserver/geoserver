/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xslt.config;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.ows.util.OwsUtils;

/**
 * Transforms CatalogInfo into id references. Derived and heavily simplified from {@link
 * XStreamPersister}
 */
class ReferenceConverter implements Converter {
    Class clazz;

    private Catalog catalog;

    public ReferenceConverter(Class clazz, Catalog catalog) {
        this.catalog = catalog;
        this.clazz = clazz;
    }

    @SuppressWarnings("unchecked")
    public boolean canConvert(Class type) {
        return clazz.isAssignableFrom(type);
    }

    public void marshal(
            Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
        // could be a proxy, unwrap it
        source = CatalogImpl.unwrap(source);

        // gets its id
        String id = (String) OwsUtils.get(source, "id");
        writer.startNode("id");
        writer.setValue(id);
        writer.endNode();
    }

    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        String ref = null;
        if (reader.hasMoreChildren()) {
            while (reader.hasMoreChildren()) {
                reader.moveDown();
                ref = reader.getValue();
                reader.moveUp();
            }
        } else {
            ref = reader.getValue();
        }

        FeatureTypeInfo result = catalog.getFeatureType(ref);
        if (result == null) {
            result = catalog.getFeatureTypeByName(ref);
        }

        return result;
    }
}
