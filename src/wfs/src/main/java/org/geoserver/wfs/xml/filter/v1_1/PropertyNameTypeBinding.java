/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml.filter.v1_1;

import org.geoserver.catalog.Catalog;
import org.geoserver.wfs.WFSException;
import org.geotools.filter.v1_0.OGCPropertyNameTypeBinding;
import org.geotools.gml3.GML;
import org.geotools.gml3.bindings.GML3EncodingUtils;
import org.geotools.xsd.ElementInstance;
import org.geotools.xsd.Node;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * A binding for ogc:PropertyName which does a special case check for an empty property name and
 * adds namespace support.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class PropertyNameTypeBinding extends OGCPropertyNameTypeBinding {
    /** the geoserver catalog */
    Catalog catalog;

    /** parser namespace mappings */
    NamespaceSupport namespaceSupport;

    public PropertyNameTypeBinding(
            FilterFactory filterFactory, NamespaceSupport namespaceSupport, Catalog catalog) {
        super(filterFactory);
        this.namespaceSupport = namespaceSupport;
        this.catalog = catalog;
    }

    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {
        PropertyName propertyName = (PropertyName) super.parse(instance, node, value);

        // JD: temporary hack, this should be carried out at evaluation time
        String name = propertyName.getPropertyName();

        if (name != null && name.matches("\\w+:\\w+")) {
            // namespace qualified name, ensure the prefix is valid
            String prefix = name.substring(0, name.indexOf(':'));
            String namespaceURI = namespaceSupport.getURI(prefix);

            // only accept if its an application schema namespace, or gml
            if (!GML.NAMESPACE.equals(namespaceURI)
                    && (catalog.getNamespaceByURI(namespaceURI) == null)) {
                throw new WFSException("Illegal attribute namespace: " + namespaceURI);
            }
        }

        if (factory instanceof FilterFactory2) {
            return ((FilterFactory2) factory)
                    .property(
                            propertyName.getPropertyName(),
                            GML3EncodingUtils.copyNamespaceSupport(namespaceSupport));
        }

        return propertyName;
    }
}
