/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import net.opengis.ows10.DomainType;
import net.opengis.ows10.Ows10Factory;
import org.geoserver.csw.records.RecordDescriptor;
import org.opengis.feature.type.Name;

/**
 * Represents the capabilities of a {@link CatalogStore}
 *
 * @author Andrea Aime - GeoSolutions
 */
public class CatalogStoreCapabilities {

    public static final String CSW_VERSION = "2.0.2";

    protected Map<Name, RecordDescriptor> descriptors;

    protected Map<String, List<DomainType>> operationParameters =
            new HashMap<String, List<DomainType>>();
    protected Map<String, List<DomainType>> operationConstraints =
            new HashMap<String, List<DomainType>>();

    public CatalogStoreCapabilities(Map<Name, RecordDescriptor> descriptors) {
        this.descriptors = descriptors;
        initialize();
    }

    private void initialize() {
        Ows10Factory owsf = Ows10Factory.eINSTANCE;

        /** OperationMetadata */
        operationParameters.put("OperationsMetadata", new LinkedList<DomainType>());
        operationConstraints.put("OperationsMetadata", new LinkedList<DomainType>());

        // - Parameters
        DomainType opMetadataParam1 = owsf.createDomainType();
        DomainType opMetadataParam2 = owsf.createDomainType();
        opMetadataParam1.setName("service");
        opMetadataParam1.getValue().add("http://www.opengis.net/cat/csw/2.0.2");
        opMetadataParam2.setName("version");
        opMetadataParam2.getValue().add(CSW_VERSION);
        operationParameters.get("OperationsMetadata").add(opMetadataParam1);
        operationParameters.get("OperationsMetadata").add(opMetadataParam2);

        // - Constraints
        DomainType opMetadataConstraint1 = owsf.createDomainType();
        opMetadataConstraint1.setName("PostEncoding");
        opMetadataConstraint1.getValue().add("XML");
        operationConstraints.get("OperationsMetadata").add(opMetadataConstraint1);

        /** GetCapabilities */
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

        /** DescribeRecord */
        operationParameters.put("DescribeRecord", new LinkedList<DomainType>());
        operationConstraints.put("DescribeRecord", new LinkedList<DomainType>());

        // prepare typenames and schema's
        List<String> typeNames = new ArrayList<>();
        List<String> outputSchemas = new ArrayList<>();
        for (RecordDescriptor rd : descriptors.values()) {
            typeNames.add(rd.getFeatureDescriptor().getName().toString());
            outputSchemas.add(rd.getOutputSchema());
        }

        // - Parameters
        DomainType describeRecordParam1 = owsf.createDomainType();
        DomainType describeRecordParam2 = owsf.createDomainType();
        DomainType describeRecordParam3 = owsf.createDomainType();
        describeRecordParam1.setName("typeName");
        describeRecordParam1.getValue().addAll(typeNames);
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

        /** GetRecords */
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
        getRecordsParam3.getValue().addAll(outputSchemas);
        getRecordsParam4.setName("typeNames");
        getRecordsParam4.getValue().addAll(typeNames);
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
        getRecordConstraint1.setName("PostEncoding");
        getRecordConstraint1.getValue().add("XML");
        operationConstraints.get("GetRecords").add(getRecordConstraint1);

        /** GetRecordById */
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
        getRecordByIdParam3.getValue().addAll(outputSchemas);
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

        /** GetDomain */
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

        /** Transaction */
        operationParameters.put("Transaction", new LinkedList<DomainType>());
        operationConstraints.put("Transaction", new LinkedList<DomainType>());
    }

    /** True if the store supports transactions (insert, update, delete), false otherwise */
    public boolean supportsTransactions() {
        return false;
    }

    /**
     * Returns the list of queriable properties supported by this implementation for the given type
     * name (empty by default)
     *
     * @param typeName Qualified name (with namespace)
     */
    public List<Name> getQueriables(Name typeName) {
        return descriptors.get(typeName).getQueryables();
    }

    /**
     * Returns the list of queriable properties for which an enumeration of the domain makes sense
     *
     * @param typeName Qualified name (with namespace)
     */
    public List<Name> getDomainQueriables(Name typeName) {
        return descriptors.get(typeName).getQueryables();
    }

    /**
     * Returns true if GetRepositoryItem is supported on the specified type
     *
     * @param typeName Qualified name (with namespace)
     */
    public boolean supportsGetRepositoryItem(Name typeName) {
        return false;
    }

    public Map<String, List<DomainType>> getOperationParameters() {
        return operationParameters;
    }

    public Map<String, List<DomainType>> getOperationConstraints() {
        return operationConstraints;
    }
}
