/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.records;

import java.util.Collection;
import java.util.Collections;
import org.eclipse.xsd.XSDComplexTypeDefinition;
import org.eclipse.xsd.XSDTypeDefinition;
import org.geotools.xsd.Configuration;
import org.geotools.xsd.complex.FeatureTypeRegistryConfiguration;
import org.opengis.feature.type.Schema;

/**
 * Simple helper for FeatureTypeRegistry, creates feature type for particular name
 *
 * @author Niels Charlier
 */
public class RecordFeatureTypeRegistryConfiguration implements FeatureTypeRegistryConfiguration {

    protected String recordFeatureTypeName;

    public RecordFeatureTypeRegistryConfiguration(String recordFeatureTypeName) {
        this.recordFeatureTypeName = recordFeatureTypeName;
    }

    @Override
    public boolean isFeatureType(XSDTypeDefinition typeDefinition) {
        return recordFeatureTypeName.equals(typeDefinition.getName());
    }

    @Override
    public boolean isGeometryType(XSDTypeDefinition typeDefinition) {
        return false;
    }

    @Override
    public boolean isIdentifiable(XSDComplexTypeDefinition typeDefinition) {
        return false;
    }

    @Override
    public Collection<Schema> getSchemas() {
        return Collections.EMPTY_SET;
    }

    @Override
    public Collection<Configuration> getConfigurations() {
        return Collections.EMPTY_SET;
    }
}
