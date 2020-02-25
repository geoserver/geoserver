/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import net.opengis.wfs.DescribeFeatureTypeType;
import net.opengis.wfs.GetCapabilitiesType;
import net.opengis.wfs.GetFeatureType;
import net.opengis.wfs.GetFeatureWithLockType;
import net.opengis.wfs.GetGmlObjectType;
import net.opengis.wfs.LockFeatureResponseType;
import net.opengis.wfs.LockFeatureType;
import net.opengis.wfs.TransactionResponseType;
import net.opengis.wfs.TransactionType;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.xml.transform.TransformerBase;

/**
 * Web Feature Service implementation, versions 1.0, and 1.1.
 *
 * <p>Each of the methods on this class corresponds to an operation as defined by the Web Feature
 * Specification. See {@link "http://www.opengeospatial.org/standards/wfs"} for more details.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public interface WebFeatureService {
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
     * WFS LockFeatureType operation.
     *
     * @param request The lock feature request.
     * @return A lock feture response type.
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

    /**
     * WFS GetGmlObject operation.
     *
     * @param request The GetGmlObject request.
     * @return The gml object request.
     * @throws WFSException Any service exceptions.
     */
    Object getGmlObject(GetGmlObjectType request) throws WFSException;

    /**
     * Release lock operation.
     *
     * <p>This is not an official operation of the spec.
     *
     * @param lockId A prefiously held lock id.
     */
    void releaseLock(String lockId) throws WFSException;
}
