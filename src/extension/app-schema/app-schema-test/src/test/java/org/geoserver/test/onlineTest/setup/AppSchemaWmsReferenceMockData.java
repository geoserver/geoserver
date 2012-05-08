/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test.onlineTest.setup;

import org.geoserver.test.AbstractAppSchemaMockData;

public class AppSchemaWmsReferenceMockData extends AbstractAppSchemaMockData {
    @Override
    protected void addContent() {
        addFeatureType(GSML_PREFIX, "Contact", "WmsDataReferenceData/gsml_Contact/gsml_Contact.xml");
        addFeatureType(GSML_PREFIX, "MappedFeature",
                "WmsDataReferenceData/gsml_MappedFeature/gsml_MappedFeature.xml",
                "WmsDataReferenceData/gsml_MappedFeature/MF_CGITermValue.xml");

        addFeatureType(GSML_PREFIX, "DisplacementEvent",
                "WmsDataReferenceData/gsml_DisplacementEvent/gsml_DisplacementEvent.xml");

        addFeatureType(GSML_PREFIX, "GeologicEvent",
                "WmsDataReferenceData/gsml_GeologicEvent/gsml_GeologicEvent.xml",
                "WmsDataReferenceData/gsml_GeologicEvent/GE_CGITermValue.xml");

        addFeatureType(GSML_PREFIX, "GeologicUnit",
                "WmsDataReferenceData/gsml_GeologicUnit/gsml_GeologicUnit.xml",
                "WmsDataReferenceData/gsml_GeologicUnit/gsml_PhysicalDescription.xml",
                "WmsDataReferenceData/gsml_GeologicUnit/GU_CGITermValue.xml",
                "WmsDataReferenceData/gsml_CompositionPart/gsml_CompositionPart.xml",
                "WmsDataReferenceData/gsml_CompositionPart/CP_CGITermValue.xml",
                "WmsDataReferenceData/gsml_CompositionPart/gsml_ConstituentPart.xml",
                "WmsDataReferenceData/gsml_CompositionPart/gsml_Mineral.xml",
                "WmsDataReferenceData/gsml_CompositionPart/RockMaterial.xml");

        addFeatureType(GSML_PREFIX, "ShearDisplacementStructure",
                "WmsDataReferenceData/gsml_ShearDisplacementStructure/gsml_ShearDisplacementStructure.xml");
    }
}
