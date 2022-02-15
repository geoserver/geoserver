/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.opengis.feature.type.FeatureType;

/** Parses and validates a comma separate list of properties */
public class PropertiesParser {

    ResourceInfo resource;

    public PropertiesParser(ResourceInfo resource) {
        this.resource = resource;
    }

    /** Parses a comma separated list of properties and */
    public List<String> parse(String spec) throws IOException {
        List<String> names = Arrays.stream(spec.split("\\s*,\\s*")).collect(Collectors.toList());
        validate(names);
        return names;
    }

    /**
     * Validates the names, currently supports only vector layers (and mostly simple feature ones)
     *
     * @param names
     * @throws IOException
     */
    protected void validate(List<String> names) throws IOException {
        if (resource instanceof FeatureTypeInfo) {
            FeatureType schema = ((FeatureTypeInfo) resource).getFeatureType();
            Set<String> availableNames =
                    schema.getDescriptors().stream()
                            .map(p -> p.getName().getLocalPart())
                            .collect(Collectors.toSet());
            if (!availableNames.containsAll(names)) {
                throw new IOException("Names does not match schema");
            }
        }
    }
}
