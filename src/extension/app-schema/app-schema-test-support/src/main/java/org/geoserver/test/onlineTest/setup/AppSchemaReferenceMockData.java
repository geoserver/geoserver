/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test.onlineTest.setup;

import org.geoserver.test.AbstractAppSchemaMockData;

/**
 * @author Niels Charlier
 *     <p>Mock data for Wms Online tests - uses some different mappings to accomodate wms
 */
public class AppSchemaReferenceMockData extends AbstractAppSchemaMockData {
    @Override
    protected void addContent() {
        addFeatureType(GSML_PREFIX, "Contact", "DataReferenceData/gsml_Contact/gsml_Contact.xml");
        addFeatureType(
                GSML_PREFIX,
                "MappedFeature",
                "DataReferenceData/gsml_MappedFeature/gsml_MappedFeature.xml",
                "DataReferenceData/gsml_MappedFeature/MF_CGITermValue.xml");

        //        addFeatureType(GSML_PREFIX, "CompositionPart",
        //                "DataReferenceData/gsml_CompositionPart/gsml_CompositionPart.xml",
        //                "DataReferenceData/gsml_CompositionPart/CGITermValue.xml",
        //                "DataReferenceData/gsml_CompositionPart/gsml_ConstituentPart.xml",
        //                "DataReferenceData/gsml_CompositionPart/gsml_Mineral.xml",
        //                "DataReferenceData/gsml_CompositionPart/RockMaterial.xml");

        addFeatureType(
                GSML_PREFIX,
                "DisplacementEvent",
                "DataReferenceData/gsml_DisplacementEvent/gsml_DisplacementEvent.xml");

        addFeatureType(
                GSML_PREFIX,
                "GeologicEvent",
                "DataReferenceData/gsml_GeologicEvent/gsml_GeologicEvent.xml",
                "DataReferenceData/gsml_GeologicEvent/GE_CGITermValue.xml");

        addFeatureType(
                GSML_PREFIX,
                "GeologicUnit",
                "DataReferenceData/gsml_GeologicUnit/gsml_GeologicUnit.xml",
                "DataReferenceData/gsml_GeologicUnit/gsml_PhysicalDescription.xml",
                "DataReferenceData/gsml_GeologicUnit/GU_CGITermValue.xml",
                "DataReferenceData/gsml_CompositionPart/gsml_CompositionPart.xml",
                "DataReferenceData/gsml_CompositionPart/CP_CGITermValue.xml",
                "DataReferenceData/gsml_CompositionPart/gsml_ConstituentPart.xml",
                "DataReferenceData/gsml_CompositionPart/gsml_Mineral.xml",
                "DataReferenceData/gsml_CompositionPart/RockMaterial.xml");

        addFeatureType(
                GSML_PREFIX,
                "ShearDisplacementStructure",
                "DataReferenceData/gsml_ShearDisplacementStructure/gsml_ShearDisplacementStructure.xml");
    }
}
