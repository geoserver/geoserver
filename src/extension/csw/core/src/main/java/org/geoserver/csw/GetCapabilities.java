/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.opengis.cat.csw20.CapabilitiesType;
import net.opengis.cat.csw20.Csw20Factory;
import net.opengis.cat.csw20.GetCapabilitiesType;
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
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.geoserver.catalog.KeywordInfo;
import org.geoserver.config.ContactInfo;
import org.geoserver.csw.records.RecordDescriptor;
import org.geoserver.csw.store.CatalogStore;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.RequestUtils;
import org.geoserver.ows.util.ResponseUtils;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.capability.FilterCapabilitiesImpl;
import org.geotools.filter.capability.ScalarCapabilitiesImpl;
import org.geotools.filter.capability.SpatialCapabiltiesImpl;
import org.geotools.filter.capability.SpatialOperatorsImpl;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.logging.Logging;
import org.geotools.xsd.EMFUtils;
import org.opengis.feature.type.Name;
import org.opengis.filter.FilterFactory2;
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
import org.xml.sax.helpers.NamespaceSupport;

/**
 * The CSW GetCapabilities implementation
 *
 * @author Alessio Fabiani - GeoSolutions
 */
public class GetCapabilities {
    private static final String CSW_VERSION = "2.0.2";
    private static final Logger LOGGER = Logging.getLogger(GetCapabilities.class);
    private static Csw20Factory cswf = Csw20Factory.eINSTANCE;
    private static Ows10Factory owsf = Ows10Factory.eINSTANCE;

    private static final boolean logicalOperators = true;

    private static final boolean eid = true;

    private static final boolean fid = true;

    public CSWInfo csw;

    public CatalogStore store;

    ApplicationContext context;

    public GetCapabilities(CSWInfo csw, CatalogStore store, ApplicationContext context) {
        this.csw = csw;
        this.store = store;
        this.context = context;
    }

    public CapabilitiesType run(GetCapabilitiesType request) throws CSWException {
        // do the version negotiation dance
        SectionsType sections = null;
        List<String> provided = Collections.singletonList(CSW_VERSION);
        List<String> accepted = null;
        if (request.getAcceptVersions() != null)
            accepted = request.getAcceptVersions().getVersion();
        String version = RequestUtils.getVersionOws11(provided, accepted);

        if (!CSW_VERSION.equals(version)) {
            throw new CSWException("Could not understand version:" + version);
        }

        if (request.getSections() != null) {
            sections = request.getSections();
        }

        // encode the response
        CapabilitiesType caps = cswf.createCapabilitiesType();
        caps.setVersion(CSW_VERSION);

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
            si.setServiceTypeVersion(CSW_VERSION);
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

            sp.setProviderName(
                    (contact.getContactOrganization() != null
                            ? contact.getContactOrganization()
                            : ""));

            OnlineResourceType providerSite = owsf.createOnlineResourceType();
            sp.setProviderSite(providerSite);
            providerSite.setHref((csw.getOnlineResource() != null ? csw.getOnlineResource() : ""));

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
                throw new IllegalArgumentException(
                        "Request object" + request + " has no 'baseUrl' property.");
            }

            OperationsMetadataType operationsMetadata = owsf.createOperationsMetadataType();
            caps.setOperationsMetadata(operationsMetadata);

            OperationMetadata_GetCapabilities(owsf, baseUrl, operationsMetadata);
            OperationMetadata_DescribeRecord(owsf, baseUrl, operationsMetadata);
            OperationMetadata_GetDomain(owsf, baseUrl, operationsMetadata);
            OperationMetadata_GetRecords(owsf, baseUrl, operationsMetadata);
            OperationMetadata_GetRecordById(owsf, baseUrl, operationsMetadata);
            if (store.getCapabilities().supportsTransactions()) {
                OperationMetadata_Transaction(owsf, baseUrl, operationsMetadata);
            }

            // - Parameters
            for (DomainType param :
                    store.getCapabilities().getOperationParameters().get("OperationsMetadata")) {
                // clone the object, as the caps decorators might want to modify it
                operationsMetadata.getParameter().add(EcoreUtil.copy(param));
            }

            // - Constraints
            for (DomainType constraint :
                    store.getCapabilities().getOperationConstraints().get("OperationsMetadata")) {
                // clone the object, as the caps decorators might want to modify it
                operationsMetadata.getConstraint().add(EcoreUtil.copy(constraint));
            }
        }

        // Filter Capabilities
        // this part is not optional, the schema has min = 0, so we don't check for the sections
        final FilterFactory2 ffFactory =
                CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
        // - Spatial Capabilities
        // SpatialCapabilities spatialCapabilities = ffFactory.spatialCapabilities(geometryOperands,
        // spatialOperands);
        SpatialCapabilities spatialCapabilities = new CSWSpatialCapabilities();

        // - Scalar Capabilities
        Operator[] operators =
                new Operator[] {
                    ffFactory.operator("EqualTo"),
                    ffFactory.operator("Like"),
                    ffFactory.operator("LessThan"),
                    ffFactory.operator("GreaterThan"),
                    ffFactory.operator("LessThanEqualTo"),
                    ffFactory.operator("GreaterThanEqualTo"),
                    ffFactory.operator("NotEqualTo"),
                    ffFactory.operator("Between"),
                    ffFactory.operator("NullCheck")
                };
        ComparisonOperators comparisonOperators = ffFactory.comparisonOperators(operators);
        ArithmeticOperators arithmeticOperators = ffFactory.arithmeticOperators(true, null);
        ScalarCapabilities scalarCapabilities =
                ffFactory.scalarCapabilities(
                        comparisonOperators, arithmeticOperators, logicalOperators);
        // - removing Arithmetic Operators...
        ((ScalarCapabilitiesImpl) scalarCapabilities).setArithmeticOperators(null);

        // - Id Capabilities
        IdCapabilities id = ffFactory.idCapabilities(eid, fid);

        FilterCapabilities filterCapabilities =
                ffFactory.capabilities("1.1.0", scalarCapabilities, spatialCapabilities, id);

        ((FilterCapabilitiesImpl) filterCapabilities).setScalar(scalarCapabilities);
        ((FilterCapabilitiesImpl) filterCapabilities).setSpatial(spatialCapabilities);
        ((FilterCapabilitiesImpl) filterCapabilities).setId(id);

        caps.setFilterCapabilities(filterCapabilities);

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
    private void OperationMetadata_GetCapabilities(
            Ows10Factory owsf, final String baseUrl, OperationsMetadataType operationsMetadata) {
        // - GetCapabilities
        OperationType getCapabilities = owsf.createOperationType();
        operationsMetadata.getOperation().add(getCapabilities);

        getCapabilities.setName("GetCapabilities");
        DCPType getCapabilitiesDCP = owsf.createDCPType();
        getCapabilities.getDCP().add(getCapabilitiesDCP);

        HTTPType getCapabilitiesHTTP = owsf.createHTTPType();
        getCapabilitiesDCP.setHTTP(getCapabilitiesHTTP);

        String getCapabilitiesHref =
                ResponseUtils.buildURL(
                        baseUrl, "csw", new HashMap<String, String>(), URLType.SERVICE);

        RequestMethodType getCapabilitiesGet = owsf.createRequestMethodType();
        getCapabilitiesGet.setHref(getCapabilitiesHref);
        getCapabilitiesHTTP.getGet().add(getCapabilitiesGet);

        RequestMethodType getCapabilitiesPost = owsf.createRequestMethodType();
        getCapabilitiesPost.setHref(getCapabilitiesHref);
        getCapabilitiesHTTP.getPost().add(getCapabilitiesPost);

        // - Parameters
        for (DomainType param :
                store.getCapabilities().getOperationParameters().get("GetCapabilities")) {
            // clone the object, as the caps decorators might want to modify it
            getCapabilities.getParameter().add(EcoreUtil.copy(param));
        }

        // - Constraints
        for (DomainType constraint :
                store.getCapabilities().getOperationConstraints().get("GetCapabilities")) {
            // clone the object, as the caps decorators might want to modify it
            getCapabilities.getConstraint().add(EcoreUtil.copy(constraint));
        }
    }

    /**
     * @param owsf
     * @param baseUrl
     * @param operationsMetadata
     */
    private void OperationMetadata_DescribeRecord(
            Ows10Factory owsf, String baseUrl, OperationsMetadataType operationsMetadata) {
        // - DescribeRecord
        OperationType describeRecord = owsf.createOperationType();
        operationsMetadata.getOperation().add(describeRecord);

        describeRecord.setName("DescribeRecord");
        DCPType describeRecordDCP = owsf.createDCPType();
        describeRecord.getDCP().add(describeRecordDCP);

        HTTPType describeRecordHTTP = owsf.createHTTPType();
        describeRecordDCP.setHTTP(describeRecordHTTP);

        String describeRecordHref =
                ResponseUtils.buildURL(
                        baseUrl, "csw", new HashMap<String, String>(), URLType.SERVICE);

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

        // - Parameters
        for (DomainType param :
                store.getCapabilities().getOperationParameters().get("DescribeRecord")) {
            // clone the object, as the caps decorators might want to modify it
            describeRecord.getParameter().add(EcoreUtil.copy(param));
        }

        // - Constraints
        for (DomainType constraint :
                store.getCapabilities().getOperationConstraints().get("DescribeRecord")) {
            // clone the object, as the caps decorators might want to modify it
            describeRecord.getConstraint().add(EcoreUtil.copy(constraint));
        }
    }

    /**
     * @param owsf
     * @param baseUrl
     * @param operationsMetadata
     */
    private void OperationMetadata_GetRecords(
            Ows10Factory owsf, String baseUrl, OperationsMetadataType operationsMetadata) {
        // - GetRecords
        OperationType getRecords = owsf.createOperationType();
        operationsMetadata.getOperation().add(getRecords);

        getRecords.setName("GetRecords");
        DCPType getRecordsDCP = owsf.createDCPType();
        getRecords.getDCP().add(getRecordsDCP);

        HTTPType getRecordsHTTP = owsf.createHTTPType();
        getRecordsDCP.setHTTP(getRecordsHTTP);

        String getRecordsHref =
                ResponseUtils.buildURL(
                        baseUrl, "csw", new HashMap<String, String>(), URLType.SERVICE);

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

        // - Parameters
        for (DomainType param :
                store.getCapabilities().getOperationParameters().get("GetRecords")) {
            // clone the object, as the caps decorators might want to modify it
            getRecords.getParameter().add(EcoreUtil.copy(param));
        }

        // - Constraints
        for (DomainType constraint :
                store.getCapabilities().getOperationConstraints().get("GetRecords")) {
            // clone the object, as the caps decorators might want to modify it
            getRecords.getConstraint().add(EcoreUtil.copy(constraint));
        }

        // the additional queriables based on the store
        try {
            for (RecordDescriptor rd : store.getRecordDescriptors()) {
                List<Name> queriables =
                        store.getCapabilities().getQueriables(rd.getFeatureDescriptor().getName());
                if (queriables != null && queriables.size() > 0) {
                    DomainType dt = owsf.createDomainType();
                    dt.setName(rd.getQueryablesDescription());
                    NamespaceSupport nss = rd.getNamespaceSupport();

                    for (Name q : queriables) {
                        String prefix = nss.getPrefix(q.getNamespaceURI());
                        dt.getValue()
                                .add(
                                        prefix == null
                                                ? q.getLocalPart()
                                                : prefix + ":" + q.getLocalPart());
                    }
                    getRecords.getConstraint().add(dt);
                }
            }

            DomainType dt = owsf.createDomainType();
            dt.setName("XPathQueryables");
            dt.getValue().add("allowed");
            getRecords.getConstraint().add(dt);

        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to encode getRecords additional queriables", e);
        }
    }

    /**
     * @param owsf
     * @param baseUrl
     * @param operationsMetadata
     */
    private void OperationMetadata_GetRecordById(
            Ows10Factory owsf, String baseUrl, OperationsMetadataType operationsMetadata) {
        // - GetRecordById
        OperationType getRecordById = owsf.createOperationType();
        operationsMetadata.getOperation().add(getRecordById);

        getRecordById.setName("GetRecordById");
        DCPType getRecordByIdDCP = owsf.createDCPType();
        getRecordById.getDCP().add(getRecordByIdDCP);

        HTTPType getRecordByIdHTTP = owsf.createHTTPType();
        getRecordByIdDCP.setHTTP(getRecordByIdHTTP);

        String getRecordByIdHref =
                ResponseUtils.buildURL(
                        baseUrl, "csw", new HashMap<String, String>(), URLType.SERVICE);

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

        // - Parameters
        for (DomainType param :
                store.getCapabilities().getOperationParameters().get("GetRecordById")) {
            // clone the object, as the caps decorators might want to modify it
            getRecordById.getParameter().add(EcoreUtil.copy(param));
        }

        // - Constraints
        for (DomainType constraint :
                store.getCapabilities().getOperationConstraints().get("GetRecordById")) {
            // clone the object, as the caps decorators might want to modify it
            getRecordById.getConstraint().add(EcoreUtil.copy(constraint));
        }
    }

    /**
     * @param owsf
     * @param baseUrl
     * @param operationsMetadata
     */
    private void OperationMetadata_GetDomain(
            Ows10Factory owsf, String baseUrl, OperationsMetadataType operationsMetadata) {
        // - GetDomain
        OperationType getDomain = owsf.createOperationType();
        operationsMetadata.getOperation().add(getDomain);

        getDomain.setName("GetDomain");
        DCPType getDomainDCP = owsf.createDCPType();
        getDomain.getDCP().add(getDomainDCP);

        HTTPType getDomainHTTP = owsf.createHTTPType();
        getDomainDCP.setHTTP(getDomainHTTP);

        String getDomainHref =
                ResponseUtils.buildURL(
                        baseUrl, "csw", new HashMap<String, String>(), URLType.SERVICE);

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

        // - Fixed Parameters
        for (DomainType param : store.getCapabilities().getOperationParameters().get("GetDomain")) {
            // clone the object, as the caps decorators might want to modify it
            getDomain.getParameter().add(EcoreUtil.copy(param));
        }

        // The domain queriables list from the catalog store
        try {
            Set<String> summary = new HashSet<String>();
            for (RecordDescriptor rd : store.getRecordDescriptors()) {
                List<Name> queriables =
                        store.getCapabilities()
                                .getDomainQueriables(rd.getFeatureDescriptor().getName());

                if (queriables != null && queriables.size() > 0) {
                    NamespaceSupport nss = rd.getNamespaceSupport();
                    for (Name q : queriables) {
                        String prefix = nss.getPrefix(q.getNamespaceURI());
                        summary.add(
                                prefix == null
                                        ? q.getLocalPart()
                                        : prefix + ":" + q.getLocalPart());
                    }
                }
            }

            if (summary.size() > 0) {
                List<String> sorted = new ArrayList<String>(summary);
                Collections.sort(sorted);
                DomainType dt = owsf.createDomainType();
                dt.setName("PropertyName");
                for (String name : sorted) {
                    dt.getValue().add(name);
                }
                getDomain.getParameter().add(dt);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to encode getDomain ParameterName values", e);
        }

        // - Constraints
        for (DomainType constraint :
                store.getCapabilities().getOperationConstraints().get("GetDomain")) {
            getDomain.getConstraint().add(EcoreUtil.copy(constraint));
        }
    }

    /**
     * @param owsf
     * @param baseUrl
     * @param operationsMetadata
     */
    private void OperationMetadata_Transaction(
            Ows10Factory owsf, String baseUrl, OperationsMetadataType operationsMetadata) {
        // - Transaction
        OperationType transaction = owsf.createOperationType();
        operationsMetadata.getOperation().add(transaction);

        transaction.setName("Transaction");
        DCPType transactionDCP = owsf.createDCPType();
        transaction.getDCP().add(transactionDCP);

        HTTPType transactionHTTP = owsf.createHTTPType();
        transactionDCP.setHTTP(transactionHTTP);

        String transactionHref =
                ResponseUtils.buildURL(
                        baseUrl, "csw", new HashMap<String, String>(), URLType.SERVICE);

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

        // - Parameters
        for (DomainType param :
                store.getCapabilities().getOperationParameters().get("Transaction")) {
            // clone the object, as the caps decorators might want to modify it
            transaction.getParameter().add(EcoreUtil.copy(param));
        }

        // - Constraints
        for (DomainType constraint :
                store.getCapabilities().getOperationConstraints().get("Transaction")) {
            // clone the object, as the caps decorators might want to modify it
            transaction.getConstraint().add(EcoreUtil.copy(constraint));
        }
    }
}

class CSWSpatialCapabilities extends SpatialCapabiltiesImpl {
    static final SpatialOperator[] spatialOperators =
            new SpatialOperator[] {
                spatialOperator("BBOX"),
                spatialOperator("Equals"),
                spatialOperator("Overlaps"),
                spatialOperator("Disjoint"),
                spatialOperator("Intersects"),
                spatialOperator("Touches"),
                spatialOperator("Crosses"),
                spatialOperator("Within"),
                spatialOperator("Contains"),
                spatialOperator("Beyond"),
                spatialOperator("DWithin")
            };

    static final GeometryOperand[] geometryOpertors =
            new GeometryOperand[] {
                GeometryOperand.get("http://www.opengis.net/gml", "Envelope"),
                GeometryOperand.get("http://www.opengis.net/gml", "Point"),
                GeometryOperand.get("http://www.opengis.net/gml", "LineString"),
                GeometryOperand.get("http://www.opengis.net/gml", "Polygon")
            };

    volatile SpatialOperators spatialOperands = new SpatialOperatorsImpl();

    List<GeometryOperand> geometryOperands = new LinkedList<GeometryOperand>();

    @Override
    public Collection<GeometryOperand> getGeometryOperands() {
        synchronized (geometryOperands) {
            if (geometryOperands == null || geometryOperands.isEmpty()) {
                // - sorting Geometry Operands
                for (GeometryOperand operator : geometryOpertors) {
                    geometryOperands.add(operator);
                }
                Collections.sort(
                        geometryOperands,
                        new Comparator<GeometryOperand>() {

                            @Override
                            public int compare(GeometryOperand o1, GeometryOperand o2) {
                                if (o2.getLocalPart().contains("Envelope")) {
                                    return -1;
                                }

                                if (o2.getLocalPart().contains("Point")) {
                                    if (o1.getLocalPart().contains("Envelope")) {
                                        return -1;
                                    } else {
                                        return 1;
                                    }
                                }

                                if (o2.getLocalPart().contains("LineString")) {
                                    if (o1.getLocalPart().contains("Point")) {
                                        return -1;
                                    } else {
                                        return 1;
                                    }
                                }

                                if (o2.getLocalPart().contains("Polygon")) {
                                    if (o1.getLocalPart().contains("LineString")) {
                                        return -1;
                                    } else {
                                        return 1;
                                    }
                                }

                                return 0;
                            }
                        });
            }
        }

        return geometryOperands;
    }

    @Override
    public SpatialOperatorsImpl getSpatialOperators() {
        if (spatialOperands == null
                || spatialOperands.getOperators() == null
                || spatialOperands.getOperators().size() == 0) {
            synchronized (this) {
                if (spatialOperands == null
                        || spatialOperands.getOperators() == null
                        || spatialOperands.getOperators().size() == 0) {
                    spatialOperands = new SpatialOperatorsImpl();

                    for (SpatialOperator operator : spatialOperators) {
                        if ((spatialOperands).getOperators() == null) {
                            ((SpatialOperatorsImpl) spatialOperands).setOperators(new HashSet<>());
                        }
                        (spatialOperands).getOperators().add(operator);
                    }
                }
            }
        }

        return (SpatialOperatorsImpl) spatialOperands;
    }

    private static SpatialOperator spatialOperator(final String name) {
        return new SpatialOperator() {

            @Override
            public String getName() {
                return name;
            }

            @Override
            public Collection<GeometryOperand> getGeometryOperands() {
                return null;
            }
        };
    }
}
