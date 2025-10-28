/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import org.glassfish.jaxb.runtime.marshaller.NamespacePrefixMapper;

/**
 * Custom namespace prefix mapper for MapML JAXB marshalling. Maps the XHTML namespace to an empty prefix (default
 * namespace).
 */
public class MapMLNamespacePrefixMapper extends NamespacePrefixMapper {

    @Override
    public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {
        if ("http://www.w3.org/1999/xhtml".equalsIgnoreCase(namespaceUri)) {
            return "";
        }
        return suggestion;
    }
}
