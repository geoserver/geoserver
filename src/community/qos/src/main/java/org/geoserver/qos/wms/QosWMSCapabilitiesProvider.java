/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.wms;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.qos.BaseQosXmlEncoder;
import org.geoserver.qos.QosSchema;
import org.geoserver.qos.QosXmlEncoders;
import org.geoserver.qos.xml.QosMainConfiguration;
import org.geoserver.wms.ExtendedCapabilitiesProvider;
import org.geoserver.wms.GetCapabilitiesRequest;
import org.geoserver.wms.WMSInfo;
import org.geotools.util.NumberRange;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Provides Quality of Service extended capabilities for WMS GetCapabilities
 *
 * @author Fernando Miño, Geosolutions
 */
public class QosWMSCapabilitiesProvider implements ExtendedCapabilitiesProvider {

    private WmsQosConfigurationLoader configLoader;
    protected BaseQosXmlEncoder encoder;

    public QosWMSCapabilitiesProvider() {
        encoder = new WmsQosXmlEncoder();
    }

    @Override
    public String[] getSchemaLocations(String schemaBaseURL) {
        return new String[] {QosSchema.QOS_WMS_NAMESPACE, QosSchema.QOS_WMS_SCHEMA};
    }

    @Override
    public void registerNamespaces(NamespaceSupport namespaces) {
        namespaces.declarePrefix(QosSchema.QOS_PREFIX, QosSchema.QOS_NAMESPACE);
        namespaces.declarePrefix(QosSchema.QOS_WMS_PREFIX, QosSchema.QOS_WMS_NAMESPACE);
        namespaces.declarePrefix(QosSchema.OWS_PREFIX, QosSchema.OWS_NAMESPACE);
    }

    @Override
    public void encode(Translator tx, WMSInfo wms, GetCapabilitiesRequest request)
            throws IOException {
        QosMainConfiguration config = loadConfiguration(wms, request);
        if (config.getActivated()) {
            tx.start(QosSchema.QOS_WMS_PREFIX + ":QualityOfServiceMetadata");
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
            tx.end(QosSchema.QOS_WMS_PREFIX + ":QualityOfServiceMetadata");
        }
    }

    protected QosMainConfiguration loadConfiguration(WMSInfo wms, GetCapabilitiesRequest request) {
        return configLoader.getConfiguration(wms);
    }

    @Override
    public List<String> getVendorSpecificCapabilitiesRoots(GetCapabilitiesRequest request) {
        return Collections.emptyList();
    }

    @Override
    public List<String> getVendorSpecificCapabilitiesChildDecls(GetCapabilitiesRequest request) {
        return Collections.emptyList();
    }

    @Override
    public void customizeRootCrsList(Set<String> srs) {}

    @Override
    public NumberRange<Double> overrideScaleDenominators(
            PublishedInfo layer, NumberRange<Double> scaleDenominators) {
        return scaleDenominators;
    }

    public WmsQosConfigurationLoader getConfigLoader() {
        return configLoader;
    }

    public void setConfigLoader(WmsQosConfigurationLoader configLoader) {
        this.configLoader = configLoader;
    }
}
