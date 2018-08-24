/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.xml;

import java.util.ArrayList;
import java.util.Arrays;

public class WmsQosConfigurationTest {

    public static QosMainConfiguration buildConfigExample() {
        QosMainMetadata wms = new QosMainMetadata();
        wms.setOperatingInfo(
                new ArrayList<>(
                        Arrays.asList(
                                new OperatingInfo[] {OperatingInfoTest.buildOperationInfoEx1()})));
        wms.setStatements(
                new ArrayList<>(
                        Arrays.asList(
                                new QualityOfServiceStatement[] {
                                    QualityOfServiceStatementTest.buildStatement_ex1()
                                })));
        wms.setOperationAnomalyFeed(
                new ArrayList<>(
                        Arrays.asList(
                                new ReferenceType[] {
                                    ReferenceTypeTest.buildOperationalAnomalyFeed_ex1()
                                })));
        QosMainConfiguration config = new QosMainConfiguration();
        config.setWmsQosMetadata(wms);
        config.setActivated(true);
        return config;
    }
}
