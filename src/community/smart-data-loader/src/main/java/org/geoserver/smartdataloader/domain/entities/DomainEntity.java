/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.domain.entities;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.geoserver.smartdataloader.domain.DomainModelVisitor;

/** Entity of the domain, an entity contains attributes and relations. */
public final class DomainEntity {

    private final List<DomainEntitySimpleAttribute> attributes = new ArrayList<>();
    private final List<DomainRelation> relations = new ArrayList<>();

    private String name;
    private GmlInfo gmlInfo;

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
        gmlInfo = new GmlInfo(name);
    }

    public GmlInfo getGmlInfo() {
        return gmlInfo;
    }

    public List<DomainEntitySimpleAttribute> getAttributes() {
        return attributes;
    }

    public List<DomainRelation> getRelations() {
        return relations;
    }

    public void add(DomainEntitySimpleAttribute attribute) {
        attributes.add(attribute);
    }

    public void add(DomainRelation relation) {
        relations.add(relation);
    }

    public void accept(DomainModelVisitor visitor, boolean isRoot) {
        if (isRoot) visitor.visitDomainRootEntity(this);
        else visitor.visitDomainChainedEntity(this);
        this.getAttributes().forEach(attrib -> attrib.accept(visitor));
        this.getRelations().forEach(relation -> relation.accept(visitor));
    }

    /** Contain GML naming info related with a domain entity. */
    public static final class GmlInfo {

        private final String entityName;

        private GmlInfo(String name) {
            entityName = getEntityName(name);
        }

        /** Utility method that will convert the name of an entity to a readable word. */
        private static String getEntityName(String name) {
            name = WordUtils.capitalizeFully(name, '_', ' ', '.');
            name = name.replace("_", "");
            name = name.replace(" ", "");
            return name.replace(".", "");
        }

        public String featureTypeName() {
            return entityName + "Feature";
        }

        public String complexTypeName() {
            return entityName + "Type";
        }

        public String complexPropertyTypeName() {
            return entityName + "PropertyType";
        }

        public String complexTypeAttributeName() {
            return StringUtils.uncapitalize(entityName);
        }
    }
}
