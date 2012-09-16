/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.opengis.cat.csw20.Csw20Factory;
import net.opengis.ows10.DomainType;
import net.opengis.ows10.Ows10Factory;

import org.geoserver.csw.records.CSWRecordDescriptor;
import org.opengis.feature.type.Name;

/**
 * Represents the capabilities of a {@link CatalogStore}
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class CatalogCapabilities {
    
    private static final String version = "2.0.2";

    static Csw20Factory cswf = Csw20Factory.eINSTANCE;
    static Ows10Factory owsf = Ows10Factory.eINSTANCE;

    static public Map<String, List<DomainType>> operationParameters = new HashMap<String, List<DomainType>>();
    static public Map<String, List<DomainType>> operationConstraints = new HashMap<String, List<DomainType>>();

    static
    {
        /** 
         * OperationMetadata 
         **/
        operationParameters.put("OperationsMetadata", new LinkedList<DomainType>());
        operationConstraints.put("OperationsMetadata", new LinkedList<DomainType>());
        
        // - Parameters
        DomainType opMetadataParam1 = owsf.createDomainType();
        DomainType opMetadataParam2 = owsf.createDomainType();
        opMetadataParam1.setName("service");
        opMetadataParam1.getValue().add("http://www.opengis.net/cat/csw/2.0.2");
        opMetadataParam2.setName("version");
        opMetadataParam2.getValue().add(version);
        operationParameters.get("OperationsMetadata").add(opMetadataParam1);
        operationParameters.get("OperationsMetadata").add(opMetadataParam2);
        
        // - Constraints
        DomainType opMetadataConstraint1 = owsf.createDomainType();
        opMetadataConstraint1.setName("PostEncoding");
        opMetadataConstraint1.getValue().add("XML");
        operationConstraints.get("OperationsMetadata").add(opMetadataConstraint1);
        
        /** 
         * GetCapabilities 
         **/
        operationParameters.put("GetCapabilities", new LinkedList<DomainType>());
        operationConstraints.put("GetCapabilities", new LinkedList<DomainType>());

        // - Parameters
        DomainType getCapabilitiesParam = owsf.createDomainType();
        getCapabilitiesParam.setName("sections");
        getCapabilitiesParam.getValue().add("ServiceIdentification");
        getCapabilitiesParam.getValue().add("ServiceProvider");
        getCapabilitiesParam.getValue().add("OperationsMetadata");
        getCapabilitiesParam.getValue().add("Filter_Capabilities");
        operationParameters.get("GetCapabilities").add(getCapabilitiesParam);

        // - Constraints
        DomainType getCapabilitiesConstraint = owsf.createDomainType();
        getCapabilitiesConstraint.setName("PostEncoding");
        getCapabilitiesConstraint.getValue().add("XML");
        operationConstraints.get("GetCapabilities").add(getCapabilitiesConstraint);

        /** 
         * DescribeRecord 
         **/
        operationParameters.put("DescribeRecord", new LinkedList<DomainType>());
        operationConstraints.put("DescribeRecord", new LinkedList<DomainType>());
        
        // - Parameters
        DomainType describeRecordParam1 = owsf.createDomainType();
        DomainType describeRecordParam2 = owsf.createDomainType();
        DomainType describeRecordParam3 = owsf.createDomainType();
        describeRecordParam1.setName("typeName");
        describeRecordParam1.getValue().add("csw:Record");
        describeRecordParam1.getValue().add("gmd:MD_Metadata");
        describeRecordParam2.setName("outputFormat");
        describeRecordParam2.getValue().add("application/xml");
        describeRecordParam3.setName("schemaLanguage");
        describeRecordParam3.getValue().add("http://www.w3.org/TR/xmlschema-1/");
        operationParameters.get("DescribeRecord").add(describeRecordParam1);
        operationParameters.get("DescribeRecord").add(describeRecordParam2);
        operationParameters.get("DescribeRecord").add(describeRecordParam3);

        // - Constraints
        DomainType describeRecordConstraint = owsf.createDomainType();
        describeRecordConstraint.setName("PostEncoding");
        describeRecordConstraint.getValue().add("XML");
        operationConstraints.get("DescribeRecord").add(describeRecordConstraint);
        
        /** 
         * GetRecords 
         **/
        operationParameters.put("GetRecords", new LinkedList<DomainType>());
        operationConstraints.put("GetRecords", new LinkedList<DomainType>());
        
        // - Parameters
        DomainType getRecordsParam1 = owsf.createDomainType();
        DomainType getRecordsParam2 = owsf.createDomainType();
        DomainType getRecordsParam3 = owsf.createDomainType();
        DomainType getRecordsParam4 = owsf.createDomainType();
        DomainType getRecordsParam5 = owsf.createDomainType();
        getRecordsParam1.setName("resultType");
        getRecordsParam1.getValue().add("hits");
        getRecordsParam1.getValue().add("results");
        getRecordsParam1.getValue().add("validate");
        getRecordsParam2.setName("outputFormat");
        getRecordsParam2.getValue().add("application/xml");
        getRecordsParam3.setName("outputSchema");
        getRecordsParam3.getValue().add("http://www.opengis.net/cat/csw/2.0.2");
        getRecordsParam3.getValue().add("http://www.isotc211.org/2005/gmd");
        getRecordsParam4.setName("typeNames");
        getRecordsParam4.getValue().add("csw:Record");
        getRecordsParam4.getValue().add("gmd:MD_Metadata");
        getRecordsParam5.setName("CONSTRAINTLANGUAGE");
        getRecordsParam5.getValue().add("FILTER");
        getRecordsParam5.getValue().add("CQL_TEXT");
        operationParameters.get("GetRecords").add(getRecordsParam1);
        operationParameters.get("GetRecords").add(getRecordsParam2);
        operationParameters.get("GetRecords").add(getRecordsParam3);
        operationParameters.get("GetRecords").add(getRecordsParam4);
        operationParameters.get("GetRecords").add(getRecordsParam5);

        // - Constraints
        DomainType getRecordConstraint1 = owsf.createDomainType();
        DomainType getRecordConstraint2 = owsf.createDomainType();
        getRecordConstraint1.setName("PostEncoding");
        getRecordConstraint1.getValue().add("XML");
        getRecordConstraint2.setName("AdditionalQueryables");
        getRecordConstraint2.getValue().add("SpecificationDate");
        getRecordConstraint2.getValue().add("ConditionApplyingToAccessAndUse");
        getRecordConstraint2.getValue().add("AccessConstraints");
        getRecordConstraint2.getValue().add("MetadataPointOfContact");
        getRecordConstraint2.getValue().add("SpecificationDateType");
        getRecordConstraint2.getValue().add("Classification");
        getRecordConstraint2.getValue().add("OtherConstraints");
        getRecordConstraint2.getValue().add("Degree");
        getRecordConstraint2.getValue().add("Lineage");
        getRecordConstraint2.getValue().add("SpecificationTitle");
        operationConstraints.get("GetRecords").add(getRecordConstraint1);
        //operationConstraints.get("GetRecords").add(getRecordConstraint2);
        
        /** 
         * GetRecordById 
         **/
        operationParameters.put("GetRecordById", new LinkedList<DomainType>());
        operationConstraints.put("GetRecordById", new LinkedList<DomainType>());
        
        // - Parameters
        DomainType getRecordByIdParam1 = owsf.createDomainType();
        DomainType getRecordByIdParam2 = owsf.createDomainType();
        DomainType getRecordByIdParam3 = owsf.createDomainType();
        DomainType getRecordByIdParam4 = owsf.createDomainType();
        getRecordByIdParam1.setName("resultType");
        getRecordByIdParam1.getValue().add("hits");
        getRecordByIdParam1.getValue().add("results");
        getRecordByIdParam1.getValue().add("validate");
        getRecordByIdParam2.setName("outputFormat");
        getRecordByIdParam2.getValue().add("application/xml");
        getRecordByIdParam3.setName("outputSchema");
        getRecordByIdParam3.getValue().add("http://www.opengis.net/cat/csw/2.0.2");
        getRecordByIdParam3.getValue().add("http://www.isotc211.org/2005/gmd");
        getRecordByIdParam4.setName("ElementSetName");
        getRecordByIdParam4.getValue().add("brief");
        getRecordByIdParam4.getValue().add("summary");
        getRecordByIdParam4.getValue().add("full");
        operationParameters.get("GetRecordById").add(getRecordByIdParam1);
        operationParameters.get("GetRecordById").add(getRecordByIdParam2);
        operationParameters.get("GetRecordById").add(getRecordByIdParam3);
        operationParameters.get("GetRecordById").add(getRecordByIdParam4);

        // - Constraints
        DomainType getRecordByIdConstraint1 = owsf.createDomainType();
        getRecordByIdConstraint1.setName("PostEncoding");
        getRecordByIdConstraint1.getValue().add("XML");
        operationConstraints.get("GetRecordById").add(getRecordByIdConstraint1);

        /** 
         * GetDomain 
         **/
        operationParameters.put("GetDomain", new LinkedList<DomainType>());
        operationConstraints.put("GetDomain", new LinkedList<DomainType>());
        
        // - Parameters
        DomainType getDomainParam1 = owsf.createDomainType();
        DomainType getDomainParam2 = owsf.createDomainType();
        getDomainParam1.setName("parameterName");
        getDomainParam1.getValue().add("xsd:anyURI");
        getDomainParam2.setName("propertyName");
        getDomainParam2.getValue().add("xsd:anyURI");
        operationParameters.get("GetDomain").add(getDomainParam1);
        operationParameters.get("GetDomain").add(getDomainParam2);
        
        /** 
         * Transaction 
         **/
        operationParameters.put("Transaction", new LinkedList<DomainType>());
        operationConstraints.put("Transaction", new LinkedList<DomainType>());
        
    }
    
    /**
     * @return the version
     */
    public static String getVersion() {
        return version;
    }

    /**
     * True if the store supports transactions (insert, update, delete), false otherwise
     */
    public boolean supportsTransactions() {
        return false;
    }

    /**
     * Returns the list of queriable properties supported by this implementation for the given type
     * name (empty by default)
     * 
     * @param typeName
     * @return
     */
    public List<Name> getQueriables(Name typeName) {
        return Collections.emptyList();
    }
    
    /**
     * Returns the list of queriable properties for which an enumeration of the domain makes sense
     * 
     * @param typeName
     * @return
     */
    public List<Name> getDomainQueriables(Name typeName) {
        List<Name> queriables = new ArrayList<Name>();
        
        for(Name property : CSWRecordDescriptor.SUMMARY_ELEMENTS)
        {
            if (property.equals(typeName))
            {
                queriables.add(property);
            }
        }
        
        return queriables;
    }
}
