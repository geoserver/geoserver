/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.wfs;

import java.io.IOException;
import java.util.Optional;
import org.geoserver.qos.BaseQosXmlEncoder;
import org.geoserver.qos.QosSchema;
import org.geoserver.qos.QosXmlEncoders;
import org.geoserver.qos.xml.QosMainConfiguration;
import org.geoserver.wfs.WFSExtendedCapabilitiesProvider;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.request.GetCapabilitiesRequest;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Provides Quality of Services metadata on WFS GetCapabilities
 *
 * @author Fernando Miño, Geosolutions
 */
public class QosWFSCapabilitiesProvider implements WFSExtendedCapabilitiesProvider {

    private WfsQosConfigurationLoader configLoader;
    protected BaseQosXmlEncoder encoder;

    public QosWFSCapabilitiesProvider() {
        encoder = new WfsQosXmlEncoder();
    }

    @Override
    public String[] getSchemaLocations(String schemaBaseURL) {
        return new String[] {QosSchema.QOS_WFS_NAMESPACE, QosSchema.QOS_WFS_SCHEMA};
    }

    @Override
    public void registerNamespaces(NamespaceSupport namespaces) {
        namespaces.declarePrefix(QosSchema.QOS_PREFIX, QosSchema.QOS_NAMESPACE);
        namespaces.declarePrefix(QosSchema.QOS_WFS_PREFIX, QosSchema.QOS_WFS_NAMESPACE);
        namespaces.declarePrefix(QosSchema.OWS_PREFIX, QosSchema.OWS_NAMESPACE);
    }

    @Override
    public void encode(Translator tx, WFSInfo wfs, GetCapabilitiesRequest request)
            throws IOException {
        QosMainConfiguration config = getConfigLoader().getConfiguration(wfs);
        if (config.getActivated()) {
            tx.start(QosSchema.QOS_WFS_PREFIX + ":QualityOfServiceMetadata");
            // encode operating info
            final String oinfoTag = QosSchema.QOS_PREFIX + ":" + QosXmlEncoders.OPERATING_INFO_TAG;
            Optional<QosMainConfiguration> configOp = Optional.of(config);
            configOp.map(x -> x.getWmsQosMetadata())
                    .map(x -> x.getOperatingInfo())
                    .map(x -> x.stream())
                    .ifPresent(
                            x ->
                                    x.forEach(
                                            o -> {
                                                encoder.encodeOperatingInfo(oinfoTag, tx, o);
                                            }));
            // <qos:QualityOfServiceStatement> ...
            final String qosStatementTag =
                    QosSchema.QOS_PREFIX + ":" + QosXmlEncoders.QOS_STATEMENT_TAG;
            configOp.map(x -> x.getWmsQosMetadata())
                    .map(x -> x.getStatements())
                    .map(x -> x.stream())
                    .ifPresent(
                            x ->
                                    x.forEach(
                                            s ->
                                                    encoder.encodeQualityOfServiceStatement(
                                                            qosStatementTag, tx, s)));
            // <qos:RepresentativeOperation>
            configOp.map(x -> x.getWmsQosMetadata())
                    .map(x -> x.getRepresentativeOperations())
                    .ifPresent(
                            x ->
                                    x.forEach(
                                            o -> {
                                                encoder.encodeRepresentativeOperation(
                                                        QosSchema.QOS_PREFIX
                                                                + ":RepresentativeOperation",
                                                        tx,
                                                        o);
                                            }));
            // <qos:OperationAnomalyFeed xlink:href="myservice.ics">
            // qos:OperationAnomalyFeed is a ReferenceType
            final String oafTag =
                    QosSchema.QOS_PREFIX + ":" + QosXmlEncoders.operationAnomalyFeed_TAG;
            configOp.map(x -> x.getWmsQosMetadata())
                    .map(x -> x.getOperationAnomalyFeed())
                    .map(x -> x.stream())
                    .ifPresent(x -> x.forEach(o -> encoder.encodeReferenceType(oafTag, tx, o)));
            // end main qos metadata tag
            tx.end(QosSchema.QOS_WFS_PREFIX + ":QualityOfServiceMetadata");
        }
    }

    public WfsQosConfigurationLoader getConfigLoader() {
        return configLoader;
    }

    public void setConfigLoader(WfsQosConfigurationLoader configLoader) {
        this.configLoader = configLoader;
    }
}
