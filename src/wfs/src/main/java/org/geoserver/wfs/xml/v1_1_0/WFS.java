/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml.v1_1_0;

import java.util.Set;
import javax.xml.namespace.QName;
import org.geoserver.ows.xml.v1_0.OWS;
import org.geoserver.wfs.xml.FeatureTypeSchemaBuilder;
import org.geotools.filter.v1_1.OGC;
import org.geotools.gml3.GML;
import org.geotools.xsd.XSD;

/**
 * XSD object for GeoServer WFS 1.1.
 *
 * <p>This object is not a singleton in the conventional java sense as the other XSD subclasses
 * (GML,OGC,OWS,etc..) are. It is a singleton, but managed as such by the spring container. The
 * reason being that it requires the catalog to operate and build the underlying schema.
 */
public class WFS extends XSD {

    /** @generated */
    public static final String NAMESPACE = "http://www.opengis.net/wfs";

    public static final String CANONICAL_SCHEMA_LOCATION =
            "http://schemas.opengis.net/wfs/1.1.0/wfs.xsd";

    /* Type Definitions */
    /** @generated */
    public static final QName ACTIONTYPE = new QName("http://www.opengis.net/wfs", "ActionType");

    /** @generated */
    public static final QName ALLSOMETYPE = new QName("http://www.opengis.net/wfs", "AllSomeType");

    /** @generated */
    public static final QName BASE_TYPENAMELISTTYPE =
            new QName("http://www.opengis.net/wfs", "Base_TypeNameListType");

    /** @generated */
    public static final QName BASEREQUESTTYPE =
            new QName("http://www.opengis.net/wfs", "BaseRequestType");

    /** @generated */
    public static final QName DELETEELEMENTTYPE =
            new QName("http://www.opengis.net/wfs", "DeleteElementType");

    /** @generated */
    public static final QName DESCRIBEFEATURETYPETYPE =
            new QName("http://www.opengis.net/wfs", "DescribeFeatureTypeType");

    /** @generated */
    public static final QName FEATURECOLLECTIONTYPE =
            new QName("http://www.opengis.net/wfs", "FeatureCollectionType");

    /** @generated */
    public static final QName FEATURESLOCKEDTYPE =
            new QName("http://www.opengis.net/wfs", "FeaturesLockedType");

    /** @generated */
    public static final QName FEATURESNOTLOCKEDTYPE =
            new QName("http://www.opengis.net/wfs", "FeaturesNotLockedType");

    /** @generated */
    public static final QName FEATURETYPELISTTYPE =
            new QName("http://www.opengis.net/wfs", "FeatureTypeListType");

    /** @generated */
    public static final QName FEATURETYPETYPE =
            new QName("http://www.opengis.net/wfs", "FeatureTypeType");

    /** @generated */
    public static final QName GETCAPABILITIESTYPE =
            new QName("http://www.opengis.net/wfs", "GetCapabilitiesType");

    /** @generated */
    public static final QName GETFEATURETYPE =
            new QName("http://www.opengis.net/wfs", "GetFeatureType");

    /** @generated */
    public static final QName GETFEATUREWITHLOCKTYPE =
            new QName("http://www.opengis.net/wfs", "GetFeatureWithLockType");

    /** @generated */
    public static final QName GETGMLOBJECTTYPE =
            new QName("http://www.opengis.net/wfs", "GetGmlObjectType");

    /** @generated */
    public static final QName GMLOBJECTTYPELISTTYPE =
            new QName("http://www.opengis.net/wfs", "GMLObjectTypeListType");

    /** @generated */
    public static final QName GMLOBJECTTYPETYPE =
            new QName("http://www.opengis.net/wfs", "GMLObjectTypeType");

    /** @generated */
    public static final QName IDENTIFIERGENERATIONOPTIONTYPE =
            new QName("http://www.opengis.net/wfs", "IdentifierGenerationOptionType");

    /** @generated */
    public static final QName INSERTEDFEATURETYPE =
            new QName("http://www.opengis.net/wfs", "InsertedFeatureType");

    /** @generated */
    public static final QName INSERTELEMENTTYPE =
            new QName("http://www.opengis.net/wfs", "InsertElementType");

    /** @generated */
    public static final QName INSERTRESULTSTYPE =
            new QName("http://www.opengis.net/wfs", "InsertResultsType");

    /** @generated */
    public static final QName LOCKFEATURERESPONSETYPE =
            new QName("http://www.opengis.net/wfs", "LockFeatureResponseType");

    /** @generated */
    public static final QName LOCKFEATURETYPE =
            new QName("http://www.opengis.net/wfs", "LockFeatureType");

    /** @generated */
    public static final QName LOCKTYPE = new QName("http://www.opengis.net/wfs", "LockType");

    /** @generated */
    public static final QName METADATAURLTYPE =
            new QName("http://www.opengis.net/wfs", "MetadataURLType");

    /** @generated */
    public static final QName NATIVETYPE = new QName("http://www.opengis.net/wfs", "NativeType");

    /** @generated */
    public static final QName OPERATIONSTYPE =
            new QName("http://www.opengis.net/wfs", "OperationsType");

    /** @generated */
    public static final QName OPERATIONTYPE =
            new QName("http://www.opengis.net/wfs", "OperationType");

    /** @generated */
    public static final QName OUTPUTFORMATLISTTYPE =
            new QName("http://www.opengis.net/wfs", "OutputFormatListType");

    /** @generated */
    public static final QName PROPERTYTYPE =
            new QName("http://www.opengis.net/wfs", "PropertyType");

    /** @generated */
    public static final QName QUERYTYPE = new QName("http://www.opengis.net/wfs", "QueryType");

    /** @generated */
    public static final QName RESULTTYPETYPE =
            new QName("http://www.opengis.net/wfs", "ResultTypeType");

    /** @generated */
    public static final QName TRANSACTIONRESPONSETYPE =
            new QName("http://www.opengis.net/wfs", "TransactionResponseType");

    /** @generated */
    public static final QName TRANSACTIONRESULTSTYPE =
            new QName("http://www.opengis.net/wfs", "TransactionResultsType");

    /** @generated */
    public static final QName TRANSACTIONSUMMARYTYPE =
            new QName("http://www.opengis.net/wfs", "TransactionSummaryType");

    /** @generated */
    public static final QName TRANSACTIONTYPE =
            new QName("http://www.opengis.net/wfs", "TransactionType");

    /** @generated */
    public static final QName TYPENAMELISTTYPE =
            new QName("http://www.opengis.net/wfs", "TypeNameListType");

    /** @generated */
    public static final QName UPDATEELEMENTTYPE =
            new QName("http://www.opengis.net/wfs", "UpdateElementType");

    /** @generated */
    public static final QName WFS_CAPABILITIESTYPE =
            new QName("http://www.opengis.net/wfs", "WFS_CapabilitiesType");

    /* Elements */
    /** @generated */
    public static final QName DELETE = new QName("http://www.opengis.net/wfs", "Delete");

    /** @generated */
    public static final QName DESCRIBEFEATURETYPE =
            new QName("http://www.opengis.net/wfs", "DescribeFeatureType");

    /** @generated */
    public static final QName FEATURECOLLECTION =
            new QName("http://www.opengis.net/wfs", "FeatureCollection");

    /** @generated */
    public static final QName FEATURETYPELIST =
            new QName("http://www.opengis.net/wfs", "FeatureTypeList");

    /** @generated */
    public static final QName GETCAPABILITIES =
            new QName("http://www.opengis.net/wfs", "GetCapabilities");

    /** @generated */
    public static final QName GETFEATURE = new QName("http://www.opengis.net/wfs", "GetFeature");

    /** @generated */
    public static final QName GETFEATUREWITHLOCK =
            new QName("http://www.opengis.net/wfs", "GetFeatureWithLock");

    /** @generated */
    public static final QName GETGMLOBJECT =
            new QName("http://www.opengis.net/wfs", "GetGmlObject");

    /** @generated */
    public static final QName INSERT = new QName("http://www.opengis.net/wfs", "Insert");

    /** @generated */
    public static final QName LOCKFEATURE = new QName("http://www.opengis.net/wfs", "LockFeature");

    /** @generated */
    public static final QName LOCKFEATURERESPONSE =
            new QName("http://www.opengis.net/wfs", "LockFeatureResponse");

    /** @generated */
    public static final QName LOCKID = new QName("http://www.opengis.net/wfs", "LockId");

    /** @generated */
    public static final QName NATIVE = new QName("http://www.opengis.net/wfs", "Native");

    /** @generated */
    public static final QName PROPERTY = new QName("http://www.opengis.net/wfs", "Property");

    /** @generated */
    public static final QName PROPERYNAME = new QName("http://www.opengis.net/wfs", "ProperyName");

    /** @generated */
    public static final QName QUERY = new QName("http://www.opengis.net/wfs", "Query");

    /** @generated */
    public static final QName SERVESGMLOBJECTTYPELIST =
            new QName("http://www.opengis.net/wfs", "ServesGMLObjectTypeList");

    /** @generated */
    public static final QName SUPPORTSGMLOBJECTTYPELIST =
            new QName("http://www.opengis.net/wfs", "SupportsGMLObjectTypeList");

    /** @generated */
    public static final QName TRANSACTION = new QName("http://www.opengis.net/wfs", "Transaction");

    /** @generated */
    public static final QName TRANSACTIONRESPONSE =
            new QName("http://www.opengis.net/wfs", "TransactionResponse");

    /** @generated */
    public static final QName UPDATE = new QName("http://www.opengis.net/wfs", "Update");

    /** @generated */
    public static final QName WFS_CAPABILITIES =
            new QName("http://www.opengis.net/wfs", "WFS_Capabilities");

    /** @generated */
    public static final QName XLINKPROPERTYNAME =
            new QName("http://www.opengis.net/wfs", "XlinkPropertyName");

    /* Attributes */

    /** schema type builder */
    FeatureTypeSchemaBuilder schemaBuilder;

    public WFS(FeatureTypeSchemaBuilder schemaBuilder) {
        this.schemaBuilder = schemaBuilder;
    }

    public FeatureTypeSchemaBuilder getSchemaBuilder() {
        return schemaBuilder;
    }

    protected void addDependencies(Set dependencies) {
        super.addDependencies(dependencies);

        dependencies.add(OGC.getInstance());
        dependencies.add(GML.getInstance());
        dependencies.add(OWS.getInstance());
    }

    /** Returns 'http://www.opengis.net/wfs' */
    public String getNamespaceURI() {
        return NAMESPACE;
    }

    /** Returns the location of 'wfs.xsd' */
    public String getSchemaLocation() {
        return org.geotools.wfs.v1_1.WFS.class.getResource("wfs.xsd").toString();
    }
}
