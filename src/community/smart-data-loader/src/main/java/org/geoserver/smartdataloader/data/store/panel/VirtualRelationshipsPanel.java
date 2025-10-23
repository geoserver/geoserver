/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.data.store.panel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.validation.validator.StringValidator;
import org.geoserver.smartdataloader.data.SmartDataLoaderDataAccessFactory;
import org.geoserver.smartdataloader.data.store.virtualfk.EntityRef;
import org.geoserver.smartdataloader.data.store.virtualfk.Key;
import org.geoserver.smartdataloader.data.store.virtualfk.Relationship;
import org.geoserver.smartdataloader.data.store.virtualfk.Relationships;
import org.geoserver.smartdataloader.data.store.virtualfk.RelationshipsXmlParser;
import org.geoserver.smartdataloader.data.store.virtualfk.RelationshipsXmlWriter;
import org.geoserver.web.wicket.GSModalWindow;
import org.geotools.util.logging.Logging;

/**
 * Panel that allows viewing and editing the virtual foreign key relationships configured for the Smart Data Loader
 * store.
 */
public class VirtualRelationshipsPanel extends Panel {

    private static final Logger LOGGER = Logging.getLogger(VirtualRelationshipsPanel.class);

    private static final List<String> CARDINALITIES = Arrays.asList("1:1", "1:n", "n:1", "n:n");

    private static final List<String> ENTITY_KINDS = Arrays.asList("table", "view");

    private final IModel<Map<String, Serializable>> connectionParametersModel;

    private final LoadableDetachableModel<List<VirtualRelationshipBean>> relationshipsModel;

    private WebMarkupContainer tableContainer;
    private WebMarkupContainer emptyContainer;
    private ListView<VirtualRelationshipBean> relationshipsView;
    private FeedbackPanel feedback;
    private GSModalWindow relationshipModal;

    public VirtualRelationshipsPanel(String id, IModel<Map<String, Serializable>> connectionParametersModel) {
        super(id);
        this.connectionParametersModel = connectionParametersModel;
        setOutputMarkupId(true);
        this.relationshipsModel = new LoadableDetachableModel<List<VirtualRelationshipBean>>() {
            @Override
            protected List<VirtualRelationshipBean> load() {
                return loadRelationships();
            }
        };
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        feedback = new FeedbackPanel("feedback");
        feedback.setOutputMarkupId(true);
        add(feedback);

        WebMarkupContainer toolbar = buildToolbar();
        add(toolbar);

        tableContainer = new WebMarkupContainer("relationshipsTable");
        tableContainer.setOutputMarkupId(true);
        tableContainer.setOutputMarkupPlaceholderTag(true);
        relationshipsView = new ListView<VirtualRelationshipBean>("relationships", relationshipsModel) {
            @Override
            protected void populateItem(ListItem<VirtualRelationshipBean> item) {
                VirtualRelationshipBean bean = item.getModelObject();
                item.add(new Label("name", bean.getName()));
                item.add(new Label("cardinality", displayCardinality(bean.getCardinality())));
                item.add(new Label("source", bean.getSourceSummary()));
                item.add(new Label("target", bean.getTargetSummary()));
                item.add(createEditLink("edit", item));
                item.add(createRemoveLink("remove", item));
            }
        };
        relationshipsView.setOutputMarkupId(true);
        tableContainer.add(relationshipsView);
        add(tableContainer);

        emptyContainer = new WebMarkupContainer("emptyContainer");
        emptyContainer.setOutputMarkupPlaceholderTag(true);
        add(emptyContainer);

        relationshipModal = new GSModalWindow("relationshipModal");
        relationshipModal.setOutputMarkupId(true);
        relationshipModal.setContent(new EmptyPanel(relationshipModal.getContentId()));
        relationshipModal.setWindowClosedCallback(target -> {
            relationshipModal.setContent(new EmptyPanel(relationshipModal.getContentId()));
            if (target != null) {
                target.add(tableContainer, emptyContainer, feedback);
                relationshipModal.close(target);
            }
        });
        add(relationshipModal);

        updateVisibility();
    }

    private WebMarkupContainer buildToolbar() {
        WebMarkupContainer toolbar = new WebMarkupContainer("toolbar");
        toolbar.setOutputMarkupId(true);
        AjaxLink<Void> addLink = new AjaxLink<Void>("addLink") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                openForAdd(target);
            }
        };
        toolbar.add(addLink);
        return toolbar;
    }

    private AjaxLink<Void> createEditLink(String id, ListItem<VirtualRelationshipBean> item) {
        return new AjaxLink<Void>(id) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                openForEdit(target, item.getIndex());
            }
        };
    }

    private AjaxLink<Void> createRemoveLink(String id, ListItem<VirtualRelationshipBean> item) {
        return new AjaxLink<Void>(id) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                removeRelationship(item.getIndex());
                target.add(tableContainer, emptyContainer, feedback);
            }
        };
    }

    private void openForAdd(AjaxRequestTarget target) {
        VirtualRelationshipBean bean = createDefaultBean();
        openModal(target, bean, -1, false);
    }

    private void openForEdit(AjaxRequestTarget target, int index) {
        List<VirtualRelationshipBean> current = relationshipsModel.getObject();
        if (current == null || index < 0 || index >= current.size()) {
            return;
        }
        VirtualRelationshipBean bean = current.get(index).copy();
        openModal(target, bean, index, true);
    }

    private void openModal(AjaxRequestTarget target, VirtualRelationshipBean bean, int index, boolean editMode) {
        relationshipModal.setTitle(Model.of(editMode ? getEditLegend() : getAddLegend()));
        RelationshipFormPanel panel =
                new RelationshipFormPanel(relationshipModal.getContentId(), bean, index, editMode);
        relationshipModal.setContent(panel);
        relationshipModal.show(target);
    }

    private void applyRelationship(VirtualRelationshipBean bean, int index) {
        List<VirtualRelationshipBean> updated = new ArrayList<>();
        List<VirtualRelationshipBean> current = relationshipsModel.getObject();
        if (current != null) {
            updated.addAll(current);
        }
        if (index >= 0 && index < updated.size()) {
            updated.set(index, bean);
        } else {
            updated.add(bean);
        }
        persistRelationships(updated);
        relationshipsModel.setObject(null);
        relationshipsModel.detach();
        List<VirtualRelationshipBean> refreshed = relationshipsModel.getObject();
        relationshipsView.removeAll();
        relationshipsView.modelChanged();
        updateVisibility(refreshed);
    }

    private void updateVisibility() {
        List<VirtualRelationshipBean> current = relationshipsModel.getObject();
        updateVisibility(current);
    }

    private void updateVisibility(List<VirtualRelationshipBean> relationships) {
        boolean hasRelationships = relationships != null && !relationships.isEmpty();
        tableContainer.setVisible(hasRelationships);
        emptyContainer.setVisible(!hasRelationships);
    }

    private List<VirtualRelationshipBean> loadRelationships() {
        List<VirtualRelationshipBean> beans = new ArrayList<>();
        Map<String, Serializable> parameters = connectionParametersModel.getObject();
        if (parameters == null) {
            return beans;
        }
        Serializable value = parameters.get(SmartDataLoaderDataAccessFactory.VIRTUAL_RELATIONSHIPS.key);
        if (value instanceof String) {
            String xml = ((String) value).trim();
            if (!xml.isEmpty()) {
                try {
                    Relationships rels = RelationshipsXmlParser.parse(xml);
                    for (Relationship relationship : rels.getRelationships()) {
                        beans.add(VirtualRelationshipBean.from(relationship));
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to parse virtual relationships configuration", e);
                    error(getString(
                            "VirtualRelationshipsPanel.parseError",
                            null,
                            "The configured virtual relationships could not be loaded."));
                }
            }
        }
        return beans;
    }

    private void removeRelationship(int index) {
        List<VirtualRelationshipBean> updated = new ArrayList<>();
        List<VirtualRelationshipBean> current = relationshipsModel.getObject();
        if (current != null) {
            updated.addAll(current);
        }
        if (index < 0 || index >= updated.size()) {
            return;
        }
        updated.remove(index);
        persistRelationships(updated);
        relationshipsModel.setObject(null);
        relationshipsModel.detach();
        List<VirtualRelationshipBean> refreshed = relationshipsModel.getObject();
        relationshipsView.removeAll();
        relationshipsView.modelChanged();
        updateVisibility(refreshed);
    }

    private void persistRelationships(List<VirtualRelationshipBean> relationships) {
        Relationships rels = new Relationships();
        if (relationships != null) {
            for (VirtualRelationshipBean bean : relationships) {
                rels.addRelationship(bean.toRelationship());
            }
        }
        String xml = RelationshipsXmlWriter.toXml(rels);
        Map<String, Serializable> parameters = connectionParametersModel.getObject();
        if (parameters != null) {
            if (xml == null) {
                parameters.remove(SmartDataLoaderDataAccessFactory.VIRTUAL_RELATIONSHIPS.key);
            } else {
                parameters.put(SmartDataLoaderDataAccessFactory.VIRTUAL_RELATIONSHIPS.key, xml);
            }
        }
    }

    private VirtualRelationshipBean createDefaultBean() {
        VirtualRelationshipBean bean = new VirtualRelationshipBean();
        bean.setSourceKind("table");
        bean.setTargetKind("table");
        bean.setCardinality("1:n");
        return bean;
    }

    private String displayCardinality(String value) {
        if ("1:n".equalsIgnoreCase(value)) {
            return "1->n";
        }
        if ("n:1".equalsIgnoreCase(value)) {
            return "n->1";
        }
        if ("1:1".equalsIgnoreCase(value)) {
            return "1<->1";
        }
        if ("n:n".equalsIgnoreCase(value)) {
            return "n<->n";
        }
        return value;
    }

    private String getAddLegend() {
        return getString("VirtualRelationshipsPanel.addLegend", null, "Add virtual relationship");
    }

    private String getEditLegend() {
        return getString("VirtualRelationshipsPanel.editLegend", null, "Edit virtual relationship");
    }

    private class RelationshipFormPanel extends Panel {

        private final CompoundPropertyModel<VirtualRelationshipBean> formModel;
        private final FeedbackPanel modalFeedback;
        private final int relationshipIndex;

        RelationshipFormPanel(String id, VirtualRelationshipBean bean, int relationshipIndex, boolean editMode) {
            super(id);
            setOutputMarkupId(true);
            this.relationshipIndex = relationshipIndex;
            this.formModel = new CompoundPropertyModel<>(bean);

            Form<VirtualRelationshipBean> form = new Form<>("form", formModel);
            form.setOutputMarkupId(true);
            add(form);

            form.add(new Label("legend", Model.of(editMode ? getEditLegend() : getAddLegend())));

            modalFeedback = new FeedbackPanel("feedback");
            modalFeedback.setOutputMarkupId(true);
            form.add(modalFeedback);

            form.add(createTextField("name"));
            form.add(createTextField("sourceSchema"));
            form.add(createTextField("sourceEntity"));
            form.add(createKindChoice("sourceKind"));
            form.add(createTextField("sourceColumn"));
            form.add(createTextField("targetSchema"));
            form.add(createTextField("targetEntity"));
            form.add(createKindChoice("targetKind"));
            form.add(createTextField("targetColumn"));
            form.add(createCardinalityChoice("cardinality"));

            AjaxButton save = new AjaxButton("save", form) {
                @Override
                protected void onSubmit(AjaxRequestTarget target) {
                    VirtualRelationshipBean bean = formModel.getObject().copy();
                    bean.normalize();
                    if (!bean.isValid()) {
                        error(getString(
                                "VirtualRelationshipsPanel.validationError", null, "All fields must be provided."));
                        target.add(modalFeedback);
                        return;
                    }
                    applyRelationship(bean, relationshipIndex);
                    relationshipModal.close(target);
                    target.add(tableContainer, emptyContainer, feedback);
                }

                @Override
                protected void onError(AjaxRequestTarget target) {
                    target.add(modalFeedback);
                }
            };
            form.add(save);

            AjaxButton cancel = new AjaxButton("cancel") {
                @Override
                protected void onSubmit(AjaxRequestTarget target) {
                    relationshipModal.close(target);
                }
            };
            cancel.setDefaultFormProcessing(false);
            form.add(cancel);
        }

        private TextField<String> createTextField(String id) {
            TextField<String> field = new TextField<>(id);
            field.setRequired(true);
            field.add(StringValidator.maximumLength(256));
            return field;
        }

        private DropDownChoice<String> createKindChoice(String id) {
            DropDownChoice<String> choice = new DropDownChoice<>(id, ENTITY_KINDS);
            choice.setNullValid(false);
            choice.setRequired(true);
            return choice;
        }

        private DropDownChoice<String> createCardinalityChoice(String id) {
            DropDownChoice<String> choice = new DropDownChoice<>(id, CARDINALITIES);
            choice.setNullValid(false);
            choice.setRequired(true);
            return choice;
        }
    }

    private static final class VirtualRelationshipBean implements Serializable {

        private String name;
        private String cardinality;
        private String sourceSchema;
        private String sourceEntity;
        private String sourceKind;
        private String sourceColumn;
        private String targetSchema;
        private String targetEntity;
        private String targetKind;
        private String targetColumn;

        static VirtualRelationshipBean from(Relationship relationship) {
            VirtualRelationshipBean bean = new VirtualRelationshipBean();
            bean.setName(relationship.getName());
            bean.setCardinality(relationship.getCardinality());
            if (relationship.getSource() != null) {
                bean.setSourceSchema(relationship.getSource().getSchema());
                bean.setSourceEntity(relationship.getSource().getEntity());
                bean.setSourceKind(relationship.getSource().getKind());
                if (relationship.getSource().getKey() != null) {
                    bean.setSourceColumn(relationship.getSource().getKey().getColumn());
                }
            }
            if (relationship.getTarget() != null) {
                bean.setTargetSchema(relationship.getTarget().getSchema());
                bean.setTargetEntity(relationship.getTarget().getEntity());
                bean.setTargetKind(relationship.getTarget().getKind());
                if (relationship.getTarget().getKey() != null) {
                    bean.setTargetColumn(relationship.getTarget().getKey().getColumn());
                }
            }
            bean.normalize();
            return bean;
        }

        Relationship toRelationship() {
            EntityRef source = new EntityRef(sourceSchema, sourceEntity, sourceKind, new Key(sourceColumn));
            EntityRef target = new EntityRef(targetSchema, targetEntity, targetKind, new Key(targetColumn));
            return new Relationship(name, cardinality, source, target);
        }

        VirtualRelationshipBean copy() {
            VirtualRelationshipBean copy = new VirtualRelationshipBean();
            copy.name = this.name;
            copy.cardinality = this.cardinality;
            copy.sourceSchema = this.sourceSchema;
            copy.sourceEntity = this.sourceEntity;
            copy.sourceKind = this.sourceKind;
            copy.sourceColumn = this.sourceColumn;
            copy.targetSchema = this.targetSchema;
            copy.targetEntity = this.targetEntity;
            copy.targetKind = this.targetKind;
            copy.targetColumn = this.targetColumn;
            return copy;
        }

        void normalize() {
            name = trimToNull(name);
            cardinality = trimToNull(cardinality);
            sourceSchema = trimToNull(sourceSchema);
            sourceEntity = trimToNull(sourceEntity);
            sourceKind = trimToNull(sourceKind);
            sourceColumn = trimToNull(sourceColumn);
            targetSchema = trimToNull(targetSchema);
            targetEntity = trimToNull(targetEntity);
            targetKind = trimToNull(targetKind);
            targetColumn = trimToNull(targetColumn);
        }

        boolean isValid() {
            return name != null
                    && cardinality != null
                    && sourceSchema != null
                    && sourceEntity != null
                    && sourceKind != null
                    && sourceColumn != null
                    && targetSchema != null
                    && targetEntity != null
                    && targetKind != null
                    && targetColumn != null;
        }

        String getSourceSummary() {
            return summarize(sourceSchema, sourceEntity, sourceColumn, sourceKind);
        }

        String getTargetSummary() {
            return summarize(targetSchema, targetEntity, targetColumn, targetKind);
        }

        private String summarize(String schema, String entity, String column, String kind) {
            StringBuilder sb = new StringBuilder();
            if (schema != null) {
                sb.append(schema).append('.');
            }
            if (entity != null) {
                sb.append(entity);
            }
            if (column != null) {
                sb.append('.').append(column);
            }
            if (kind != null) {
                sb.append(' ').append('(').append(kind).append(')');
            }
            return sb.toString();
        }

        private static String trimToNull(String value) {
            if (value == null) {
                return null;
            }
            String trimmed = value.trim();
            return trimmed.isEmpty() ? null : trimmed;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCardinality() {
            return cardinality;
        }

        public void setCardinality(String cardinality) {
            this.cardinality = cardinality;
        }

        public String getSourceSchema() {
            return sourceSchema;
        }

        public void setSourceSchema(String sourceSchema) {
            this.sourceSchema = sourceSchema;
        }

        public String getSourceEntity() {
            return sourceEntity;
        }

        public void setSourceEntity(String sourceEntity) {
            this.sourceEntity = sourceEntity;
        }

        public String getSourceKind() {
            return sourceKind;
        }

        public void setSourceKind(String sourceKind) {
            this.sourceKind = sourceKind;
        }

        public String getSourceColumn() {
            return sourceColumn;
        }

        public void setSourceColumn(String sourceColumn) {
            this.sourceColumn = sourceColumn;
        }

        public String getTargetSchema() {
            return targetSchema;
        }

        public void setTargetSchema(String targetSchema) {
            this.targetSchema = targetSchema;
        }

        public String getTargetEntity() {
            return targetEntity;
        }

        public void setTargetEntity(String targetEntity) {
            this.targetEntity = targetEntity;
        }

        public String getTargetKind() {
            return targetKind;
        }

        public void setTargetKind(String targetKind) {
            this.targetKind = targetKind;
        }

        public String getTargetColumn() {
            return targetColumn;
        }

        public void setTargetColumn(String targetColumn) {
            this.targetColumn = targetColumn;
        }
    }
}
