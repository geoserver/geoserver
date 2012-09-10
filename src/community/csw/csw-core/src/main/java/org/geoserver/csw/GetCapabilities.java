/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import net.opengis.cat.csw20.CapabilitiesType;
import net.opengis.cat.csw20.Csw20Factory;
import net.opengis.cat.csw20.GetCapabilitiesType;
import net.opengis.fes20.Fes20Factory;
import net.opengis.ows10.AddressType;
import net.opengis.ows10.CodeType;
import net.opengis.ows10.ContactType;
import net.opengis.ows10.DCPType;
import net.opengis.ows10.DomainType;
import net.opengis.ows10.HTTPType;
import net.opengis.ows10.KeywordsType;
import net.opengis.ows10.OnlineResourceType;
import net.opengis.ows10.OperationType;
import net.opengis.ows10.OperationsMetadataType;
import net.opengis.ows10.Ows10Factory;
import net.opengis.ows10.RequestMethodType;
import net.opengis.ows10.ResponsiblePartySubsetType;
import net.opengis.ows10.SectionsType;
import net.opengis.ows10.ServiceIdentificationType;
import net.opengis.ows10.ServiceProviderType;
import net.opengis.ows10.TelephoneType;

import org.geoserver.catalog.KeywordInfo;
import org.geoserver.config.ContactInfo;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.RequestUtils;
import org.geoserver.ows.util.ResponseUtils;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.util.logging.Logging;
import org.geotools.xml.EMFUtils;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.capability.ArithmeticOperators;
import org.opengis.filter.capability.ComparisonOperators;
import org.opengis.filter.capability.FilterCapabilities;
import org.opengis.filter.capability.GeometryOperand;
import org.opengis.filter.capability.IdCapabilities;
import org.opengis.filter.capability.Operator;
import org.opengis.filter.capability.ScalarCapabilities;
import org.opengis.filter.capability.SpatialCapabilities;
import org.opengis.filter.capability.SpatialOperator;
import org.opengis.filter.capability.SpatialOperators;
import org.springframework.context.ApplicationContext;

/**
 * The CSW GetCapabilities implementation
 * 
 * @author Alessio Fabiani - GeoSolutions
 * 
 */
public class GetCapabilities {
    static final Logger LOGGER = Logging.getLogger(GetCapabilities.class);

    private static final boolean logicalOperators = true;

    private static final boolean eid = true;

    private static final boolean fid = true;

    private static final boolean isTransactional = true;

    public CSWInfo csw;

    ApplicationContext context;

    public GetCapabilities(CSWInfo csw, ApplicationContext context) {
        this.csw = csw;
        this.context = context;
    }

    public CapabilitiesType run(GetCapabilitiesType request) throws CSWException {
        // do the version negotiation dance
        SectionsType sections = null;
        List<String> provided = Collections.singletonList("2.0.2");
        List<String> accepted = null;
        if (request.getAcceptVersions() != null)
            accepted = request.getAcceptVersions().getVersion();
        String version = RequestUtils.getVersionOws11(provided, accepted);

        if (!"2.0.2".equals(version)) {
            throw new CSWException("Could not understand version:" + version);
        }

        if (request.getSections() != null) {
            sections = request.getSections();
        }

        // encode the response
        Csw20Factory cswf = Csw20Factory.eINSTANCE;
        Ows10Factory owsf = Ows10Factory.eINSTANCE;
        Fes20Factory fesf = Fes20Factory.eINSTANCE;

        CapabilitiesType caps = cswf.createCapabilitiesType();
        caps.setVersion("2.0.2");

        // ServiceIdentification
        if (sections == null || requestedSection("ServiceIdentification", sections)) {
            ServiceIdentificationType si = owsf.createServiceIdentificationType();
            caps.setServiceIdentification(si);

            si.setTitle(csw.getTitle());
            si.setAbstract(csw.getAbstract());

            KeywordsType kw = null;
            List<KeywordInfo> keywords = csw.getKeywords();
            if (keywords != null && keywords.size() > 0) {
                kw = owsf.createKeywordsType();
                for (KeywordInfo keyword : keywords) {
                    kw.getKeyword().add(keyword.getValue());
                }
            }

            if (kw != null) {
                si.getKeywords().add(kw);
            }

            CodeType CSW = owsf.createCodeType();
            CSW.setValue("CSW");
            si.setServiceType(CSW);
            si.setServiceTypeVersion("2.0.2");
            si.setFees(csw.getFees());

            if (csw.getAccessConstraints() != null) {
                si.setAccessConstraints(csw.getAccessConstraints());
            }
        }

        // ServiceProvider
        if (sections == null || requestedSection("ServiceProvider", sections)) {
            ServiceProviderType sp = owsf.createServiceProviderType();
            caps.setServiceProvider(sp);

            final ContactInfo contact = csw.getGeoServer().getGlobal().getSettings().getContact();

            sp.setProviderName(contact.getContactOrganization());

            OnlineResourceType providerSite = owsf.createOnlineResourceType();
            sp.setProviderSite(providerSite);
            providerSite.setHref(csw.getOnlineResource());

            ResponsiblePartySubsetType serviceContact = owsf.createResponsiblePartySubsetType();
            sp.setServiceContact(serviceContact);
            serviceContact.setIndividualName(contact.getContactPerson());
            serviceContact.setPositionName(contact.getContactPosition());

            ContactType contactInfo = owsf.createContactType();
            serviceContact.setContactInfo(contactInfo);
            AddressType address = owsf.createAddressType();
            contactInfo.setAddress(address);
            address.setAdministrativeArea(contact.getAddressState());
            address.setCity(contact.getAddressCity());
            address.setCountry(contact.getAddressCountry());
            address.setDeliveryPoint(null);
            address.setElectronicMailAddress(contact.getContactEmail());
            address.setPostalCode(contact.getAddressPostalCode());

            contactInfo.setContactInstructions(null);
            contactInfo.setHoursOfService(null);

            OnlineResourceType onlineResource = owsf.createOnlineResourceType();
            contactInfo.setOnlineResource(onlineResource);
            onlineResource.setHref(contact.getOnlineResource());

            TelephoneType telephone = owsf.createTelephoneType();
            contactInfo.setPhone(telephone);
            telephone.setFacsimile(contact.getContactFacsimile());
            telephone.setVoice(contact.getContactVoice());

            serviceContact.setRole(null);
        }

        // Operations Metadata
        if (sections == null || requestedSection("OperationsMetadata", sections)) {
            final String baseUrl = (String) EMFUtils.get(request, "baseUrl");
            if (baseUrl == null) {
                throw new IllegalArgumentException("Request object" + request
                        + " has no 'baseUrl' property.");
            }

            OperationsMetadataType operationsMetadata = owsf.createOperationsMetadataType();
            caps.setOperationsMetadata(operationsMetadata);

            OperationMetadata_GetCapabilities(owsf, baseUrl, operationsMetadata);
            OperationMetadata_DescribeRecord(owsf, baseUrl, operationsMetadata);
            OperationMetadata_GetDomain(owsf, baseUrl, operationsMetadata);
            OperationMetadata_GetRecords(owsf, baseUrl, operationsMetadata);
            OperationMetadata_GetRecordById(owsf, baseUrl, operationsMetadata);
            if (isTransactional) {
                OperationMetadata_Transaction(owsf, baseUrl, operationsMetadata);
            }
            
            // - Parameters
            DomainType opMetadataParam1 = owsf.createDomainType();
            DomainType opMetadataParam2 = owsf.createDomainType();
            
            opMetadataParam1.setName("service");
            opMetadataParam1.getValue().add("http://www.opengis.net/cat/csw/2.0.2");

            opMetadataParam2.setName("version");
            opMetadataParam2.getValue().add(version);
            
            operationsMetadata.getParameter().add(opMetadataParam1);
            operationsMetadata.getParameter().add(opMetadataParam2);

            // - Constraints
            DomainType opMetadataConstraint1 = owsf.createDomainType();

            opMetadataConstraint1.setName("PostEncoding");
            opMetadataConstraint1.getValue().add("XML");

            operationsMetadata.getConstraint().add(opMetadataConstraint1);
        }

        // Filter Capabilities
        if (sections == null || requestedSection("OperationsMetadata", sections)) {
            final FilterFactory ffFactory = CommonFactoryFinder.getFilterFactory(null);
            // - Spatial Capabilities
            GeometryOperand[] geometryOperands = new GeometryOperand[] { GeometryOperand.Envelope,
                    GeometryOperand.Point, GeometryOperand.LineString, GeometryOperand.Polygon };
            SpatialOperator[] spatialOperators = new SpatialOperator[] {
                    ffFactory.spatialOperator("BBOX", new GeometryOperand[] {}),
                    ffFactory.spatialOperator("Equals", new GeometryOperand[] {}),
                    ffFactory.spatialOperator("Overlaps", new GeometryOperand[] {}),
                    ffFactory.spatialOperator("Disjoint", new GeometryOperand[] {}),
                    ffFactory.spatialOperator("Intersects", new GeometryOperand[] {}),
                    ffFactory.spatialOperator("Touches", new GeometryOperand[] {}),
                    ffFactory.spatialOperator("Crosses", new GeometryOperand[] {}),
                    ffFactory.spatialOperator("Within", new GeometryOperand[] {}),
                    ffFactory.spatialOperator("Contains", new GeometryOperand[] {}),
                    ffFactory.spatialOperator("Beyond", new GeometryOperand[] {}),
                    ffFactory.spatialOperator("DWithin", new GeometryOperand[] {}) };
            SpatialOperators spatialOperands = ffFactory.spatialOperators(spatialOperators);
            SpatialCapabilities spatialCapabilities = ffFactory.spatialCapabilities(
                    geometryOperands, spatialOperands);

            // - Scalar Capabilities
            Operator[] operators = new Operator[] { ffFactory.operator("EqualTo"),
                    ffFactory.operator("Like"), ffFactory.operator("LessThan"),
                    ffFactory.operator("GreaterThan"), ffFactory.operator("LessThanEqualTo"),
                    ffFactory.operator("GreaterThanEqualTo"), ffFactory.operator("NotEqualTo"),
                    ffFactory.operator("Between"), ffFactory.operator("NullCheck") };
            ComparisonOperators comparisonOperators = ffFactory.comparisonOperators(operators);
            ArithmeticOperators arithmeticOperators = null;
            ScalarCapabilities scalarCapabilities = ffFactory.scalarCapabilities(
                    comparisonOperators, arithmeticOperators, logicalOperators);

            // - Id Capabilities
            IdCapabilities id = ffFactory.idCapabilities(eid, fid);

            FilterCapabilities filterCapabilities = ffFactory.capabilities(version,
                    scalarCapabilities, spatialCapabilities, id);
            caps.setFilterCapabilities(filterCapabilities);
        }

        return caps;
    }

    private static boolean requestedSection(String sectionName, SectionsType sections) {
        boolean sectionIsRequested = false;

        for (Object section : sections.getSection()) {
            if (((String) section).equalsIgnoreCase(sectionName)) {
                sectionIsRequested = true;
                break;
            }
        }
        return sectionIsRequested;
    }

    /**
     * @param owsf
     * @param baseUrl
     * @param operationsMetadata
     */
    private void OperationMetadata_GetCapabilities(Ows10Factory owsf, final String baseUrl,
            OperationsMetadataType operationsMetadata) {
        // - GetCapabilities
        OperationType getCapabilities = owsf.createOperationType();
        operationsMetadata.getOperation().add(getCapabilities);

        getCapabilities.setName("GetCapabilities");
        DCPType getCapabilitiesDCP = owsf.createDCPType();
        getCapabilities.getDCP().add(getCapabilitiesDCP);

        HTTPType getCapabilitiesHTTP = owsf.createHTTPType();
        getCapabilitiesDCP.setHTTP(getCapabilitiesHTTP);

        String getCapabilitiesHref = ResponseUtils.buildURL(baseUrl, "csw",
                new HashMap<String, String>(), URLType.SERVICE);

        RequestMethodType getCapabilitiesGet = owsf.createRequestMethodType();
        getCapabilitiesGet.setHref(getCapabilitiesHref);
        getCapabilitiesHTTP.getGet().add(getCapabilitiesGet);

        RequestMethodType getCapabilitiesPost = owsf.createRequestMethodType();
        getCapabilitiesPost.setHref(getCapabilitiesHref);
        getCapabilitiesHTTP.getPost().add(getCapabilitiesPost);

        // - GetCapabilities - parameters
        DomainType getCapabilitiesParams = owsf.createDomainType();
        getCapabilities.getParameter().add(getCapabilitiesParams);

        getCapabilitiesParams.setName("sections");
        getCapabilitiesParams.getValue().add("ServiceIdentification");
        getCapabilitiesParams.getValue().add("ServiceProvider");
        getCapabilitiesParams.getValue().add("OperationsMetadata");
        getCapabilitiesParams.getValue().add("Filter_Capabilities");

        // - GetCapabilities - constraints
        DomainType getCapabilitiesConstraints = owsf.createDomainType();
        getCapabilities.getConstraint().add(getCapabilitiesConstraints);

        getCapabilitiesConstraints.setName("PostEncoding");
        getCapabilitiesConstraints.getValue().add("XML");
    }

    /**
     * 
     * @param owsf
     * @param baseUrl
     * @param operationsMetadata
     */
    private void OperationMetadata_DescribeRecord(Ows10Factory owsf, String baseUrl,
            OperationsMetadataType operationsMetadata) {
        // - DescribeRecord
        OperationType describeRecord = owsf.createOperationType();
        operationsMetadata.getOperation().add(describeRecord);

        describeRecord.setName("DescribeRecord");
        DCPType describeRecordDCP = owsf.createDCPType();
        describeRecord.getDCP().add(describeRecordDCP);

        HTTPType describeRecordHTTP = owsf.createHTTPType();
        describeRecordDCP.setHTTP(describeRecordHTTP);

        String describeRecordHref = ResponseUtils.buildURL(baseUrl, "csw",
                new HashMap<String, String>(), URLType.SERVICE);

        RequestMethodType describeRecordGet = owsf.createRequestMethodType();
        describeRecordGet.setHref(describeRecordHref);
        describeRecordHTTP.getGet().add(describeRecordGet);

        RequestMethodType describeRecordPost = owsf.createRequestMethodType();
        describeRecordPost.setHref(describeRecordHref);

        // - DescribeRecord - constraints
        DomainType describeRecordPostConstraints = owsf.createDomainType();
        describeRecordPostConstraints.setName("PostEncoding");
        describeRecordPostConstraints.getValue().add("XML");

        describeRecordPost.getConstraint().add(describeRecordPostConstraints);
        describeRecordHTTP.getPost().add(describeRecordPost);

        // - DescribeRecord - parameters
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

        describeRecord.getParameter().add(describeRecordParam1);
        describeRecord.getParameter().add(describeRecordParam2);
        describeRecord.getParameter().add(describeRecordParam3);

        // - DescribeRecord - constraints
        DomainType describeRecordConstraints = owsf.createDomainType();
        describeRecord.getConstraint().add(describeRecordConstraints);

        describeRecordConstraints.setName("PostEncoding");
        describeRecordConstraints.getValue().add("XML");
    }

    /**
     * 
     * @param owsf
     * @param baseUrl
     * @param operationsMetadata
     */
    private void OperationMetadata_GetRecords(Ows10Factory owsf, String baseUrl,
            OperationsMetadataType operationsMetadata) {
        // - GetRecords
        OperationType getRecords = owsf.createOperationType();
        operationsMetadata.getOperation().add(getRecords);

        getRecords.setName("GetRecords");
        DCPType getRecordsDCP = owsf.createDCPType();
        getRecords.getDCP().add(getRecordsDCP);

        HTTPType getRecordsHTTP = owsf.createHTTPType();
        getRecordsDCP.setHTTP(getRecordsHTTP);

        String getRecordsHref = ResponseUtils.buildURL(baseUrl, "csw",
                new HashMap<String, String>(), URLType.SERVICE);

        RequestMethodType getRecordsGet = owsf.createRequestMethodType();
        getRecordsGet.setHref(getRecordsHref);
        getRecordsHTTP.getGet().add(getRecordsGet);

        RequestMethodType getRecordsPost = owsf.createRequestMethodType();
        getRecordsPost.setHref(getRecordsHref);

        // - GetRecords - constraints
        DomainType getRecordsPostConstraints = owsf.createDomainType();
        getRecordsPostConstraints.setName("PostEncoding");
        getRecordsPostConstraints.getValue().add("XML");

        getRecordsPost.getConstraint().add(getRecordsPostConstraints);
        getRecordsHTTP.getPost().add(getRecordsPost);

        // - GetRecords - parameters
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

        getRecords.getParameter().add(getRecordsParam1);
        getRecords.getParameter().add(getRecordsParam2);
        getRecords.getParameter().add(getRecordsParam3);
        getRecords.getParameter().add(getRecordsParam4);
        getRecords.getParameter().add(getRecordsParam5);

        // - GetRecords - constraints
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

        getRecords.getConstraint().add(getRecordConstraint1);
        getRecords.getConstraint().add(getRecordConstraint2);
    }

    /**
     * 
     * @param owsf
     * @param baseUrl
     * @param operationsMetadata
     */
    private void OperationMetadata_GetRecordById(Ows10Factory owsf, String baseUrl,
            OperationsMetadataType operationsMetadata) {
        // - GetRecordById
        OperationType getRecordById = owsf.createOperationType();
        operationsMetadata.getOperation().add(getRecordById);

        getRecordById.setName("GetRecordById");
        DCPType getRecordByIdDCP = owsf.createDCPType();
        getRecordById.getDCP().add(getRecordByIdDCP);

        HTTPType getRecordByIdHTTP = owsf.createHTTPType();
        getRecordByIdDCP.setHTTP(getRecordByIdHTTP);

        String getRecordByIdHref = ResponseUtils.buildURL(baseUrl, "csw",
                new HashMap<String, String>(), URLType.SERVICE);

        RequestMethodType getRecordByIdGet = owsf.createRequestMethodType();
        getRecordByIdGet.setHref(getRecordByIdHref);
        getRecordByIdHTTP.getGet().add(getRecordByIdGet);

        RequestMethodType getRecordByIdPost = owsf.createRequestMethodType();
        getRecordByIdPost.setHref(getRecordByIdHref);

        // - GetRecordById - constraints
        DomainType getRecordByIdPostConstraints = owsf.createDomainType();
        getRecordByIdPostConstraints.setName("PostEncoding");
        getRecordByIdPostConstraints.getValue().add("XML");

        getRecordByIdPost.getConstraint().add(getRecordByIdPostConstraints);
        getRecordByIdHTTP.getPost().add(getRecordByIdPost);
        
        // - GetRecordById - parameters
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

        getRecordById.getParameter().add(getRecordByIdParam1);
        getRecordById.getParameter().add(getRecordByIdParam2);
        getRecordById.getParameter().add(getRecordByIdParam3);
        getRecordById.getParameter().add(getRecordByIdParam4);

        // - GetRecordById - constraints
        DomainType getRecordConstraint1 = owsf.createDomainType();

        getRecordConstraint1.setName("PostEncoding");
        getRecordConstraint1.getValue().add("XML");

        getRecordById.getConstraint().add(getRecordConstraint1);
    }

    /**
     * 
     * @param owsf
     * @param baseUrl
     * @param operationsMetadata
     */
    private void OperationMetadata_GetDomain(Ows10Factory owsf, String baseUrl,
            OperationsMetadataType operationsMetadata) {
        // - GetDomain
        OperationType getDomain = owsf.createOperationType();
        operationsMetadata.getOperation().add(getDomain);

        getDomain.setName("GetDomain");
        DCPType getDomainDCP = owsf.createDCPType();
        getDomain.getDCP().add(getDomainDCP);

        HTTPType getDomainHTTP = owsf.createHTTPType();
        getDomainDCP.setHTTP(getDomainHTTP);

        String getDomainHref = ResponseUtils.buildURL(baseUrl, "csw",
                new HashMap<String, String>(), URLType.SERVICE);

        RequestMethodType getDomainGet = owsf.createRequestMethodType();
        getDomainGet.setHref(getDomainHref);
        getDomainHTTP.getGet().add(getDomainGet);

        RequestMethodType getDomainPost = owsf.createRequestMethodType();
        getDomainPost.setHref(getDomainHref);

        // - GetDomain - constraints
        DomainType getDomainPostConstraints = owsf.createDomainType();
        getDomainPostConstraints.setName("PostEncoding");
        getDomainPostConstraints.getValue().add("XML");

        getDomainPost.getConstraint().add(getDomainPostConstraints);
        getDomainHTTP.getPost().add(getDomainPost);
    }

    /**
     * 
     * @param owsf
     * @param baseUrl
     * @param operationsMetadata
     */
    private void OperationMetadata_Transaction(Ows10Factory owsf, String baseUrl,
            OperationsMetadataType operationsMetadata) {
        // - Transaction
        OperationType transaction = owsf.createOperationType();
        operationsMetadata.getOperation().add(transaction);

        transaction.setName("Transaction");
        DCPType transactionDCP = owsf.createDCPType();
        transaction.getDCP().add(transactionDCP);

        HTTPType transactionHTTP = owsf.createHTTPType();
        transactionDCP.setHTTP(transactionHTTP);

        String transactionHref = ResponseUtils.buildURL(baseUrl, "csw",
                new HashMap<String, String>(), URLType.SERVICE);

        RequestMethodType transactionGet = owsf.createRequestMethodType();
        transactionGet.setHref(transactionHref);
        transactionHTTP.getGet().add(transactionGet);

        RequestMethodType transactionPost = owsf.createRequestMethodType();
        transactionPost.setHref(transactionHref);

        // - Transaction - constraints
        DomainType transactionPostConstraints = owsf.createDomainType();
        transactionPostConstraints.setName("PostEncoding");
        transactionPostConstraints.getValue().add("XML");

        transactionPost.getConstraint().add(transactionPostConstraints);
        transactionHTTP.getPost().add(transactionPost);
    }

}
