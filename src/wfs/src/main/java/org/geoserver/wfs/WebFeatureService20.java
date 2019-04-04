/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import net.opengis.wfs20.CreateStoredQueryResponseType;
import net.opengis.wfs20.CreateStoredQueryType;
import net.opengis.wfs20.DescribeFeatureTypeType;
import net.opengis.wfs20.DescribeStoredQueriesResponseType;
import net.opengis.wfs20.DescribeStoredQueriesType;
import net.opengis.wfs20.DropStoredQueryType;
import net.opengis.wfs20.ExecutionStatusType;
import net.opengis.wfs20.GetCapabilitiesType;
import net.opengis.wfs20.GetFeatureType;
import net.opengis.wfs20.GetFeatureWithLockType;
import net.opengis.wfs20.GetPropertyValueType;
import net.opengis.wfs20.ListStoredQueriesResponseType;
import net.opengis.wfs20.ListStoredQueriesType;
import net.opengis.wfs20.LockFeatureResponseType;
import net.opengis.wfs20.LockFeatureType;
import net.opengis.wfs20.TransactionResponseType;
import net.opengis.wfs20.TransactionType;
import net.opengis.wfs20.ValueCollectionType;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.xml.transform.TransformerBase;

/**
 * Web Feature Service implementation version 2.0.
 *
 * <p>Each of the methods on this class corresponds to an operation as defined by the Web Feature
 * Specification. See {@link "http://www.opengeospatial.org/standards/wfs"} for more details.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public interface WebFeatureService20 {
    /** The configuration of the service. */
    WFSInfo getServiceInfo();

    /**
     * WFS GetCapabilities operation.
     *
     * @param request The get capabilities request.
     * @return A transformer instance capable of serializing a wfs capabilities document.
     * @throws WFSException Any service exceptions.
     */
    TransformerBase getCapabilities(GetCapabilitiesType request) throws WFSException;

    /**
     * WFS DescribeFeatureType operation.
     *
     * @param request The describe feature type request.
     * @return A set of feature type metadata objects.
     * @throws WFSException Any service exceptions.
     */
    FeatureTypeInfo[] describeFeatureType(DescribeFeatureTypeType request) throws WFSException;

    /**
     * WFS GetFeature operation.
     *
     * @param request The get feature request.
     * @return A feature collection type instance.
     * @throws WFSException Any service exceptions.
     */
    FeatureCollectionResponse getFeature(GetFeatureType request) throws WFSException;

    /**
     * WFS GetFeatureWithLock operation.
     *
     * @param request The get feature with lock request.
     * @return A feature collection type instance.
     * @throws WFSException Any service exceptions.
     */
    FeatureCollectionResponse getFeatureWithLock(GetFeatureWithLockType request)
            throws WFSException;

    /**
     * WFS GetPropertyValue operation.
     *
     * @param request The get property value request.
     * @return A value collection type instance.
     * @throws WFSException Any service exceptions.
     */
    ValueCollectionType getPropertyValue(GetPropertyValueType request) throws WFSException;

    /**
     * WFS LockFeatureType operation.
     *
     * @param request The lock feature request.
     * @return A lock feature response type.
     * @throws WFSException An service exceptions.
     */
    LockFeatureResponseType lockFeature(LockFeatureType request) throws WFSException;

    /**
     * WFS transaction operation.
     *
     * @param request The transaction request.
     * @return A transaction response instance.
     * @throws WFSException Any service exceptions.
     */
    TransactionResponseType transaction(TransactionType request) throws WFSException;

    /** WFS list stored query operation. */
    ListStoredQueriesResponseType listStoredQueries(ListStoredQueriesType request)
            throws WFSException;

    /** WFS describe stored query operation. */
    DescribeStoredQueriesResponseType describeStoredQueries(DescribeStoredQueriesType request)
            throws WFSException;

    /** WFS create stored query operation. */
    CreateStoredQueryResponseType createStoredQuery(CreateStoredQueryType request)
            throws WFSException;

    /** WFS drop stored query operation. */
    ExecutionStatusType dropStoredQuery(DropStoredQueryType request) throws WFSException;

    /**
     * Release lock operation.
     *
     * <p>This is not an official operation of the spec.
     *
     * @param lockId A prefiously held lock id.
     */
    void releaseLock(String lockId) throws WFSException;
}
