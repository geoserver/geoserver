/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * Original from GeoServer 2.24-SNAPSHOT under GPL 2.0 license (factored out from org.geoserver.geofence.server.web.GeofenceRulePage)
 */
package org.geoserver.acl.plugin.web.accessrules.layerdetails;

import com.google.common.collect.Streams;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.validation.INullAcceptingValidator;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.ValidationError;
import org.geolatte.geom.MultiPolygon;
import org.geoserver.acl.domain.rules.LayerDetails;
import org.geoserver.acl.domain.rules.LayerDetails.LayerType;
import org.geoserver.acl.domain.rules.RuleLimits;
import org.geoserver.acl.domain.rules.SpatialFilterType;
import org.geoserver.acl.plugin.web.accessrules.event.GrantTypeChangeEvent;
import org.geoserver.acl.plugin.web.accessrules.event.LayerChangeEvent;
import org.geoserver.acl.plugin.web.accessrules.event.PublishedInfoChangeEvent;
import org.geoserver.acl.plugin.web.accessrules.event.WorkspaceChangeEvent;
import org.geoserver.acl.plugin.web.accessrules.model.LayerAttributesEditModel;
import org.geoserver.acl.plugin.web.accessrules.model.LayerDetailsEditModel;
import org.geoserver.acl.plugin.web.accessrules.model.MutableLayerAttribute;
import org.geoserver.acl.plugin.web.accessrules.model.MutableLayerDetails;
import org.geoserver.acl.plugin.web.components.AllowedAreaEditPanel;
import org.geoserver.acl.plugin.web.components.ModelUpdatingAutoCompleteTextField;
import org.geoserver.acl.plugin.web.components.Select2SetMultiChoice;
import org.geoserver.acl.plugin.web.support.SerializableFunction;
import org.geoserver.catalog.PublishedInfo;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.util.logging.Logging;
import org.wicketstuff.select2.ChoiceProvider;
import org.wicketstuff.select2.Response;
import org.wicketstuff.select2.StringTextChoiceProvider;

/**
 * Form panel for a {@link MutableLayerDetails}.
 *
 * <p>
 *
 * <ul>
 *   <li>{@link MutableLayerDetails#getLayerType() layerType} (VECTOR, LAYER, LAYERGROUP) is fixed to the resolved value
 *       from the current instance layer/group.
 *   <li>If the {@link MutableLayerDetails#getLayerType() layerType} is {@link LayerType#RASTER RASTER}, the
 *       {@link MutableLayerDetails#getSpatialFilterType() spatialFilterType} is fixed to {@link SpatialFilterType#CLIP
 *       CLIP}.
 *   <li>If the {@link MutableLayerDetails#getLayerType() layerType} is {@link LayerType#LAYERGROUP LAYERGROUP}, then
 *       {@link MutableLayerDetails#getDefaultStyle DefaultStyle}, {@link MutableLayerDetails#getAllowedStyles
 *       AllowedStyles}, {@link MutableLayerDetails#getCqlFilterRead CqlFilterRead}, and
 *       {@link MutableLayerDetails#getCqlFilterWrite CqlFilterWrite} are not editable nor visible.
 *   <li>{@link MutableLayerDetails#getCatalogMode() CatalogMode} is not shown in this panel, it's assigned the value
 *       given at {@link DataAccessRuleEditPanel}, which pivots to {@link LayerDetails#getCatalogMode() LayerDetails}
 *       and {@link RuleLimits#getCatalogMode() RuleLimits} as appropriate.
 * </ul>
 *
 * @see LayerAttributesEditPanel
 * @see WorkspaceChangeEvent
 * @see LayerChangeEvent
 */
@SuppressWarnings("serial")
public class LayerDetailsEditPanel extends FormComponentPanel<MutableLayerDetails> {

    private static final Logger log = Logging.getLogger(LayerDetailsEditPanel.class);

    private WebMarkupContainer detailsContainer;

    private FormComponent<LayerType> layerType;

    /**
     * container for {@link #defaultStyle} and {@link #allowedStyles}, made invisible if the layer type is layergroup
     */
    private WebMarkupContainer stylesContainer;

    private FormComponent<String> defaultStyle;
    private FormComponent<Set<String>> allowedStyles;

    /**
     * container for{@link #area}, {@link #cqlFilterRead} and {@link #cqlFilterWrite}, makes cql filters invisible if
     * the layer type is not of type vector
     */
    private WebMarkupContainer filtersContainer;

    private LayerDetailsAreaEditPanel area;
    private FormComponent<String> cqlFilterRead;
    private FormComponent<String> cqlFilterWrite;

    private FormComponent<List<MutableLayerAttribute>> layerAttributes;

    private CompoundPropertyModel<MutableLayerDetails> componentModel;
    private LayerDetailsEditModel editModel;

    public LayerDetailsEditPanel(String id, LayerDetailsEditModel model) {
        super(id);
        this.editModel = model;
        setModel(componentModel = CompoundPropertyModel.of(editModel.getModel()));

        add(setLayerDetailsCheck());
        add(detailsContainer = detailsContainer());

        detailsContainer.add(layerType = layerType());
        detailsContainer.add(stylesContainer = stylesContainer());
        detailsContainer.add(filtersContainer = filtersContainer());

        // detailsContainer.add(area = new LayerDetailsAreaEditPanel("allowedArea",
        // componentModel));
        detailsContainer.add(layerAttributes = layerAttributes());

        setVisible(editModel.isShowPanel());
    }

    public @Override void convertInput() {
        final boolean setDetails = editModel.getSetLayerDetailsModel().getObject();
        if (!setDetails) {
            setConvertedInput(null);
            return;
        }

        MutableLayerDetails ld = getModelObject();
        ld.setDefaultStyle(defaultStyle.getConvertedInput());
        ld.setAllowedStyles(allowedStyles.getConvertedInput());
        // ld.setArea(area.getare);
        ld.setAttributes(layerAttributes.getConvertedInput());
        ld.setCqlFilterRead(cqlFilterRead.getConvertedInput());
        ld.setCqlFilterWrite(cqlFilterWrite.getConvertedInput());
        setConvertedInput(ld);
    }

    public @Override void onEvent(IEvent<?> event) {
        Object payload = event.getPayload();
        if (payload instanceof GrantTypeChangeEvent changeEvent2) {
            onGrantTypeChangeEvent(changeEvent2);
        } else if (payload instanceof WorkspaceChangeEvent) {
            onWorkspaceChangeEvent((WorkspaceChangeEvent) event.getPayload());
        } else if (payload instanceof LayerChangeEvent changeEvent1) {
            onLayerChangeEvent(changeEvent1);
        } else if (payload instanceof PublishedInfoChangeEvent changeEvent) {
            onPublishedInfoChangeEvent(changeEvent);
        }
    }

    void onGrantTypeChangeEvent(GrantTypeChangeEvent event) {
        handleVisibility(event.getTarget());
    }

    void onWorkspaceChangeEvent(WorkspaceChangeEvent event) {
        handleVisibility(event.getTarget());
    }

    void onLayerChangeEvent(LayerChangeEvent event) {
        handleVisibility(event.getTarget());
    }

    private void onPublishedInfoChangeEvent(PublishedInfoChangeEvent event) {
        log.info("layerTypeLabel: " + layerType.getDefaultModelObject());
        event.getTarget().add(layerType);
        event.getTarget().add(layerAttributes);
    }

    private void handleVisibility(AjaxRequestTarget target) {
        boolean showPanel = editModel.isShowPanel();
        if (this.isVisible() != showPanel) {
            setVisible(showPanel);
            target.add(this);
        }
        boolean showDetails = editModel.isShowLayerDetails();
        if (detailsContainer.isVisible() != showDetails) {
            detailsContainer.setVisible(showDetails);
            target.add(detailsContainer);
        }
    }

    private FormComponent<Boolean> setLayerDetailsCheck() {
        CheckBox check = new CheckBox("setLayerDetails", editModel.getSetLayerDetailsModel());
        check.setOutputMarkupId(true);
        check.add(new OnChangeAjaxBehavior() {
            protected @Override void onUpdate(AjaxRequestTarget target) {
                // model updated, then...
                handleVisibility(target);
            }
        });
        return check;
    }

    private WebMarkupContainer detailsContainer() {
        WebMarkupContainer detailsContainer = new WebMarkupContainer("detailsContainer");
        detailsContainer.setOutputMarkupPlaceholderTag(true);
        detailsContainer.setVisible(editModel.isShowLayerDetails());
        return detailsContainer;
    }

    private WebMarkupContainer stylesContainer() {
        WebMarkupContainer container = new WebMarkupContainer("styles") {
            public @Override void onEvent(IEvent<?> event) {
                if (event.getPayload() instanceof PublishedInfoChangeEvent) {
                    PublishedInfoChangeEvent e = (PublishedInfoChangeEvent) event.getPayload();
                    boolean visible = editModel.canHaveStyles();
                    if (visible != stylesContainer.isVisible()) {
                        stylesContainer.setVisible(visible);
                        e.getTarget().add(stylesContainer);
                    }
                }
            }
        };
        container.setOutputMarkupPlaceholderTag(true);
        container.add(defaultStyle = defaultStyle());
        container.add(allowedStyles = allowedStyles());
        container.setVisible(editModel.canHaveStyles());
        return container;
    }

    RadioGroup<String> filtertabset;
    Radio<String> wktareaTab;
    Radio<String> cqlreadTab;
    Radio<String> cqlwriteTab;

    private WebMarkupContainer filtersContainer() {
        WebMarkupContainer container = new WebMarkupContainer("filterTabs") {
            public @Override void onEvent(IEvent<?> event) {
                if (event.getPayload() instanceof PublishedInfoChangeEvent) {
                    updateFilterTabs((PublishedInfoChangeEvent) event.getPayload());
                }
            }
        };
        container.setOutputMarkupPlaceholderTag(true);

        filtertabset = new RadioGroup<>("filtertabset", Model.of("area"));
        filtertabset.setOutputMarkupPlaceholderTag(true);
        wktareaTab = new Radio<>("wktareaTab", Model.of("area"), filtertabset);
        cqlreadTab = new Radio<>("cqlreadTab", Model.of("cqlr"), filtertabset);
        cqlwriteTab = new Radio<>("cqlwriteTab", Model.of("cqlw"), filtertabset);
        filtertabset.add(wktareaTab, cqlreadTab, cqlwriteTab);

        IModel<String> readModel = componentModel.bind("cqlFilterRead");
        IModel<String> writeModel = componentModel.bind("cqlFilterWrite");
        cqlFilterRead = new CQLFilterTextArea("cqlFilterRead", readModel);
        cqlFilterWrite = new CQLFilterTextArea("cqlFilterWrite", writeModel);

        area = new LayerDetailsAreaEditPanel("allowedArea", componentModel);

        container.add(filtertabset, area, cqlFilterRead, cqlFilterWrite);
        boolean supportsCQL = editModel.canHaveCqLFilters();
        cqlreadTab.setEnabled(supportsCQL); // the css will hide it if disabled
        cqlwriteTab.setEnabled(supportsCQL); // the css will hide it if disabled
        return container;
    }

    private void updateFilterTabs(PublishedInfoChangeEvent e) {
        boolean supportsCQL = editModel.canHaveCqLFilters();
        cqlreadTab.setEnabled(supportsCQL);
        cqlwriteTab.setEnabled(supportsCQL);
        if (!supportsCQL) {
            filtertabset.getModel().setObject(wktareaTab.getModelObject());
        }
        e.getTarget().add(filtersContainer);
    }

    private FormComponent<String> defaultStyle() {
        IModel<String> model = componentModel.bind("defaultStyle");
        return autoCompleteChoice("defaultStyle", model, editModel::getStyleChoices);
    }

    private AutoCompleteTextField<String> autoCompleteChoice(
            String id, IModel<String> model, SerializableFunction<String, Iterator<String>> choiceResolver) {

        AutoCompleteTextField<String> field;
        field = new ModelUpdatingAutoCompleteTextField<>(id, model, choiceResolver);
        field.setOutputMarkupId(true);
        field.setConvertEmptyInputStringToNull(true);
        return field;
    }

    private FormComponent<Set<String>> allowedStyles() {
        IModel<Set<String>> model = componentModel.bind("allowedStyles");
        ChoiceProvider<String> choiceProvider = new AllowedStylesChoiceProvider();
        Select2SetMultiChoice<String> allowedStyles;
        allowedStyles = new Select2SetMultiChoice<>("allowedStyles", model, choiceProvider);
        allowedStyles.setOutputMarkupPlaceholderTag(true);
        return allowedStyles;
    }

    private class AllowedStylesChoiceProvider extends StringTextChoiceProvider {
        public @Override void query(String term, int page, Response<String> response) {
            Streams.stream(editModel.getStyleChoices(term)).forEach(response::add);
        }
    }

    private FormComponent<LayerType> layerType() {
        IModel<LayerType> model = componentModel.bind("layerType");
        IModel<PublishedInfo> publishedInfoModel = editModel.getPublishedInfoModel();
        return new LayerTypeFormComponent("layerType", model, publishedInfoModel);
    }

    private FormComponent<List<MutableLayerAttribute>> layerAttributes() {
        LayerAttributesEditModel model = editModel.layerAttributes();
        LayerAttributesEditPanel panel = new LayerAttributesEditPanel("layerAttributes", model);
        panel.setOutputMarkupPlaceholderTag(true);
        return panel;
    }

    private static class CQLFilterTextArea extends TextArea<String> {
        public CQLFilterTextArea(String id, IModel<String> model) {
            super(id, model);
            setRequired(false);
            setOutputMarkupId(true);
            setOutputMarkupPlaceholderTag(true);
            add(new CQLFilterValidator());
        }
    }

    private static final class CQLFilterValidator implements INullAcceptingValidator<String> {
        @Override
        public void validate(IValidatable<String> validatable) {
            String value = validatable.getValue();
            if (value != null) {
                try {
                    ECQL.toFilter(value);
                } catch (CQLException e) {
                    ValidationError error = new ValidationError(this);
                    error.setVariable("error", e.getMessage());
                    validatable.error(error);
                }
            }
        }
    }

    class LayerDetailsAreaEditPanel extends AllowedAreaEditPanel<MutableLayerDetails> {

        public LayerDetailsAreaEditPanel(String id, IModel<MutableLayerDetails> model) {
            super(id, model, "area", "spatialFilterType");
            super.intersect.setEnabled(editModel.getLayerType() != LayerType.RASTER);
        }

        public @Override void onEvent(IEvent<?> event) {
            if (event.getPayload() instanceof PublishedInfoChangeEvent) {
                PublishedInfoChangeEvent e = ((PublishedInfoChangeEvent) event.getPayload());
                super.intersect.setEnabled(editModel.getLayerType() != LayerType.RASTER);
                e.getTarget().add(this);
            }
        }

        @Override
        public void convertInput() {
            // super.convertInput();
            MultiPolygon<?> area = getAllowedAreaConvertedInput();
            SpatialFilterType type = getSpatialFilterTypeConvertedInput();
            MutableLayerDetails modelObject = getModelObject();
            modelObject.setArea(area);
            modelObject.setSpatialFilterType(type);
            setConvertedInput(modelObject);
        }
    }
}
