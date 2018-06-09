/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.bdb;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import org.geoserver.catalog.Info;

public class CatalogObjectConverter implements Converter {

    public boolean canConvert(Class type) {
        return Info.class.isAssignableFrom(type);
    }

    public void marshal(
            Object source, HierarchicalStreamWriter writer, MarshallingContext context) {}

    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {

        return null;
    }
}
