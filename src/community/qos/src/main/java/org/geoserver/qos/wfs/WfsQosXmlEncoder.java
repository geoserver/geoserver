/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.wfs;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.geoserver.ExtendedCapabilitiesProvider.Translator;
import org.geoserver.qos.BaseQosXmlEncoder;
import org.geoserver.qos.QosSchema;
import org.geoserver.qos.xml.QosRepresentativeOperation;
import org.geoserver.qos.xml.WfsAdHocQueryConstraints;
import org.geoserver.qos.xml.WfsGetFeatureOperation;

public class WfsQosXmlEncoder extends BaseQosXmlEncoder {

    public WfsQosXmlEncoder() {
        super(QosSchema.QOS_WFS_PREFIX);
    }

    @Override
    public void encodeRepresentativeOperation(
            String tag, Translator tx, QosRepresentativeOperation repOp) {
        tx.start(tag);
        if (CollectionUtils.isNotEmpty(repOp.getGetFeatureOperations())) {
            repOp.getGetFeatureOperations()
                    .forEach(
                            x -> {
                                String gfoTag = QosSchema.QOS_WFS_PREFIX + ":GetFeatureOperation";
                                encodeWfsGetFeatureOperation(gfoTag, tx, x);
                            });
        }
        if (CollectionUtils.isNotEmpty(repOp.getQualityOfServiceStatements())) {
            repOp.getQualityOfServiceStatements()
                    .forEach(
                            x -> {
                                encodeQualityOfServiceStatement(
                                        "qos:QualityOfServiceStatement", tx, x);
                            });
        }
        tx.end(tag);
    }

    public void encodeWfsGetFeatureOperation(
            String tag, Translator tx, WfsGetFeatureOperation gfOp) {
        tx.start(tag);
        // http method
        this.encodeHttpMethod(tx, gfOp.getHttpMethod());
        // List<WfsAdHocQueryConstraints>
        if (CollectionUtils.isNotEmpty(gfOp.getAdHocQueryConstraints())) {
            gfOp.getAdHocQueryConstraints()
                    .forEach(
                            x -> {
                                String adHocTag =
                                        QosSchema.QOS_WFS_PREFIX + ":AdHocQueryConstraints";
                                encodeAdHocQueryConstraints(adHocTag, tx, x);
                            });
        }
        tx.end(tag);
    }

    public String toWfsCrs(String crs) {
        return "urn:ogc:def:crs:EPSG::" + crs.split(":")[1];
    }

    public void encodeAdHocQueryConstraints(
            String tag, Translator tx, WfsAdHocQueryConstraints adHoc) {
        tx.start(tag);
        // <qos:AreaConstraint srsName="urn:ogc:def:crs:EPSG::4258">
        if (adHoc.getAreaConstraint() != null) {
            encodeAreaConstraint(
                    "qos:AreaConstraint", tx, adHoc.getAreaConstraint(), toWfsCrs(adHoc.getCrs()));
        }
        // <qos-wfs:TypeNames>ad:Address</qos-wfs:TypeNames>
        if (CollectionUtils.isNotEmpty(adHoc.getTypeNames())) {
            String typesTag = QosSchema.QOS_WFS_PREFIX + ":TypeNames";
            for (String ti : adHoc.getTypeNames()) {
                tx.start(typesTag);
                tx.chars(ti);
                tx.end(typesTag);
            }
        }
        // <qos:RequestParameterConstraint name="CRS">
        if (StringUtils.isNotEmpty(adHoc.getCrs())) {
            encodeRequestParameterConstraint(
                    tx,
                    "CRS",
                    (Void) -> {
                        encodeOwsValue(tx, toWfsCrs(adHoc.getCrs()));
                    });
        }
        // <qos:RequestParameterConstraint name="OutputFormat">
        if (CollectionUtils.isNotEmpty(adHoc.getOutputFormat())) {
            encodeRequestParameterConstraint(
                    tx,
                    "OutputFormat",
                    (Void) -> {
                        adHoc.getOutputFormat().forEach(x -> encodeOwsValue(tx, x));
                    });
        }
        // <qos:RequestParameterConstraint name="ImageWidth">
        //        OwsRange imgWidth = adHoc.getImageWidth();
        //        if (imgWidth != null
        //                && (StringUtils.isNotEmpty(imgWidth.getMinimunValue())
        //                        || StringUtils.isNotEmpty(imgWidth.getMaximunValue()))) {
        //            encodeRequestParameterConstraint(
        //                    tx,
        //                    "ImageWidth",
        //                    (Void) -> {
        //                        encodeOwsRange(tx, imgWidth.getMinimunValue(),
        // imgWidth.getMaximunValue());
        //                    });
        //        }
        // <qos:RequestParameterConstraint name="ImageHeight">
        //        OwsRange imgHeight = adHoc.getImageHeight();
        //        if (imgHeight != null
        //                && (StringUtils.isNotEmpty(imgHeight.getMinimunValue())
        //                        || StringUtils.isNotEmpty(imgHeight.getMaximunValue()))) {
        //            encodeRequestParameterConstraint(
        //                    tx,
        //                    "ImageHeight",
        //                    (Void) -> {
        //                        encodeOwsRange(
        //                                tx, imgHeight.getMinimunValue(),
        // imgHeight.getMaximunValue());
        //                    });
        //        }
        // <qos:RequestParameterConstraint name="Count">
        if (adHoc.getCount() != null) {
            encodeRequestParameterConstraint(
                    tx,
                    "Count",
                    (Void) -> {
                        encodeOwsValue(tx, adHoc.getCount().toString());
                    });
        }
        // <qos:RequestParameterConstraint name="ResolveReferences">
        if (StringUtils.isNotEmpty(adHoc.getResolveReferences())) {
            encodeRequestParameterConstraint(
                    tx,
                    "ResolveReferences",
                    (Void) -> {
                        encodeOwsValue(tx, adHoc.getResolveReferences());
                    });
        }

        tx.end(tag);
    }

    @Override
    public String owsPrefix() {
        return "ows";
    }
}
