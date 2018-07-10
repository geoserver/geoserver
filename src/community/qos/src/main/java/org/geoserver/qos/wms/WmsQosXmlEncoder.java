/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.wms;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.geoserver.ExtendedCapabilitiesProvider.Translator;
import org.geoserver.qos.BaseQosXmlEncoder;
import org.geoserver.qos.QosSchema;
import org.geoserver.qos.xml.QosRepresentativeOperation;
import org.geoserver.qos.xml.QosWMSOperation;

public class WmsQosXmlEncoder extends BaseQosXmlEncoder {

    public WmsQosXmlEncoder() {
        super(QosSchema.QOS_WMS_PREFIX);
    }

    @Override
    public void encodeRepresentativeOperation(
            String tag, Translator tx, QosRepresentativeOperation repOp) {
        tx.start(tag);
        // GetMapOperations
        if (CollectionUtils.isNotEmpty(repOp.getGetMapOperations())) {
            repOp.getGetMapOperations()
                    .forEach(
                            x ->
                                    encodeWmsOperation(
                                            servicePrefix + ":" + "GetMapOperation", tx, x));
        }
        // GetFeatureInfoOperations
        // repOp.getGetFeatureInfoOperations()
        if (CollectionUtils.isNotEmpty(repOp.getGetFeatureInfoOperations())) {
            repOp.getGetFeatureInfoOperations()
                    .forEach(
                            x ->
                                    encodeWmsOperation(
                                            servicePrefix + ":" + "GetFeatureInfoOperation",
                                            tx,
                                            x));
        }
        // translate statements:
        if (CollectionUtils.isNotEmpty(repOp.getQualityOfServiceStatements()))
            repOp.getQualityOfServiceStatements()
                    .forEach(
                            x -> {
                                encodeQualityOfServiceStatement(
                                        QosSchema.QOS_PREFIX + ":" + QOS_STATEMENT_TAG, tx, x);
                            });
        tx.end(tag);
    }

    public void encodeWmsOperation(String tag, Translator tx, QosWMSOperation wmsop) {
        tx.start(tag);
        // encode method: <ows:DCP> <ows:HTTP> <ows:Get></ows:Get>
        if (StringUtils.isNotEmpty(wmsop.getHttpMethod())) {
            encodeHttpMethod(tx, wmsop.getHttpMethod());
        }
        // <qos-wms:RequestOption>
        if (CollectionUtils.isNotEmpty(wmsop.getRequestOptions())) {
            // LimitedAreaRequestConstraints -> qos-wms:RequestOption
            wmsop.getRequestOptions()
                    .forEach(
                            x ->
                                    encodeLimitedAreaRequestConstraint(
                                            QosSchema.QOS_WMS_PREFIX + ":RequestOption", tx, x));
        }
        tx.end(tag);
    }
}
