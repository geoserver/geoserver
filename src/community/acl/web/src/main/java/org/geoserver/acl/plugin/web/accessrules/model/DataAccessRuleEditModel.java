/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.web.accessrules.model;

import jakarta.annotation.Nullable;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.geoserver.acl.domain.rules.GrantType;
import org.geoserver.acl.domain.rules.LayerDetails;
import org.geoserver.acl.domain.rules.Rule;
import org.geoserver.acl.domain.rules.RuleAdminService;
import org.geoserver.acl.plugin.web.accessrules.event.PublishedInfoChangeEvent;
import org.geoserver.acl.plugin.web.components.AbstractRuleEditModel;
import org.geoserver.acl.plugin.web.support.ApplicationContextSupport;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.StyleInfo;
import org.geotools.util.logging.Logging;
import org.springframework.util.StringUtils;

@SuppressWarnings("serial")
public class DataAccessRuleEditModel extends AbstractRuleEditModel<MutableRule> {
    private static final Logger log = Logging.getLogger(DataAccessRuleEditModel.class);

    /**
     * @see #workSpaceNameChanged(String)
     * @see #layerNameChanged(String)
     */
    private final PublishedInfoDetachableModel publishedInfoModel = new PublishedInfoDetachableModel();

    public DataAccessRuleEditModel() {
        this(new MutableRule());
    }

    public DataAccessRuleEditModel(@NonNull MutableRule rule) {
        super(rule);
        final String ruleID = rule.getId();
        if (null != ruleID && rule.canHaveLayerDetails()) {
            adminService().getLayerDetails(ruleID).map(MutableLayerDetails::new).ifPresent(rule::setLayerDetails);
        }
        if (null != rule.getLayer()) {
            publishedInfoModel.setObject(rule.getWorkspace(), rule.getLayer());
            PublishedInfo info = publishedInfoModel.getObject();
            if (null != info) {
                updateModelFor(info);
            }
        }
    }

    public LayerDetailsEditModel layerDetails() {
        return new LayerDetailsEditModel(this);
    }

    public IModel<PublishedInfo> getPublishedInfoModel() {
        return publishedInfoModel;
    }

    @Override
    protected String getRoleName(MutableRule rule) {
        return rule.getRoleName();
    }

    @Override
    public void save() {
        final MutableRule modelRule = getModelObject();

        RuleAdminService service = adminService();
        final Rule rule;
        if (null == modelRule.getId()) {
            Rule newRule = modelRule.toRule();
            rule = service.insert(newRule);
        } else {
            Rule current = loadDomainRule();
            Rule toUpdate = modelRule.toRule(current);
            // this also removes the LayerDetails if its no longer applicable (e.g. the
            // grant type is no longer ALLOW and/or there's no layer set)
            rule = service.update(toUpdate);
        }
        LayerDetails ld = modelRule.toLayerDetails();
        if (null != ld) {
            service.setLayerDetails(rule.getId(), ld);
        }
        getModel().setObject(new MutableRule(rule, ld));
    }

    public Rule loadDomainRule() {
        MutableRule modelRule = getModelObject();
        RuleAdminService service = adminService();
        Rule current = service.get(modelRule.getId())
                .orElseThrow(() -> new IllegalStateException("The rule no longer exists"));
        return current;
    }

    private RuleAdminService adminService() {
        return ApplicationContextSupport.getBeanOfType(RuleAdminService.class);
    }

    protected @Override String getSelectedWorkspace() {
        return getModelObject().getWorkspace();
    }

    public Iterator<String> getStyleChoices(@Nullable String input) {
        Catalog catalog = rawCatalog();
        final Pattern test = caseInsensitiveContains(input);
        Stream<StyleInfo> styles = catalog.getStylesByWorkspace(CatalogFacade.NO_WORKSPACE).stream();
        String workspace = getSelectedWorkspace();
        if (StringUtils.hasText(workspace)) {
            List<StyleInfo> stylesByWorkspace = catalog.getStylesByWorkspace(workspace);
            styles = Stream.concat(styles, stylesByWorkspace.stream());
        }
        return styles.parallel()
                .map(StyleInfo::prefixedName)
                .filter(name -> inputMatches(test, name))
                .sorted()
                .limit(MAX_SUGGESTIONS)
                .iterator();
    }

    private GrantType getAccess() {
        return getModelObject().getAccess();
    }

    public boolean isShowCatalogMode() {
        GrantType access = getAccess();
        switch (access) {
            case ALLOW:
                return isLayerSelected();
            case LIMIT:
                return true;
            default:
                return false;
        }
    }

    public boolean isShowRuleLimits() {
        return getAccess() == GrantType.LIMIT;
    }

    private boolean isLayerSelected() {
        String layer = getModelObject().getLayer();
        boolean hasLayer = StringUtils.hasText(layer);
        return hasLayer;
    }

    public Optional<PublishedInfoChangeEvent> workSpaceNameChanged(String workspace, AjaxRequestTarget target) {
        String layer = getModelObject().getLayer();
        return updatePublishedInfo(workspace, layer, target);
    }

    public Optional<PublishedInfoChangeEvent> layerNameChanged(String layer, AjaxRequestTarget target) {
        String workspace = getSelectedWorkspace();
        return updatePublishedInfo(workspace, layer, target);
    }

    private Optional<PublishedInfoChangeEvent> updatePublishedInfo(
            String workspace, String layer, AjaxRequestTarget target) {
        {
            String currWs = publishedInfoModel.getWorkspace();
            String currLayer = publishedInfoModel.getLayer();
            if (Objects.equals(currWs, workspace) && Objects.equals(currLayer, layer)) {
                return Optional.empty();
            }
        }
        PublishedInfo info = publishedInfoModel.setObject(workspace, layer);
        updateModelFor(info);
        return Optional.of(new PublishedInfoChangeEvent(workspace, layer, Optional.ofNullable(info), target));
    }

    private void updateModelFor(PublishedInfo info) {
        updateLayerType(info);
        updateLayerAttributes(info);
    }

    private void updateLayerType(PublishedInfo info) {
        MutableRule object = getModelObject();
        MutableLayerDetails ld = object.getLayerDetails();
        if (null != ld) {
            ld.setLayerTypeFrom(info);
        }
    }

    private void updateLayerAttributes(PublishedInfo info) {
        MutableRule object = getModelObject();
        MutableLayerDetails ld = object.getLayerDetails();
        if (null == ld) {
            return;
        }
        List<MutableLayerAttribute> origAtts = ld.getAttributes();
        if (!origAtts.isEmpty()) {
            return;
        }
        List<AttributeTypeInfo> attributes = List.of();
        if (info instanceof LayerInfo li) {
            if (li.getType() == PublishedType.VECTOR) {
                FeatureTypeInfo resource = (FeatureTypeInfo) li.getResource();
                try {
                    attributes = resource.attributes();
                } catch (IOException e) {
                    log.log(Level.WARNING, "Error getting layer attributes for " + info.prefixedName(), e);
                }
            }
        }
        List<MutableLayerAttribute> mapped =
                attributes.stream().map(MutableLayerAttribute::new).collect(Collectors.toList());
        ld.getAttributes().clear();
        ld.getAttributes().addAll(mapped);
    }

    private MutableRule getModelObject() {
        return getModel().getObject();
    }
}
