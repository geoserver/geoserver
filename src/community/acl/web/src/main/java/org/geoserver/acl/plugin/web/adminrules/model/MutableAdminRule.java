/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.web.adminrules.model;

import java.io.Serializable;
import lombok.Data;
import org.geoserver.acl.domain.adminrules.AdminGrantType;
import org.geoserver.acl.domain.adminrules.AdminRule;
import org.geoserver.acl.domain.adminrules.AdminRuleIdentifier;

/**
 * @author ETj (etj at geo-solutions.it) - Originally as part of GeoFence
 * @author Gabriel Roldan - Camptocamp
 */
@Data
@SuppressWarnings("serial")
public class MutableAdminRule implements Serializable, Cloneable {

    private String id;
    private long priority;

    private String userName;
    private String roleName;

    private String extId;
    private String name;
    private String description;

    private String addressRange;

    private String workspace;

    private AdminGrantType access;

    public MutableAdminRule() {
        access = AdminGrantType.USER;
        priority = 0;
    }

    public MutableAdminRule(AdminRule rule) {
        from(rule);
    }

    public MutableAdminRule from(AdminRule rule) {
        setId(rule.getId());
        setPriority(rule.getPriority());

        setUserName(rule.getIdentifier().getUsername());
        setRoleName(rule.getIdentifier().getRolename());

        setExtId(rule.getExtId());
        setName(rule.getName());
        setDescription(rule.getDescription());

        setAddressRange(rule.getIdentifier().getAddressRange());

        setWorkspace(rule.getIdentifier().getWorkspace());
        setAccess(rule.getAccess());
        return this;
    }

    public AdminRule toRule() {
        return syncTo(AdminRule.builder());
    }

    public AdminRule toRule(AdminRule current) {
        return syncTo(current.toBuilder());
    }

    private AdminRule syncTo(AdminRule.Builder builder) {
        return builder.id(getId())
                .priority(getPriority())
                .access(getAccess())
                .extId(getExtId())
                .name(getName())
                .description(getDescription())
                .identifier(AdminRuleIdentifier.builder()
                        .username(getUserName())
                        .rolename(getRoleName())
                        .addressRange(getAddressRange())
                        .workspace(getWorkspace())
                        .build())
                .build();
    }

    public @Override MutableAdminRule clone() {
        try {
            return (MutableAdminRule) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
