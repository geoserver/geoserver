/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.transform.AttributeRemapTransform;
import org.geoserver.importer.transform.DateFormatTransform;
import org.geoserver.importer.transform.NumberFormatTransform;
import org.geoserver.importer.transform.ReprojectTransform;
import org.geoserver.importer.transform.TransformChain;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.CRSPanel;

public class ImportTaskAdvancedPage extends GeoServerSecuredPage {

    CheckBox reprojectCheckBox;
    ReprojectionPanel reprojectPanel;
    AttributeRemappingPanel remapPanel;

    public ImportTaskAdvancedPage(final IModel<ImportTask> model) {
        ImportTask item = model.getObject();
        // item.getTransform().get

        Form form = new Form("form");
        add(form);

        ReprojectTransform reprojectTx =
                (ReprojectTransform) item.getTransform().get(ReprojectTransform.class);

        reprojectCheckBox = new CheckBox("enableReprojection", new Model(reprojectTx != null));
        reprojectCheckBox.add(
                new AjaxFormComponentUpdatingBehavior("click") {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        reprojectPanel.setEnabled(reprojectCheckBox.getModelObject());
                        target.add(reprojectPanel);
                    }
                });
        form.add(reprojectCheckBox);

        if (reprojectTx == null) {
            reprojectTx = new ReprojectTransform(null);
            reprojectTx.setSource(item.getLayer().getResource().getNativeCRS());
        }

        reprojectPanel = new ReprojectionPanel("reprojection", reprojectTx);
        reprojectPanel.setOutputMarkupId(true);
        reprojectPanel.setEnabled(false);
        form.add(reprojectPanel);

        remapPanel = new AttributeRemappingPanel("remapping", model);
        form.add(remapPanel);

        form.add(
                new AjaxSubmitLink("save") {
                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        ImportTask task = model.getObject();
                        TransformChain txChain = task.getTransform();

                        // reprojection
                        txChain.removeAll(ReprojectTransform.class);

                        if (reprojectCheckBox.getModelObject()) {
                            txChain.add(reprojectPanel.getTransform());
                        }

                        // remaps
                        txChain.removeAll(AttributeRemapTransform.class);
                        txChain.getTransforms().addAll(remapPanel.remaps);

                        try {
                            ImporterWebUtils.importer().changed(task);
                        } catch (IOException e) {
                            error(e);
                        }

                        PageParameters pp =
                                new PageParameters().add("id", task.getContext().getId());
                        setResponsePage(ImportPage.class, pp);
                    }
                });
        form.add(
                new AjaxLink("cancel") {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        ImportTask task = model.getObject();
                        PageParameters pp =
                                new PageParameters().add("id", task.getContext().getId());
                        setResponsePage(ImportPage.class, pp);
                    }
                });
    }

    static class ReprojectionPanel extends Panel {

        ReprojectTransform transform;

        public ReprojectionPanel(String id, ReprojectTransform transform) {
            super(id);

            this.transform = transform;

            add(new CRSPanel("from", new PropertyModel(transform, "source")));
            add(new CRSPanel("to", new PropertyModel(transform, "target")));
        }

        public ReprojectTransform getTransform() {
            return transform;
        }
    }

    static class AttributeRemappingPanel extends Panel {

        List<AttributeRemapTransform> remaps;
        ListView<AttributeRemapTransform> remapList;

        public AttributeRemappingPanel(String id, IModel<ImportTask> itemModel) {
            super(id, itemModel);
            setOutputMarkupId(true);

            FeatureTypeInfo featureType =
                    (FeatureTypeInfo) itemModel.getObject().getLayer().getResource();
            final List atts = new ArrayList();
            for (AttributeTypeInfo at : featureType.getAttributes()) {
                atts.add(at.getName());
            }

            final List<Class> types = (List) Arrays.asList(Integer.class, Double.class, Date.class);

            final WebMarkupContainer remapContainer = new WebMarkupContainer("remapsContainer");
            remapContainer.setOutputMarkupId(true);
            add(remapContainer);

            remaps = itemModel.getObject().getTransform().getAll(AttributeRemapTransform.class);
            remapList =
                    new ListView<AttributeRemapTransform>("remaps", remaps) {

                        @Override
                        protected void populateItem(final ListItem<AttributeRemapTransform> item) {

                            final DropDownChoice<String> attChoice =
                                    new DropDownChoice<String>(
                                            "att",
                                            new PropertyModel(item.getModel(), "field"),
                                            atts);
                            item.add(attChoice);

                            final DropDownChoice<Class> typeChoice =
                                    new DropDownChoice<Class>(
                                            "type",
                                            new PropertyModel(item.getModel(), "type"),
                                            types,
                                            new ChoiceRenderer<Class>() {

                                                public Object getDisplayValue(Class object) {
                                                    return object.getSimpleName();
                                                }
                                            });
                            item.add(typeChoice);

                            final TextField<String> dateFormatTextField =
                                    new TextField<String>("dateFormat", new Model());
                            dateFormatTextField.setOutputMarkupId(true);
                            item.add(dateFormatTextField);

                            typeChoice.add(
                                    new AjaxFormComponentUpdatingBehavior("change") {
                                        @Override
                                        protected void onUpdate(AjaxRequestTarget target) {
                                            dateFormatTextField.setEnabled(
                                                    Date.class.equals(typeChoice.getModelObject()));
                                            target.add(dateFormatTextField);
                                        }
                                    });
                            // dateFormatTextField.setVisible(false);

                            item.add(
                                    new AjaxButton("apply") {
                                        @Override
                                        protected void onSubmit(
                                                AjaxRequestTarget target, Form<?> form) {
                                            attChoice.processInput();
                                            typeChoice.processInput();
                                            dateFormatTextField.processInput();

                                            AttributeRemapTransform tx = item.getModelObject();

                                            String field = tx.getField();
                                            Class type = typeChoice.getModelObject();

                                            if (Date.class.equals(type)) {
                                                String dateFormat =
                                                        dateFormatTextField.getModelObject();
                                                if (dateFormat == null
                                                        || "".equals(dateFormat.trim())) {
                                                    dateFormat = null;
                                                }
                                                item.setModelObject(
                                                        new DateFormatTransform(field, dateFormat));
                                            } else if (Number.class.isAssignableFrom(type)) {
                                                item.setModelObject(
                                                        new NumberFormatTransform(field, type));
                                            }

                                            target.add(remapContainer);
                                        }
                                    }.setDefaultFormProcessing(false));

                            item.add(
                                    new AjaxButton("cancel") {
                                        @Override
                                        protected void onSubmit(
                                                AjaxRequestTarget target, Form<?> form) {
                                            remaps.remove(item.getModelObject());
                                            target.add(remapContainer);
                                        }
                                    }.setDefaultFormProcessing(false));
                        }
                    };
            remapList.setOutputMarkupId(true);
            remapContainer.add(remapList);

            add(
                    new AjaxLink<ImportTask>("add", itemModel) {
                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            remaps.add(new AttributeRemapTransform(null, null));
                            target.add(remapContainer);
                        }
                    });
        }
    }
}
