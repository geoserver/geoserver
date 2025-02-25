package org.geoserver.smartdataloader.visitors.appschema;

import java.io.Serializable;
import org.geoserver.smartdataloader.domain.entities.DomainEntitySimpleAttribute;

public class IdExpression implements Serializable {

    private String name;
    private String expression;

    public IdExpression(String name, String expression) {
        this.name = name;
        this.expression = expression;
    }

    public IdExpression(DomainEntitySimpleAttribute attribute) {
        this.name = attribute.getName();
        if (attribute.hasExpression()) {
            this.expression = attribute.getExpression();
        } else {
            this.expression = null;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public String getOCQLDefinition() {
        if (expression == null) {
            return name;
        }
        return expression;
    }
}
