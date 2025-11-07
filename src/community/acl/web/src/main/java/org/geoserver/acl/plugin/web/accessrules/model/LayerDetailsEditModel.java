/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.web.accessrules.model;

import static java.util.Objects.nonNull;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import lombok.Getter;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.acl.domain.rules.GrantType;
import org.geoserver.acl.domain.rules.LayerDetails.LayerType;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.PublishedType;
import org.springframework.util.StringUtils;

@SuppressWarnings("serial")
public class LayerDetailsEditModel implements Serializable {

    private final IModel<Boolean> setLayerDetails;

    private final @Getter CompoundPropertyModel<MutableLayerDetails> model;

    private final @Getter CompoundPropertyModel<MutableRule> rule;

    private DataAccessRuleEditModel ruleEditModel;

    public LayerDetailsEditModel(DataAccessRuleEditModel ruleEditModel) {
        this.ruleEditModel = ruleEditModel;
        this.rule = ruleEditModel.getModel();
        model = CompoundPropertyModel.of(rule.bind("layerDetails"));

        boolean isNew = model.getObject().isNew();
        boolean initVisible = !isNew && isShowPanel() && hasDisplayablePropertiesSet();
        this.setLayerDetails = Model.of(initVisible);
    }

    public IModel<PublishedInfo> getPublishedInfoModel() {
        return ruleEditModel.getPublishedInfoModel();
    }

    public IModel<Boolean> getSetLayerDetailsModel() {
        return setLayerDetails;
    }

    public LayerAttributesEditModel layerAttributes() {
        IModel<List<MutableLayerAttribute>> attsModel = model.bind("attributes");
        return new LayerAttributesEditModel(this, attsModel);
    }

    public boolean isShowPanel() {
        return rule.getObject().canHaveLayerDetails();
    }

    public boolean isShowLayerDetails() {
        boolean checked = setLayerDetails.getObject().booleanValue();
        return isShowPanel() && checked;
    }

    public boolean isLayerSelected() {
        String layer = rule.getObject().getLayer();
        boolean hasLayer = StringUtils.hasText(layer);
        return hasLayer;
    }

    public GrantType getAccess() {
        return rule.getObject().getAccess();
    }

    public boolean hasDisplayablePropertiesSet() {
        MutableLayerDetails ld = getModel().getObject();
        return ld != null
                && (nonNull(ld.getDefaultStyle())
                        || nonNull(ld.getCqlFilterRead())
                        || nonNull(ld.getCqlFilterWrite())
                        || nonNull(ld.getArea())
                        // || (nonNull(catalogMode) && catalogMode == HIDE)
                        // || (nonNull(spatialFilterType) && spatialFilterType == INTERSECT)
                        || (nonNull(ld.getAllowedStyles())
                                && !ld.getAllowedStyles().isEmpty())
                        || (nonNull(ld.getAttributes()) && !ld.getAttributes().isEmpty()));
    }

    public Iterator<String> getStyleChoices(String input) {
        return ruleEditModel.getStyleChoices(input);
    }

    public boolean canHaveStyles() {
        LayerType ltype = getLayerType();
        if (ltype == null) {
            PublishedInfo info = ruleEditModel.getPublishedInfoModel().getObject();
            return info != null && (info.getType() != PublishedType.GROUP);
        }
        return ltype == LayerType.RASTER || ltype == LayerType.VECTOR;
    }

    public boolean canHaveCqLFilters() {
        LayerType ltype = getLayerType();
        if (ltype == null) {
            PublishedInfo info = ruleEditModel.getPublishedInfoModel().getObject();
            return info != null && (info.getType() == PublishedType.VECTOR);
        }
        return ltype == LayerType.VECTOR;
    }

    public LayerType getLayerType() {
        MutableLayerDetails layerDetails = getModel().getObject();
        return null == layerDetails ? null : layerDetails.getLayerType();
    }
}
