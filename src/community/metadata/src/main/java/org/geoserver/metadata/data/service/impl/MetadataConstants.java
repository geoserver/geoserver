/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.service.impl;

import java.sql.Date;
import org.locationtech.jts.geom.Geometry;

public final class MetadataConstants {

    public static final String DIRECTORY = "metadata";

    public static final String TEMPLATES_DIRECTORY = "metadata-template";

    public static final String CUSTOM_METADATA_KEY = "custom";

    public static final String DERIVED_KEY = "custom-derived-attributes";

    public static final String TIMESTAMP_KEY = "_timestamp";

    public static final String FEATURE_CATALOG_CONFIG_FILE = "featureCatalog.yaml";

    public static final String FEATURE_ATTRIBUTE_TYPENAME = "featureAttribute";

    public static final String FEATURE_ATTRIBUTE_NAME = "name";

    public static final String FEATURE_ATTRIBUTE_TYPE = "type";

    public static final String FEATURE_ATTRIBUTE_LENGTH = "length";

    public static final String FEATURE_ATTRIBUTE_MIN_OCCURRENCE = "min-occurrence";

    public static final String FEATURE_ATTRIBUTE_MAX_OCCURRENCE = "max-occurrence";

    public static final String FEATURE_ATTRIBUTE_DOMAIN = "domain";

    public static final String DOMAIN_TYPENAME = "domain";

    public static final String DOMAIN_ATT_VALUE = "value";

    public static final String DOMAIN_ATT_DEFINITION = "definition";

    public static final Class<?>[] FEATURE_CATALOG_KNOWN_TYPES =
            new Class<?>[] {String.class, Number.class, Geometry.class, Date.class, Boolean.class};

    public static final String FEATURECATALOG_TYPE_UNKNOWN = "unknown";

    private MetadataConstants() {}
}
