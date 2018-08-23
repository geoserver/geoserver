/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.xml;

import java.io.Serializable;

public class AssessedOperationType implements Serializable {
    private static final long serialVersionUID = 1L;

    private AbstractOperation operation;
    private QualityOfServiceStatement qosStatement;

    public AssessedOperationType() {}

    /**
     * <element ref="qos:_Operation" minOccurs="0" maxOccurs="unbounded"/> <element
     * name="_Operation" type="qos:AbstractOperationType" abstract="true"/> <complexType
     * name="AbstractOperationType" abstract="true"> <sequence> <element ref="ows:DCP" minOccurs="0"
     * maxOccurs="unbounded"/> <element name="Constraint" type="ows:DomainType" minOccurs="0"
     * maxOccurs="unbounded"/> </sequence> </complexType>
     */
    public AbstractOperation getOperation() {
        return operation;
    }

    public void setOperation(AbstractOperation operation) {
        this.operation = operation;
    }

    public QualityOfServiceStatement getQosStatement() {
        return qosStatement;
    }

    public void setQosStatement(QualityOfServiceStatement qosStatement) {
        this.qosStatement = qosStatement;
    }
}
