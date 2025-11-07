/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.web.accessrules.model;

import java.io.Serializable;
import lombok.Data;
import lombok.NonNull;
import org.geoserver.acl.domain.rules.CatalogMode;
import org.geoserver.acl.domain.rules.GrantType;
import org.geoserver.acl.domain.rules.LayerDetails;
import org.geoserver.acl.domain.rules.Rule;
import org.geoserver.acl.domain.rules.Rule.Builder;
import org.geoserver.acl.domain.rules.RuleIdentifier;
import org.geoserver.acl.domain.rules.RuleLimits;

@Data
@SuppressWarnings("serial")
public class MutableRule implements Serializable, Cloneable {

    private String id;
    private long priority;
    private GrantType access = GrantType.DENY;

    private String extId;
    private String name;
    private String description;

    private String userName;
    private String roleName;

    private String addressRange;

    private String service;
    private String request;
    private String subfield;
    private String workspace;
    private String layer;

    private MutableRuleLimits ruleLimits = new MutableRuleLimits();

    private MutableLayerDetails layerDetails = new MutableLayerDetails();

    private CatalogMode catalogMode;

    public MutableRule() {
        this(Rule.deny());
    }

    public MutableRule(@NonNull Rule rule) {
        this(rule, null);
    }

    public MutableRule(@NonNull Rule rule, LayerDetails layerDetails) {
        setId(rule.getId());
        setPriority(rule.getPriority());
        setAccess(rule.getIdentifier().getAccess());

        setExtId(rule.getExtId());
        setName(rule.getName());
        setDescription(rule.getDescription());

        setAddressRange(rule.getIdentifier().getAddressRange());

        setUserName(rule.getIdentifier().getUsername());
        setRoleName(rule.getIdentifier().getRolename());

        setService(rule.getIdentifier().getService());
        setRequest(rule.getIdentifier().getRequest());
        setSubfield(rule.getIdentifier().getSubfield());
        setWorkspace(rule.getIdentifier().getWorkspace());
        setLayer(rule.getIdentifier().getLayer());

        if (null != rule.getRuleLimits()) {
            setRuleLimits(new MutableRuleLimits(rule.getRuleLimits()));
        } else if (null != layerDetails) {
            setLayerDetails(new MutableLayerDetails(layerDetails));
        }
    }

    public void setRuleLimits(MutableRuleLimits limits) {
        this.ruleLimits = limits;
        if (null != limits) this.catalogMode = limits.getCatalogMode();
    }

    public void setLayerDetails(MutableLayerDetails details) {
        this.layerDetails = details;
        if (null != details) this.catalogMode = details.getCatalogMode();
    }

    //    public CatalogMode getCatalogMode() {
    //        CatalogMode rlcm = ruleLimits == null ? null : ruleLimits.getCatalogMode();
    //        CatalogMode ldcm = layerDetails == null ? null : layerDetails.getCatalogMode();
    //        return getStricter(rlcm, ldcm);
    //    }
    //
    //    public void setCatalogMode(CatalogMode catalogMode) {
    //        if (null != ruleLimits) ruleLimits.setCatalogMode(catalogMode);
    //        if (null != layerDetails && canHaveLayerDetails())
    // layerDetails.setCatalogMode(catalogMode);
    //    }

    public boolean canHaveLayerDetails() {
        GrantType access = getAccess();
        String layer = getLayer();
        return GrantType.ALLOW == access && null != layer;
    }

    public LayerDetails toLayerDetails() {
        LayerDetails ld = null;
        if (canHaveLayerDetails() && this.catalogMode != null) {
            if (layerDetails == null) layerDetails = new MutableLayerDetails();
            layerDetails.setCatalogMode(catalogMode);
            ld = layerDetails.toLayerDetails();
        }
        return ld;
    }

    protected static CatalogMode getStricter(CatalogMode m1, CatalogMode m2) {
        if (m1 == null) return m2;
        if (m2 == null) return m1;

        if (CatalogMode.HIDE == m1 || CatalogMode.HIDE == m2) return CatalogMode.HIDE;

        if (CatalogMode.MIXED == m1 || CatalogMode.MIXED == m2) return CatalogMode.MIXED;

        return CatalogMode.CHALLENGE;
    }

    public Rule toRule() {
        return toRule(Rule.builder());
    }

    public Rule toRule(Rule rule) {
        return toRule(rule.toBuilder());
    }

    private Rule toRule(Builder builder) {
        RuleIdentifier.Builder idb = RuleIdentifier.builder();
        idb.access(getAccess());
        idb.addressRange(getAddressRange());

        idb.username(getUserName());
        idb.rolename(getRoleName());

        idb.service(getService());
        idb.request(getRequest());
        idb.subfield(getSubfield());

        idb.workspace(getWorkspace());
        idb.layer(getLayer());

        Rule rule = builder.identifier(idb.build())
                .id(getId())
                .priority(getPriority())
                .extId(getExtId())
                .name(getName())
                .description(getDescription())
                .ruleLimits(ruleLimits())
                .build();
        return rule;
    }

    private RuleLimits ruleLimits() {
        if (GrantType.LIMIT != access) {
            return null;
        }

        return RuleLimits.builder()
                .allowedArea(ruleLimits.getAllowedArea())
                .spatialFilterType(ruleLimits.getSpatialFilterType())
                .catalogMode(this.getCatalogMode()) // pivot from this
                .build();
    }

    public @Override MutableRule clone() {
        try {
            return (MutableRule) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
