/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.domain.entities;

import org.geoserver.smartdataloader.domain.DomainModelVisitor;

/** Represents a simple attribute of a domain entity. */
public final class DomainEntitySimpleAttribute {

    private String name;
    private String expression;
    private DomainAttributeType type;
    private boolean identifier;

    public DomainEntitySimpleAttribute() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DomainAttributeType getType() {
        return type;
    }

    public void setType(DomainAttributeType type) {
        this.type = type;
    }

    public boolean isIdentifier() {
        return identifier;
    }

    public void setIdentifier(boolean identifier) {
        this.identifier = identifier;
    }

    public String getExpression() {
        return expression != null ? expression : name;
    }

    public boolean hasExpression() {
        return expression != null;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public void accept(DomainModelVisitor visitor) {
        visitor.visitDomainEntitySimpleAttribute(this);
    }
}
