/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml.v1_0_0;

import java.io.IOException;
import java.util.Set;
import javax.xml.namespace.QName;
import org.eclipse.xsd.XSDSchema;
import org.geoserver.wfs.xml.FeatureTypeSchemaBuilder;
import org.geotools.filter.v1_0.OGC;
import org.geotools.gml2.GML;
import org.geotools.xsd.XSD;

/**
 * XSD object for GeoServer WFS 1.0.
 *
 * <p>This object is not a singleton in the conventional java sense as the other XSD subclasses
 * (GML,OGC,OWS,etc..) are. It is a singleton, but managed as such by the spring container. The
 * reason being that it requires the catalog to operate and build the underlying schema.
 */
public final class WFS extends XSD {

    /** @generated */
    public static final String NAMESPACE = "http://www.opengis.net/wfs";

    public static final String CANONICAL_SCHEMA_LOCATION_BASIC =
            "http://schemas.opengis.net/wfs/1.0.0/WFS-basic.xsd";

    public static final String CANONICAL_SCHEMA_LOCATION_CAPABILITIES =
            "http://schemas.opengis.net/wfs/1.0.0/WFS-capabilities.xsd";

    /* Type Definitions */
    /** @generated */
    public static final QName ALLSOMETYPE = new QName("http://www.opengis.net/wfs", "AllSomeType");

    /** @generated */
    public static final QName DELETEELEMENTTYPE =
            new QName("http://www.opengis.net/wfs", "DeleteElementType");

    /** @generated */
    public static final QName DESCRIBEFEATURETYPETYPE =
            new QName("http://www.opengis.net/wfs", "DescribeFeatureTypeType");

    /** @generated */
    public static final QName EMPTYTYPE = new QName("http://www.opengis.net/wfs", "EmptyType");

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
    public static final QName GETCAPABILITIESTYPE =
            new QName("http://www.opengis.net/wfs", "GetCapabilitiesType");

    /** @generated */
    public static final QName GETFEATURETYPE =
            new QName("http://www.opengis.net/wfs", "GetFeatureType");

    /** @generated */
    public static final QName GETFEATUREWITHLOCKTYPE =
            new QName("http://www.opengis.net/wfs", "GetFeatureWithLockType");

    /** @generated */
    public static final QName INSERTELEMENTTYPE =
            new QName("http://www.opengis.net/wfs", "InsertElementType");

    /** @generated */
    public static final QName INSERTRESULTTYPE =
            new QName("http://www.opengis.net/wfs", "InsertResultType");

    /** @generated */
    public static final QName LOCKFEATURETYPE =
            new QName("http://www.opengis.net/wfs", "LockFeatureType");

    /** @generated */
    public static final QName LOCKTYPE = new QName("http://www.opengis.net/wfs", "LockType");

    /** @generated */
    public static final QName NATIVETYPE = new QName("http://www.opengis.net/wfs", "NativeType");

    /** @generated */
    public static final QName PROPERTYTYPE =
            new QName("http://www.opengis.net/wfs", "PropertyType");

    /** @generated */
    public static final QName QUERYTYPE = new QName("http://www.opengis.net/wfs", "QueryType");

    /** @generated */
    public static final QName STATUSTYPE = new QName("http://www.opengis.net/wfs", "StatusType");

    /** @generated */
    public static final QName TRANSACTIONRESULTTYPE =
            new QName("http://www.opengis.net/wfs", "TransactionResultType");

    /** @generated */
    public static final QName TRANSACTIONTYPE =
            new QName("http://www.opengis.net/wfs", "TransactionType");

    /** @generated */
    public static final QName UPDATEELEMENTTYPE =
            new QName("http://www.opengis.net/wfs", "UpdateElementType");

    /** @generated */
    public static final QName WFS_LOCKFEATURERESPONSETYPE =
            new QName("http://www.opengis.net/wfs", "WFS_LockFeatureResponseType");

    /** @generated */
    public static final QName WFS_TRANSACTIONRESPONSETYPE =
            new QName("http://www.opengis.net/wfs", "WFS_TransactionResponseType");

    /* Elements */
    /** @generated */
    public static final QName DELETE = new QName("http://www.opengis.net/wfs", "Delete");

    /** @generated */
    public static final QName DESCRIBEFEATURETYPE =
            new QName("http://www.opengis.net/wfs", "DescribeFeatureType");

    /** @generated */
    public static final QName FAILED = new QName("http://www.opengis.net/wfs", "FAILED");

    /** @generated */
    public static final QName FEATURECOLLECTION =
            new QName("http://www.opengis.net/wfs", "FeatureCollection");

    /** @generated */
    public static final QName GETCAPABILITIES =
            new QName("http://www.opengis.net/wfs", "GetCapabilities");

    /** @generated */
    public static final QName GETFEATURE = new QName("http://www.opengis.net/wfs", "GetFeature");

    /** @generated */
    public static final QName GETFEATUREWITHLOCK =
            new QName("http://www.opengis.net/wfs", "GetFeatureWithLock");

    /** @generated */
    public static final QName INSERT = new QName("http://www.opengis.net/wfs", "Insert");

    /** @generated */
    public static final QName LOCKFEATURE = new QName("http://www.opengis.net/wfs", "LockFeature");

    /** @generated */
    public static final QName LOCKID = new QName("http://www.opengis.net/wfs", "LockId");

    /** @generated */
    public static final QName NATIVE = new QName("http://www.opengis.net/wfs", "Native");

    /** @generated */
    public static final QName PARTIAL = new QName("http://www.opengis.net/wfs", "PARTIAL");

    /** @generated */
    public static final QName PROPERTY = new QName("http://www.opengis.net/wfs", "Property");

    /** @generated */
    public static final QName QUERY = new QName("http://www.opengis.net/wfs", "Query");

    /** @generated */
    public static final QName SUCCESS = new QName("http://www.opengis.net/wfs", "SUCCESS");

    /** @generated */
    public static final QName TRANSACTION = new QName("http://www.opengis.net/wfs", "Transaction");

    /** @generated */
    public static final QName UPDATE = new QName("http://www.opengis.net/wfs", "Update");

    /** @generated */
    public static final QName WFS_LOCKFEATURERESPONSE =
            new QName("http://www.opengis.net/wfs", "WFS_LockFeatureResponse");

    /** @generated */
    public static final QName WFS_TRANSACTIONRESPONSE =
            new QName("http://www.opengis.net/wfs", "WFS_TransactionResponse");

    /* Attributes */

    /** schema type builder */
    FeatureTypeSchemaBuilder schemaBuilder;

    public WFS(FeatureTypeSchemaBuilder schemaBuilder) {
        this.schemaBuilder = schemaBuilder;
    }

    public FeatureTypeSchemaBuilder getSchemaBuilder() {
        return schemaBuilder;
    }

    /** Adds dependencies on the filter and gml schemas. */
    protected void addDependencies(Set dependencies) {
        dependencies.add(OGC.getInstance());
        dependencies.add(GML.getInstance());
    }

    /** Returns 'http://www.opengis.net/wfs' */
    public String getNamespaceURI() {
        return NAMESPACE;
    }

    /** Returns the location of 'WFS-transaction.xsd' */
    public String getSchemaLocation() {
        return getClass().getResource("WFS-transaction.xsd").toString();
    }

    /**
     * Suplements the schema built by the parent by adding hte aplication schema feature typs
     * defined in GeoServer.
     */
    protected XSDSchema buildSchema() throws IOException {
        XSDSchema wfsSchema = super.buildSchema();
        wfsSchema = schemaBuilder.addApplicationTypes(wfsSchema);
        return wfsSchema;
    }
}
