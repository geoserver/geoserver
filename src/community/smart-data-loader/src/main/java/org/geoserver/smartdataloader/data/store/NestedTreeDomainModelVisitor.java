/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.data.store;

import java.util.HashMap;
import java.util.Map;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import org.geoserver.smartdataloader.domain.DomainModelVisitorImpl;
import org.geoserver.smartdataloader.domain.entities.DomainEntity;
import org.geoserver.smartdataloader.domain.entities.DomainEntitySimpleAttribute;
import org.geoserver.smartdataloader.domain.entities.DomainRelation;

/**
 * DomainModelVisitor that translates DomainModel representation into the required structure for
 * NestedTree.
 */
public class NestedTreeDomainModelVisitor extends DomainModelVisitorImpl {

    private DefaultTreeModel treeModel;
    private DefaultMutableTreeNode root;

    private Map<String, DefaultMutableTreeNode> entities = new HashMap<>();

    private DefaultMutableTreeNode currentTreeNode;

    @Override
    public void visitDomainRootEntity(DomainEntity entity) {
        String de = entity.getName();
        if (treeModel == null) {
            root = new DefaultMutableTreeNode(de);
            treeModel = new DefaultTreeModel(root);
            entities.put(entity.getName(), root);
            currentTreeNode = root;
        }
    }

    @Override
    public void visitDomainChainedEntity(DomainEntity entity) {
        String de = entity.getName();
        DefaultMutableTreeNode chainedEntity = addNodes(currentTreeNode, de);
        entities.put(entity.getName(), chainedEntity);
        currentTreeNode = chainedEntity;
    }

    @Override
    public void visitDomainEntitySimpleAttribute(DomainEntitySimpleAttribute domainAttribute) {
        String da = domainAttribute.getName();
        addNodes(currentTreeNode, da);
    }

    @Override
    public void visitDomainRelation(DomainRelation relation) {
        currentTreeNode = entities.get(relation.getContainingEntity().getName());
    }

    public DefaultTreeModel getTreeModel() {
        return treeModel;
    }

    private DefaultMutableTreeNode addNodes(DefaultMutableTreeNode parent, String... childrenNode) {
        DefaultMutableTreeNode newNode = null;
        for (String childNode : childrenNode) {
            newNode = new DefaultMutableTreeNode(childNode);
            parent.add(newNode);
        }
        return newNode;
    }
}
