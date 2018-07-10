/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos;

import com.thoughtworks.xstream.XStream;
import java.io.Serializable;
import org.geoserver.qos.xml.DayOfWeek;
import org.geoserver.qos.xml.LimitedAreaRequestConstraints;
import org.geoserver.qos.xml.OperatingInfo;
import org.geoserver.qos.xml.OperatingInfoTime;
import org.geoserver.qos.xml.OwsAbstract;
import org.geoserver.qos.xml.QoSMetadata;
import org.geoserver.qos.xml.QosMainConfiguration;
import org.geoserver.qos.xml.QosRepresentativeOperation;
import org.geoserver.qos.xml.QosWMSOperation;
import org.geoserver.qos.xml.QualityOfServiceStatement;
import org.geoserver.qos.xml.ReferenceType;
import org.geoserver.qos.xml.WfsAdHocQueryConstraints;
import org.geoserver.qos.xml.WfsGetFeatureOperation;
import org.geoserver.qos.xml.WfsStoredQueryConstraintsType;

public class QosXstreamAliasConfigurator implements Serializable {

    public static QosXstreamAliasConfigurator instance() {
        return new QosXstreamAliasConfigurator();
    }

    private QosXstreamAliasConfigurator() {}

    public void configure(XStream xs) {
        xs.allowTypesByWildcard(new String[] {"org.geoserver.qos.xml.**"});
        xs.alias("qosMainConfiguration", QosMainConfiguration.class);
        xs.addDefaultImplementation(QosMainConfiguration.class, QosMainConfiguration.class);
        // QoSMetadata
        xs.addImplicitCollection(
                QoSMetadata.class, "operatingInfo", "operatingInfo", OperatingInfo.class);
        xs.addImplicitCollection(
                QoSMetadata.class, "statements", "statement", QualityOfServiceStatement.class);
        xs.addImplicitCollection(
                QoSMetadata.class,
                "operationAnomalyFeed",
                "operationAnomalyFeed",
                ReferenceType.class);
        xs.addImplicitCollection(
                QoSMetadata.class,
                "representativeOperations",
                "representativeOperation",
                QosRepresentativeOperation.class);
        // OperatingInfo
        xs.addImplicitCollection(
                OperatingInfo.class, "byDaysOfWeek", "byDaysOfWeek", OperatingInfoTime.class);
        // OperatingInfoTime
        xs.addImplicitCollection(OperatingInfoTime.class, "days", "days", DayOfWeek.class);
        // Representative Operations
        xs.addImplicitCollection(
                QosRepresentativeOperation.class,
                "getMapOperations",
                "getMapOperation",
                QosWMSOperation.class);
        xs.addImplicitCollection(
                QosRepresentativeOperation.class,
                "getFeatureInfoOperations",
                "getFeatureInfoOperation",
                QosWMSOperation.class);
        // QosRepresentativeOperation -> qualityOfServiceStatements
        // List<QualityOfServiceStatement>
        xs.addImplicitCollection(
                QosRepresentativeOperation.class,
                "qualityOfServiceStatements",
                "statement",
                QualityOfServiceStatement.class);
        // QosWMSOperation
        xs.addImplicitCollection(
                QosWMSOperation.class,
                "requestOptions",
                "requestOption",
                LimitedAreaRequestConstraints.class);
        // LimitedAreaRequestConstraints
        xs.addImplicitCollection(
                LimitedAreaRequestConstraints.class, "layerNames", "layerName", String.class);
        xs.addImplicitCollection(
                LimitedAreaRequestConstraints.class, "outputFormat", "outputFormat", String.class);
        xs.addImplicitCollection(ReferenceType.class, "abstracts", "abstract", OwsAbstract.class);
        /*
         * WFS alias
         */
        // getFeatureOperation
        xs.addImplicitCollection(
                QosRepresentativeOperation.class,
                "getFeatureOperations",
                "getFeatureOperation",
                WfsGetFeatureOperation.class);
        // adHocQueryConstraints
        xs.addImplicitCollection(
                WfsGetFeatureOperation.class,
                "adHocQueryConstraints",
                "adHocQueryConstraints",
                WfsAdHocQueryConstraints.class);
        xs.addImplicitCollection(
                WfsAdHocQueryConstraints.class, "typeNames", "typeNames", String.class);
        // storedQueryConstraints
        xs.addImplicitCollection(
                WfsGetFeatureOperation.class,
                "storedQueryConstraints",
                "storedQueryConstraints",
                WfsStoredQueryConstraintsType.class);
        xs.addImplicitCollection(
                WfsStoredQueryConstraintsType.class,
                "storedQueryIds",
                "storedQueryIds",
                String.class);
    }
}
