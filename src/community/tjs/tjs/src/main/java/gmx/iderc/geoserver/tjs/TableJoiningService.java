/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package gmx.iderc.geoserver.tjs;


import net.opengis.tjs10.*;
import org.geotools.data.wfs.protocol.wfs.WFSException;
import org.geotools.xml.transform.TransformerBase;

/**
 * Table Joining Service implementation, versions 1.0.
 * <p>
 * Each of the methods on this class corresponds to an operation as defined
 * for more details.
 * </p>
 *
 * @author Jos'e Luis Capote, GeoMIX, GEOCUBA
 */
public interface TableJoiningService {
    /**
     * The configuration of the service.
     */
    TJSInfo getServiceInfo();

    /**
     * WFS GetCapabilities operation.
     *
     * @param request The get capabilities request.
     * @return A transformer instance capable of serializing a wfs capabilities
     *         document.
     * @throws WFSException Any service exceptions.
     */
    TransformerBase getCapabilities(GetCapabilitiesType request)
            throws TJSException;

    /**
     * TJS DescribeFrameworks operation.
     *
     * @param request The describe frameworks type request.
     * @return The framework descriptions.
     * @throws TJSException Any service exceptions.
     */
    TransformerBase describeFrameworks(DescribeFrameworksType request)
            throws TJSException;

    /**
     * TJS DescribeKey operation.
     *
     * @param request The describe frameworks type request.
     * @return The framework descriptions.
     * @throws TJSException Any service exceptions.
     */
    TransformerBase describeKey(DescribeKeyType request)
            throws TJSException;

    /**
     * TJS DescribeDatasets operation.
     */
    TransformerBase DescribeDatasets(DescribeDatasetsType request)
            throws TJSException;

    /**
     * TJS DescribeData operation.
     */
    TransformerBase DescribeData(DescribeDataType request)
            throws TJSException;

    /**
     * TJS getData operation.
     */
    TransformerBase getData(GetDataType request)
            throws TJSException;

    /**
     * TJS DescribeJoinAbilities operation.
     */
    TransformerBase DescribeJoinAbilities(RequestBaseType request)
            throws TJSException;

    /**
     * TJS JoinData operation.
     */
    TransformerBase JoinData(JoinDataType request)
            throws TJSException;
}
