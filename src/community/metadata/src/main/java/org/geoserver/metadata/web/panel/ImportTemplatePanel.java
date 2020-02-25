/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.web.panel;

import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.feedback.ContainerFeedbackMessageFilter;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.DefaultItemReuseStrategy;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.metadata.data.model.MetadataTemplate;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;

/**
 * The ImportTemplatePanel allows the user to link the metadata to values configured in the metadata
 * template.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 */
public abstract class ImportTemplatePanel extends Panel {
    private static final long serialVersionUID = 1297739738862860160L;

    private GeoServerTablePanel<MetadataTemplate> templatesPanel;

    private ImportTemplateDataProvider linkedTemplatesDataProvider;

    private Label noData;

    private AjaxSubmitLink remove;

    public ImportTemplatePanel(String id, IModel<List<MetadataTemplate>> templatesModel) {
        super(id);
        linkedTemplatesDataProvider = new ImportTemplateDataProvider(templatesModel);
    }

    @Override
    public void onInitialize() {
        super.onInitialize();

        GeoServerDialog dialog = new GeoServerDialog("importDialog");
        dialog.setInitialHeight(100);
        add(dialog);

        // link action and dropdown
        DropDownChoice<MetadataTemplate> dropDown = createTemplatesDropDown();
        dropDown.setOutputMarkupId(true);
        add(dropDown);
        AjaxSubmitLink importAction = createImportAction(dropDown, dialog);
        add(importAction);
        // unlink button
        remove = createUnlinkAction();
        remove.setOutputMarkupId(true);
        remove.setOutputMarkupPlaceholderTag(true);
        remove.setEnabled(false);
        add(remove);
        add(
                new FeedbackPanel("linkTemplateFeedback", new ContainerFeedbackMessageFilter(this))
                        .setOutputMarkupId(true));

        // the panel
        templatesPanel = createTemplateTable(remove);
        templatesPanel.setFilterVisible(false);
        templatesPanel.setFilterable(false);
        templatesPanel.getTopPager().setVisible(false);
        templatesPanel.getBottomPager().setVisible(false);
        templatesPanel.setSelectable(true);
        templatesPanel.setSortable(false);
        templatesPanel.setOutputMarkupId(true);
        templatesPanel.setOutputMarkupPlaceholderTag(true);
        templatesPanel.setItemReuseStrategy(DefaultItemReuseStrategy.getInstance());

        add(templatesPanel);

        // the no data links label
        noData = new Label("noData", new ResourceModel("noData"));
        noData.setOutputMarkupId(true);
        noData.setOutputMarkupPlaceholderTag(true);
        add(noData);
        updateTableState(null, linkedTemplatesDataProvider);
    }

    public FeedbackPanel getFeedbackPanel() {
        return (FeedbackPanel) get("linkTemplateFeedback");
    }

    private DropDownChoice<MetadataTemplate> createTemplatesDropDown() {
        IModel<MetadataTemplate> model = new Model<MetadataTemplate>();
        List<MetadataTemplate> unlinked = linkedTemplatesDataProvider.getUnlinkedItems();
        DropDownChoice<MetadataTemplate> dropDownChoice =
                new DropDownChoice<>(
                        "metadataTemplate", model, unlinked, new ChoiceRenderer<>("name"));
        return dropDownChoice;
    }

    @SuppressWarnings("unchecked")
    protected DropDownChoice<MetadataTemplate> getDropDown() {
        return (DropDownChoice<MetadataTemplate>) get("metadataTemplate");
    }

    private AjaxSubmitLink createImportAction(
            final DropDownChoice<MetadataTemplate> dropDown, GeoServerDialog dialog) {
        return new AjaxSubmitLink("link") {
            private static final long serialVersionUID = -8718015688839770852L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {

                boolean valid = true;
                if (dropDown.getModelObject() == null) {
                    error(
                            new ParamResourceModel("errorSelectTemplate", ImportTemplatePanel.this)
                                    .getString());
                    valid = false;
                }
                if (valid) {
                    dialog.setTitle(
                            new ParamResourceModel(
                                    "confirmImportDialog.title", ImportTemplatePanel.this));
                    dialog.showOkCancel(
                            target,
                            new GeoServerDialog.DialogDelegate() {

                                private static final long serialVersionUID = -5552087037163833563L;

                                @Override
                                protected Component getContents(String id) {
                                    ParamResourceModel resource =
                                            new ParamResourceModel(
                                                    "confirmImportDialog.content",
                                                    ImportTemplatePanel.this);
                                    return new MultiLineLabel(id, resource.getString());
                                }

                                @Override
                                protected boolean onSubmit(
                                        AjaxRequestTarget target, Component contents) {
                                    linkTemplate(target, dropDown.getModelObject());
                                    handleUpdate(target);
                                    return true;
                                }
                            });
                }
                target.add(getFeedbackPanel());
                target.add(templatesPanel);
                target.add(dropDown);
            }

            protected void onError(AjaxRequestTarget target, Form<?> form) {
                ((GeoServerBasePage) getPage()).addFeedbackPanels(target);
            }
        };
    }

    private AjaxSubmitLink createUnlinkAction() {
        return new AjaxSubmitLink("removeSelected") {
            private static final long serialVersionUID = 3581476968062788921L;

            @Override
            public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                unlinkTemplate(target, templatesPanel.getSelection());
                handleUpdate(target);
            }

            protected void onError(AjaxRequestTarget target, Form<?> form) {
                ((GeoServerBasePage) getPage()).addFeedbackPanels(target);
            }
        };
    }

    private GeoServerTablePanel<MetadataTemplate> createTemplateTable(AjaxSubmitLink remove) {

        return new GeoServerTablePanel<MetadataTemplate>(
                "templatesPanel", linkedTemplatesDataProvider, true) {

            private static final long serialVersionUID = -8943273843044917552L;

            @Override
            protected void onSelectionUpdate(AjaxRequestTarget target) {
                remove.setEnabled(templatesPanel.getSelection().size() > 0);
                target.add(remove);
            }

            @Override
            protected Component getComponentForProperty(
                    String id,
                    IModel<MetadataTemplate> itemModel,
                    GeoServerDataProvider.Property<MetadataTemplate> property) {

                return null;
            }
        };
    }

    /** Link the template and the current metadata */
    public void linkTemplate(AjaxRequestTarget target, MetadataTemplate selected) {
        // add template link to metadata
        linkedTemplatesDataProvider.addLink(selected);
        getDropDown().setModelObject(null);
        getDropDown().setChoices(linkedTemplatesDataProvider.getUnlinkedItems());
        updateTableState(target, linkedTemplatesDataProvider);
        target.add(templatesPanel);
        target.add(getDropDown());
    }

    /** Link the template and the selected metadata */
    public void unlinkTemplate(AjaxRequestTarget target, List<MetadataTemplate> templates) {

        linkedTemplatesDataProvider.removeLinks(templates);
        templatesPanel.clearSelection();
        getDropDown().setChoices(linkedTemplatesDataProvider.getUnlinkedItems());
        updateTableState(target, linkedTemplatesDataProvider);
        target.add(getFeedbackPanel());
        target.add(templatesPanel);
        target.add(getDropDown());
    }

    public List<MetadataTemplate> getLinkedTemplates() {
        return linkedTemplatesDataProvider.getItems();
    }

    protected abstract void handleUpdate(AjaxRequestTarget target);

    private void updateTableState(
            AjaxRequestTarget target, ImportTemplateDataProvider dataProvider) {
        boolean isEmpty = dataProvider.getItems().isEmpty();
        templatesPanel.setVisible(!isEmpty);
        remove.setVisible(!isEmpty);
        noData.setVisible(isEmpty);

        if (target != null) {
            target.add(getFeedbackPanel());
            target.add(noData);
            target.add(remove);
            target.add(templatesPanel);
            target.add(getDropDown());
        }
    }
}
