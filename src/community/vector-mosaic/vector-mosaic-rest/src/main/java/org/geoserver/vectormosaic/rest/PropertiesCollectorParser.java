/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.vectormosaic.rest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import org.geotools.gce.imagemosaic.properties.PropertiesCollector;
import org.geotools.gce.imagemosaic.properties.PropertiesCollectorBean;

/** provides support methods to extract {@link PropertiesCollector} definitions from properties String. */
public class PropertiesCollectorParser {

    public static final String PROPERTY_COLLECTORS = "PropertyCollectors";

    public List<PropertiesCollector> parse(Properties storeProperties) {
        List<String> pcConfigs = extractCollectorStrings(storeProperties);
        return pcConfigs.stream()
                .map(PropertiesCollectorParser::getBean)
                .map(PropertiesCollectorBean::getCollector)
                .toList();
    }

    /**
     * Extract the PropertiesCollectorBean from a property definition. For example:
     * TimestampFileNameExtractorSPI[[0-9]{8}T[0-9]{6},format=yyyyMMdd'T'HHmmss](time)
     *
     * @param definition
     * @return
     */
    public static PropertiesCollectorBean getBean(String definition) {
        int bOpen = definition.indexOf('[');
        int bClose = definition.lastIndexOf(']');
        int pOpen = definition.indexOf('(', bClose);
        int pClose = definition.lastIndexOf(')');
        String spi = definition.substring(0, bOpen);
        String config = definition.substring(bOpen + 1, bClose);
        String propertyName = definition.substring(pOpen + 1, pClose);
        return new PropertiesCollectorBean(spi.trim(), config.trim(), propertyName.trim());
    }

    /**
     * Extract the PropertyCollectors defined in the provided Properties. Note that PropertyCollectors String may
     * contain multiple SPIs definitions separated by a comma.
     *
     * @return
     */
    public static List<String> extractCollectorStrings(Properties properties) {
        if (!properties.containsKey(PROPERTY_COLLECTORS)) {
            return Collections.emptyList();
        }
        String definition = (String) properties.get(PROPERTY_COLLECTORS);
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int bracketDepth = 0;
        int parenDepth = 0;

        for (int i = 0; i < definition.length(); i++) {
            char c = definition.charAt(i);

            if (c == '[') bracketDepth++;
            else if (c == ']') bracketDepth--;
            else if (c == '(') parenDepth++;
            else if (c == ')') parenDepth--;

            if (c == ',' && bracketDepth == 0 && parenDepth == 0) {
                result.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        if (!current.isEmpty()) {
            result.add(current.toString().trim());
        }

        return result;
    }
}
