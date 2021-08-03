/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.web;

import org.geoserver.platform.exception.GeoServerException;

public class TemplateConfigurationException extends GeoServerException {

    public static final String MISSING_PREVIEW_OUTPUT_FORMAT = "MISSING_PREVIEW_OUTPUT_FORMAT";
    public static final String MISSING_PREVIEW_FEATURE_TYPE = "MISSING_PREVIEW_FEATURE_TYPE";
    public static final String MISSING_PREVIEW_WORKSPACE = "MISSING_PREVIEW_WORKSPACE";

    public static final String MISSING_RULE_OUTPUT_FORMAT = "MISSING_RULE_OUTPUT_FORMAT";
    public static final String MISSING_RULE_TEMPLATE_NAME = "MISSING_RULE_TEMPLATE_NAME";
    public static final String INCOMPATIBLE_OUTPUT_FORMAT = "INCOMPATIBLE_OUTPUT_FORMAT";

    public static final String DUPLICATE_TEMPLATE_NAME = "DUPLICATE_TEMPLATE_NAME";
}
