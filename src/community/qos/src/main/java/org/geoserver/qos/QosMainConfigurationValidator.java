/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.geoserver.qos.xml.OperatingInfo;
import org.geoserver.qos.xml.OperatingInfoTime;
import org.geoserver.qos.xml.QosMainConfiguration;
import org.geoserver.qos.xml.QosMainMetadata;
import org.geoserver.qos.xml.QosRepresentativeOperation;
import org.geoserver.qos.xml.QualityOfServiceStatement;
import org.geoserver.qos.xml.QualityOfServiceStatement.ValueType;
import org.geoserver.qos.xml.ReferenceType;

public abstract class QosMainConfigurationValidator implements Serializable {

    public QosMainConfigurationValidator() {}

    public void validate(QosMainConfiguration config) {
        if (config.getActivated() && config.getWmsQosMetadata() != null) {
            valid(config.getWmsQosMetadata());
        }
    }

    protected void valid(QosMainMetadata metadata) {
        // Validate Operating Info
        if (metadata.getOperatingInfo() != null) metadata.getOperatingInfo().forEach(o -> valid(o));
        // Validate Statements
        if (metadata.getStatements() != null) metadata.getStatements().forEach(s -> valid(s));
        // Validate Anomaly Feeds
        if (metadata.getOperationAnomalyFeed() != null)
            metadata.getOperationAnomalyFeed().forEach(f -> validateAnomalyFeed(f));
        // Validate Representative Operations
        if (metadata.getRepresentativeOperations() != null) {
            metadata.getRepresentativeOperations().forEach(o -> validateRepresentativeOperation(o));
        }
    }

    protected void valid(OperatingInfo opInfo) {
        if (opInfo.getOperationalStatus() == null)
            throw new IllegalArgumentException("Operational Status is required");
        if (StringUtils.isEmpty(opInfo.getOperationalStatus().getTitle()))
            throw new IllegalArgumentException("Operational Status Title is required");
        if (opInfo.getByDaysOfWeek() == null || opInfo.getByDaysOfWeek().isEmpty())
            throw new IllegalArgumentException("Operational Status ByDaysOfWeek is required");
        opInfo.getByDaysOfWeek().forEach(b -> valid(b));
    }

    protected void valid(OperatingInfoTime opTime) {
        if (opTime.getDays() == null || opTime.getDays().isEmpty())
            throw new IllegalArgumentException("ByDaysOfWeek Days required");
        if (opTime.getStartTime() == null || opTime.getEndTime() == null)
            throw new IllegalArgumentException("ByDaysOfWeek Start and End time required");
        if (opTime.getStartTime().compareTo(opTime.getEndTime()) >= 0)
            throw new IllegalArgumentException(
                    "ByDaysOfWeek Start time must be lower than End time");
    }

    public void valid(QualityOfServiceStatement statement) {
        QualityOfServiceStatement st = statement;
        if (st.getMetric() == null) throw new IllegalArgumentException("Statement Metric Required");
        if (StringUtils.isEmpty(st.getMetric().getHref()))
            throw new IllegalArgumentException("Statement Metrics Href Required");
        if (StringUtils.isEmpty(st.getMetric().getTitle()))
            throw new IllegalArgumentException("Statement Metrics Title Required");
        if (st.getMeassure() == null)
            throw new IllegalArgumentException("Statement Value Required");
        if (st.getValueType() == null)
            throw new IllegalArgumentException("Statement Value Type Required");
        // check Uom for more and less than or equal valueType
        if (st.getValueType().equals(ValueType.lessThanOrEqual)
                || st.getValueType().equals(ValueType.moreThanOrEqual)) {
            if (StringUtils.isEmpty(st.getMeassure().getUom()))
                throw new IllegalArgumentException("Statement Value Uom Required");
        }
        if (StringUtils.isEmpty(st.getMeassure().getValue()))
            throw new IllegalArgumentException("Statement Value Required");
        // if value is not number pattern
        if (!NumberUtils.isNumber(st.getMeassure().getValue()))
            throw new IllegalArgumentException("Statement Value is not a number");
    }

    public void validateAnomalyFeed(ReferenceType anomalyFeed) {
        if (StringUtils.isEmpty(anomalyFeed.getHref()))
            throw new IllegalArgumentException("Anomaly Feed URL required");
        if (StringUtils.isEmpty(anomalyFeed.getAbstractOne()))
            throw new IllegalArgumentException("Anomaly Feed Description required");
        if (StringUtils.isEmpty(anomalyFeed.getFormat()))
            throw new IllegalArgumentException("Anomaly Feed Format required");
    }

    public void validateRepresentativeOperation(QosRepresentativeOperation repOp) {
        Optional<QosRepresentativeOperation> ro = Optional.ofNullable(repOp);
        // Validate internal Statements
        ro.map(QosRepresentativeOperation::getQualityOfServiceStatements)
                .map(List::stream)
                .ifPresent(s -> s.forEach(st -> valid(st)));
    }
}
