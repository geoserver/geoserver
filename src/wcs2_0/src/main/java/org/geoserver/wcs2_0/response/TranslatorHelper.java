/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.response;

import java.util.Enumeration;
import org.geoserver.platform.ServiceException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * A helper object factoring out common methods used in Translators, we might want to add these to
 * TranslatorSupport later
 *
 * @author Andrea Aime - GeoSolutions
 */
class TranslatorHelper {

    /**
     * Register all namespaces as xmlns:xxx attributes for the top level element of a xml document
     */
    void registerNamespaces(NamespaceSupport ns, AttributesImpl attributes) {
        Enumeration declaredPrefixes = ns.getDeclaredPrefixes();
        while (declaredPrefixes.hasMoreElements()) {
            String prefix = (String) declaredPrefixes.nextElement();
            String uri = ns.getURI(prefix);

            // ignore xml prefix
            if ("xml".equals(prefix)) {
                continue;
            }

            String prefixDef = "xmlns:" + prefix;

            attributes.addAttribute("", prefixDef, prefixDef, "", uri);
        }
    }

    /** Adds together two sets of schema locations */
    String[] append(String[] locations1, String[] locations2) {
        String[] result = new String[locations1.length + locations2.length];
        System.arraycopy(locations1, 0, result, 0, locations1.length);
        System.arraycopy(locations2, 0, result, locations1.length, locations2.length);

        return result;
    }

    /** Builds the schema locations from the provided namespace/location list */
    String buildSchemaLocation(String... schemaLocations) {
        StringBuilder schemaLocation = new StringBuilder();
        try {
            for (int i = 0; i < schemaLocations.length - 1; i += 2) {
                schemaLocation.append(" ");
                schemaLocation
                        .append(schemaLocations[i])
                        .append(" ")
                        .append(schemaLocations[i + 1]);
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new ServiceException(
                    "Extended capabilities provider returned improper "
                            + "set of namespace,location pairs from getSchemaLocations()",
                    e);
        }

        return schemaLocation.toString();
    }
}
