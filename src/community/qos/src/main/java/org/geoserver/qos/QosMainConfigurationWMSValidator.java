/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.geoserver.qos.xml.AreaConstraint;
import org.geoserver.qos.xml.LimitedAreaRequestConstraints;
import org.geoserver.qos.xml.OwsRange;
import org.geoserver.qos.xml.QosMainConfiguration;
import org.geoserver.qos.xml.QosRepresentativeOperation;
import org.geoserver.qos.xml.QosWMSOperation;

public class QosMainConfigurationWMSValidator extends QosMainConfigurationValidator {

    public QosMainConfigurationWMSValidator() {}

    public void validate(QosMainConfiguration config) {
        if (config.getWmsQosMetadata() != null) valid(config.getWmsQosMetadata());
    }

    @Override
    public void validateRepresentativeOperation(QosRepresentativeOperation repOp) {
        super.validateRepresentativeOperation(repOp);
        // Validate repOp.getGetMapOperations() and repOp.getGetFeatureInfoOperations()
        if (repOp.getGetMapOperations() != null)
            repOp.getGetMapOperations().forEach(x -> validateWmsOperation(x, "GetMap Operation"));
        if (repOp.getGetFeatureInfoOperations() != null)
            repOp.getGetFeatureInfoOperations()
                    .forEach(x -> validateWmsOperation(x, "GetFeatureInfo Operation"));
        // Validate Statements
        if (CollectionUtils.isEmpty(repOp.getQualityOfServiceStatements())) {
            throw new IllegalArgumentException(
                    "At least one statement required in Representative Operation");
        }
        repOp.getQualityOfServiceStatements().forEach(x -> valid(x));
    }

    protected void validateWmsOperation(QosWMSOperation wmsOp, String entityName) {
        if (wmsOp == null) return;
        if (StringUtils.isEmpty(wmsOp.getHttpMethod()))
            throw new IllegalArgumentException(entityName + " Http Method Required");
        if (wmsOp.getRequestOptions() == null || wmsOp.getRequestOptions().isEmpty())
            throw new IllegalArgumentException(entityName + " Request Options Required");
        wmsOp.getRequestOptions().forEach(x -> validateAreaConstraint(x));
    }

    protected void validateAreaConstraint(LimitedAreaRequestConstraints areaConstrain) {
        if (StringUtils.isEmpty(areaConstrain.getCrs()))
            throw new IllegalArgumentException("Request Constraint CRS Required");
        if (areaConstrain.getLayerNames() == null || areaConstrain.getLayerNames().isEmpty())
            throw new IllegalArgumentException("Request Constraint Layers Required");
        if (areaConstrain.getOutputFormat() == null || areaConstrain.getOutputFormat().isEmpty())
            throw new IllegalArgumentException("Request Constraint Output Formats Required");
        if (areaConstrain.getAreaConstraint() == null)
            throw new IllegalArgumentException("Request Constraint Area Required");
        if (areaConstrain.getImageHeight() != null) {
            validateImageRange(areaConstrain.getImageHeight());
        }
        if (areaConstrain.getImageWidth() != null) {
            validateImageRange(areaConstrain.getImageWidth());
        }
        validateArea(areaConstrain.getAreaConstraint());
    }

    protected void validateImageRange(OwsRange range) {
        Integer min = null, max = null;
        if (StringUtils.isNotBlank(range.getMinimunValue())) {
            try {
                min = Integer.parseInt(range.getMinimunValue());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Number format error on Minimun value");
            }
        }
        if (StringUtils.isNotBlank(range.getMaximunValue())) {
            try {
                max = Integer.parseInt(range.getMaximunValue());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Number format error on Maximun value");
            }
        }
        if (min != null && max != null) {
            if (Integer.compare(min, max) >= 0) {
                throw new IllegalArgumentException(
                        "Minimun value must be lower than Maximun value");
            }
        }
    }

    protected void validateArea(AreaConstraint area) {
        if (area.getMinX() == null)
            throw new IllegalArgumentException("Area Constraint MinX Required");
        if (area.getMinY() == null)
            throw new IllegalArgumentException("Area Constraint MinY Required");
        if (area.getMaxX() == null)
            throw new IllegalArgumentException("Area Constraint MaxX Required");
        if (area.getMaxY() == null)
            throw new IllegalArgumentException("Area Constraint MaxY Required");
    }
}
