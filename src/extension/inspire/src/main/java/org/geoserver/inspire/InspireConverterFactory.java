/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.inspire;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.wfs.WFSInfo;
import org.geotools.util.Converter;
import org.geotools.util.ConverterFactory;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;

/**
 * Marshals {@link UniqueResourceIdentifiers} to and from String for including in the {@link
 * WFSInfo} {@link MetadataMap}
 *
 * @author Andrea Aime - GeoSolutions
 */
public class InspireConverterFactory implements ConverterFactory {
    static final Logger LOGGER = Logging.getLogger(InspireConverterFactory.class);

    @Override
    public Converter createConverter(Class<?> source, Class<?> target, Hints hints) {
        if (String.class.isAssignableFrom(source)
                && UniqueResourceIdentifiers.class.isAssignableFrom(target)) {
            return new SpatialDatasetIdentifiersConverter();
        } else if (String.class.isAssignableFrom(target)
                && UniqueResourceIdentifiers.class.isAssignableFrom(source)) {
            return new SpatialDatasetIdentifiersConverter();
        }
        return null;
    }

    public class SpatialDatasetIdentifiersConverter implements Converter {

        @Override
        public <T> T convert(Object source, Class<T> target) throws Exception {
            if (source instanceof String && UniqueResourceIdentifiers.class.equals(target)) {
                UniqueResourceIdentifiers identifiers = new UniqueResourceIdentifiers();
                String[] values = ((String) source).split("\\s*;\\s*");
                for (int i = 0; i < values.length; i++) {
                    String value = values[i];
                    String[] elements = value.split("\\s*,\\s*");
                    if (elements.length == 0) {
                        continue;
                    }
                    String code = elements[0];
                    if ("".equals(code)) {
                        LOGGER.warning(
                                "Skipping InspireDatasetIdentifier because code is empty: "
                                        + value);
                        continue;
                    }
                    String namespace = null;
                    if (elements.length > 1) {
                        namespace = elements[1];
                        try {
                            new URI(namespace);
                        } catch (URISyntaxException e) {
                            LOGGER.log(
                                    Level.WARNING,
                                    "Skipping InspireDatasetIdentifier because "
                                            + "namespace is not a valid URI: "
                                            + value,
                                    e);
                            continue;
                        }
                        if (namespace.trim().equals("")) {
                            namespace = null;
                        }
                    }
                    String metadataURL = null;
                    if (elements.length > 2) {
                        metadataURL = elements[2];
                        try {
                            new URI(metadataURL);
                        } catch (URISyntaxException e) {
                            LOGGER.log(
                                    Level.WARNING,
                                    "Skipping InspireDatasetIdentifier because "
                                            + "metadataURL is not a valid URI: "
                                            + value,
                                    e);
                            continue;
                        }
                    }
                    UniqueResourceIdentifier id =
                            new UniqueResourceIdentifier(code, namespace, metadataURL);
                    identifiers.add(id);
                }
                return (T) identifiers;
            } else if (source instanceof UniqueResourceIdentifiers && String.class.equals(target)) {
                UniqueResourceIdentifiers ids = (UniqueResourceIdentifiers) source;
                if (ids.size() > 0) {
                    StringBuilder sb = new StringBuilder();
                    for (UniqueResourceIdentifier id : ids) {
                        String ns = id.getNamespace();
                        String metadata = id.getMetadataURL();
                        String code = id.getCode();
                        sb.append(code).append(",");
                        if (ns != null) {
                            sb.append(ns);
                        }
                        sb.append(",");
                        if (metadata != null) {
                            sb.append(metadata);
                        }
                        sb.append(";");
                    }
                    sb.setLength(sb.length() - 1);
                    return (T) sb.toString();
                } else {
                    return (T) "";
                }
            }

            return null;
        }
    }
}
