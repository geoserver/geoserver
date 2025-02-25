package org.geoserver.smartdataloader.data.store;

import java.util.Map;
import org.geoserver.smartdataloader.domain.DomainModelVisitorImpl;
import org.geoserver.smartdataloader.domain.entities.DomainEntity;
import org.geoserver.smartdataloader.domain.entities.DomainEntitySimpleAttribute;
import org.geoserver.smartdataloader.domain.entities.DomainModel;

public class ExpressionOverridesDomainModelVisitor extends DomainModelVisitorImpl {

    private final Map<String, String> overrideExpressions;
    private final PkOverrider pkOverrider;

    private DomainEntity currentEntity;

    public ExpressionOverridesDomainModelVisitor(Map<String, String> overrideExpressions) {
        this.overrideExpressions = overrideExpressions;
        this.pkOverrider = new PkOverrider(overrideExpressions);
    }

    @Override
    public void visitDomainModel(DomainModel model) {
        super.visitDomainModel(model);
    }

    @Override
    public void visitDomainEntitySimpleAttribute(DomainEntitySimpleAttribute attribute) {
        if (currentEntity != null) {
            pkOverrider.attributeOverride(currentEntity.getName(), attribute);
            String expression = overrideExpressions.get(getCurrentEntityColumnPath(attribute));
            if (expression != null) {
                attribute.setExpression(expression);
            }
        }
    }

    private String getCurrentEntityColumnPath(DomainEntitySimpleAttribute attribute) {
        return currentEntity.getName() + "." + attribute.getName();
    }

    @Override
    public void visitDomainRootEntity(DomainEntity entity) {
        currentEntity = entity;
    }

    @Override
    public void visitDomainChainedEntity(DomainEntity entity) {
        currentEntity = entity;
    }
}
