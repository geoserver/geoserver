/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.resource;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.beanutils.BeanToPropertyValueTransformer;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.data.layer.CascadedWFSStoredQueryEditPage;
import org.geoserver.web.data.layer.SQLViewEditPage;
import org.geoserver.web.wicket.GSModalWindow;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.data.wfs.internal.v2_0.storedquery.StoredQueryConfiguration;
import org.geotools.filter.FilterAttributeExtractor;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.jdbc.VirtualTable;
import org.geotools.measure.Measure;
import org.geotools.util.logging.Logging;

@SuppressWarnings("serial")
public class FeatureResourceConfigurationPanel extends ResourceConfigurationPanel {
    static final Logger LOGGER = Logging.getLogger(FeatureResourceConfigurationPanel.class);

    GSModalWindow reloadWarningDialog;

    ListView<AttributeTypeInfo> attributes;

    private WebMarkupContainer attributePanel;

    AttributeTypeInfoEditor attributesEditor;

    boolean customizeFeatureType;

    public FeatureResourceConfigurationPanel(String id, final IModel model) {
        super(id, model);

        CheckBox circularArcs = new CheckBox("circularArcPresent");
        add(circularArcs);

        TextField<Measure> tolerance = new TextField<>("linearizationTolerance", Measure.class);
        add(tolerance);

        attributePanel = new WebMarkupContainer("attributePanel");
        attributePanel.setOutputMarkupId(true);
        add(attributePanel);

        // We need to use the resourcePool directly because we're playing with an edited
        // FeatureTypeInfo and the info.getFeatureType() and info.getAttributes() will hit
        // the resource pool without the modified properties (since it passes "this" into calls
        // to the ResourcePool

        List<AttributeTypeInfo> attributes = ((FeatureTypeInfo) model.getObject()).getAttributes();
        this.customizeFeatureType = attributes != null && !attributes.isEmpty();
        Component nativeAttributePanel = getNativeAttributesTable("attributesTable", model);
        attributePanel.add(nativeAttributePanel);
        nativeAttributePanel.setVisible(!customizeFeatureType);
        attributesEditor = new AttributeTypeInfoEditor("attributesEditor", model, attributePanel);
        attributePanel.add(attributesEditor);
        attributesEditor.setVisible(customizeFeatureType);

        CheckBox customizeCheck =
                new CheckBox("customizeFeatureType", new PropertyModel<>(this, "customizeFeatureType"));
        customizeCheck.add(new AjaxFormComponentUpdatingBehavior("click") {
            @Override
            protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {
                customizeCheck.processInput();
                nativeAttributePanel.setVisible(!customizeFeatureType);
                attributesEditor.setVisible(customizeFeatureType);
                ajaxRequestTarget.add(attributePanel);
            }
        });
        attributePanel.add(customizeCheck);

        TextArea<String> cqlFilter = new TextArea<>("cqlFilter");
        cqlFilter.add(new CqlFilterValidator(model));
        add(cqlFilter);

        // reload links
        WebMarkupContainer reloadContainer = new WebMarkupContainer("reloadContainer");
        attributePanel.add(reloadContainer);
        GeoServerAjaxFormLink reload = new GeoServerAjaxFormLink("reload") {
            @Override
            protected void onClick(AjaxRequestTarget target, Form form) {
                GeoServerApplication app = (GeoServerApplication) getApplication();

                FeatureTypeInfo ft = (FeatureTypeInfo) getResourceInfo();
                app.getCatalog().getResourcePool().clear(ft);
                app.getCatalog().getResourcePool().clear(ft.getStore());
                target.add(attributePanel);
            }
        };
        reloadContainer.add(reload);

        GeoServerAjaxFormLink warning = new GeoServerAjaxFormLink("reloadWarning") {
            @Override
            protected void onClick(AjaxRequestTarget target, Form form) {
                reloadWarningDialog.show(target);
            }
        };
        reloadContainer.add(warning);

        add(reloadWarningDialog = new GSModalWindow("reloadWarningDialog"));
        reloadWarningDialog.setContent(new ReloadWarningDialog(
                reloadWarningDialog.getContentId(), new StringResourceModel("featureTypeReloadWarning", this, null)));
        reloadWarningDialog.setTitle(new StringResourceModel("warning", null, null));
        reloadWarningDialog.setInitialHeight(100);

        // sql view handling
        WebMarkupContainer sqlViewContainer = new WebMarkupContainer("editSqlContainer");
        attributePanel.add(sqlViewContainer);
        sqlViewContainer.add(new Link<>("editSql") {

            @Override
            public void onClick() {
                FeatureTypeInfo typeInfo = (FeatureTypeInfo) model.getObject();
                try {
                    setResponsePage(new SQLViewEditPage(typeInfo, ((ResourceConfigurationPage) this.getPage())));
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Failure opening the sql view edit page", e);
                    error(e.toString());
                }
            }
        });

        // which one do we show, reload or edit?
        FeatureTypeInfo typeInfo = (FeatureTypeInfo) model.getObject();
        reloadContainer.setVisible(
                typeInfo.getMetadata().get(FeatureTypeInfo.JDBC_VIRTUAL_TABLE, VirtualTable.class) == null);
        sqlViewContainer.setVisible(!reloadContainer.isVisible());

        // Cascaded Stored Query
        WebMarkupContainer cascadedStoredQueryContainer = new WebMarkupContainer("editCascadedStoredQueryContainer");
        attributePanel.add(cascadedStoredQueryContainer);
        cascadedStoredQueryContainer.add(new Link<>("editCascadedStoredQuery") {
            @Override
            public void onClick() {
                FeatureTypeInfo typeInfo = (FeatureTypeInfo) model.getObject();
                try {
                    setResponsePage(
                            new CascadedWFSStoredQueryEditPage(typeInfo, ((ResourceConfigurationPage) this.getPage())));
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Failure opening the sql view edit page", e);
                    error(e.toString());
                }
            }
        });
        cascadedStoredQueryContainer.setVisible(
                typeInfo.getMetadata().get(FeatureTypeInfo.STORED_QUERY_CONFIGURATION, StoredQueryConfiguration.class)
                        != null);
    }

    private Component getNativeAttributesTable(String id, IModel model) {
        Fragment fragment = new Fragment(id, "attributePanelFragment", this);

        // just use the direct attributes, this is not editable atm
        attributes = new ListView<>("attributes", new AttributeListModel()) {
            @Override
            protected void populateItem(ListItem item) {

                // odd/even style
                item.add(AttributeModifier.replace("class", item.getIndex() % 2 == 0 ? "even" : "odd"));

                // dump the attribute information we have
                AttributeTypeInfo attribute = (AttributeTypeInfo) item.getModelObject();
                item.add(new Label("name", attribute.getName()));
                item.add(new Label("minmax", attribute.getMinOccurs() + "/" + attribute.getMaxOccurs()));
                try {
                    // working around a serialization issue
                    FeatureTypeInfo typeInfo = (FeatureTypeInfo) model.getObject();
                    final ResourcePool resourcePool =
                            GeoServerApplication.get().getCatalog().getResourcePool();
                    final FeatureType featureType = resourcePool.getFeatureType(typeInfo);
                    org.geotools.api.feature.type.PropertyDescriptor pd =
                            featureType.getDescriptor(attribute.getName());
                    String typeName = "?";
                    String nillable = "?";
                    try {
                        typeName = pd.getType().getBinding().getSimpleName();
                        nillable = String.valueOf(pd.isNillable());
                    } catch (Exception e) {
                        LOGGER.log(
                                Level.INFO,
                                "Could not find attribute " + attribute.getName() + " in feature type " + featureType,
                                e);
                    }
                    item.add(new Label("type", typeName));
                    item.add(new Label("nillable", nillable));
                } catch (IOException e) {
                    item.add(new Label("type", "?"));
                    item.add(new Label("nillable", "?"));
                }
            }
        };
        fragment.add(attributes);
        return fragment;
    }

    @Override
    public void resourceUpdated(AjaxRequestTarget target) {
        if (target != null) {
            // force it to reload the attribute list
            attributes.getModel().detach();
            target.add(attributePanel);
        }
    }

    static class ReloadWarningDialog extends Panel {
        public ReloadWarningDialog(String id, StringResourceModel message) {
            super(id);
            add(new Label("message", message));
        }
    }

    /*
     * Wicket validator to check CQL filter string
     */
    private class CqlFilterValidator implements IValidator<String> {

        private FeatureTypeInfo typeInfo;

        public CqlFilterValidator(IModel model) {
            this.typeInfo = (FeatureTypeInfo) model.getObject();
        }

        @Override
        public void validate(IValidatable<String> validatable) {
            try {
                validateCqlFilter(typeInfo, validatable.getValue());
            } catch (Exception e) {
                ValidationError error = new ValidationError();
                error.setMessage(e.getMessage());
                validatable.error(error);
            }
        }
    }

    /*
     * Validate that CQL filter syntax is valid, and attribute names used in the CQL filter are actually part of the layer
     */
    private void validateCqlFilter(FeatureTypeInfo typeInfo, String cqlFilterString) throws Exception {
        Filter cqlFilter = null;
        if (cqlFilterString != null && !cqlFilterString.isEmpty()) {
            cqlFilter = ECQL.toFilter(cqlFilterString);
            FeatureType ft = typeInfo.getFeatureType();
            if (ft instanceof SimpleFeatureType sft) {
                BeanToPropertyValueTransformer transformer = new BeanToPropertyValueTransformer("localName");
                Collection<String> featureAttributesNames = CollectionUtils.collect(
                        sft.getAttributeDescriptors(), ad -> (String) transformer.transform(ad));

                FilterAttributeExtractor filterAttriubtes = new FilterAttributeExtractor(null);
                cqlFilter.accept(filterAttriubtes, null);
                Set<String> filterAttributesNames = filterAttriubtes.getAttributeNameSet();
                for (String filterAttributeName : filterAttributesNames) {
                    if (!featureAttributesNames.contains(filterAttributeName)) {
                        throw new ResourceConfigurationException(
                                ResourceConfigurationException.CQL_ATTRIBUTE_NAME_NOT_FOUND_$1,
                                new Object[] {filterAttributeName});
                    }
                }
            }
        }
    }

    @Override
    public void onSave() {
        FeatureTypeInfo fti = (FeatureTypeInfo) getDefaultModelObject();
        if (customizeFeatureType) {
            fti.getAttributes().clear();
            fti.getAttributes().addAll(attributesEditor.getItems());
        } else {
            fti.getAttributes().clear();
        }
    }

    class AttributeListModel extends LoadableDetachableModel<List<AttributeTypeInfo>> {

        @Override
        protected List<AttributeTypeInfo> load() {
            return AttributeTypeInfoEditor.loadNativeAttributes(
                    (FeatureTypeInfo) getDefaultModelObject(), FeatureResourceConfigurationPanel.this);
        }
    }
}
