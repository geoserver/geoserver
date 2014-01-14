package org.geoserver.csw.records;

import org.eclipse.xsd.XSDComplexTypeDefinition;
import org.eclipse.xsd.XSDTypeDefinition;
import org.geotools.data.complex.config.FeatureTypeRegistryHelper;

/**
 * Simple helper for FeatureTypeRegistry, creates feature type for particular name
 * 
 * @author Niels Charlier
 *
 */
public class RecordFeatureTypeRegistryHelper implements FeatureTypeRegistryHelper {
    
    protected String recordFeatureTypeName;
    
    public RecordFeatureTypeRegistryHelper(String recordFeatureTypeName) {
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

}
