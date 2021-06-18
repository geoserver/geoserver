/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.validation;

import java.io.IOException;
import org.geoserver.catalog.FeatureTypeInfo;
import org.opengis.feature.type.FeatureType;

/**
 * This class perform a validation of a template by evaluating dynamic and source fields using
 * {@link ValidateExpressionVisitor}
 */
public class TemplateValidator extends AbstractTemplateValidator {

    private FeatureTypeInfo type;

    public TemplateValidator(FeatureTypeInfo type) {
        this.type = type;
    }

    @Override
    protected FeatureType getFeatureType() throws IOException {
        return type.getFeatureType();
    }

    @Override
    public String getTypeName() {
        return type.getNativeName();
    }
}
