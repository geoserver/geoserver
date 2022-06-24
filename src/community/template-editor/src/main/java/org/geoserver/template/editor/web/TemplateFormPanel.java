/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.template.editor.web;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class TemplateFormPanel extends Panel {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    private Label srcpath_label, destpath_label, destpath_label_dirty, filename_label;

    private TextArea<String> tpl;

    private AjaxLink save_btn, reload_btn, delete_btn;

    private AbstractTemplateEditorPage templateEditorPage;

    private IModel<TemplateResourceObject> templateModel;

    public TemplateFormPanel(String id, final IModel<TemplateResourceObject> model) {
        super(id, model);
        this.templateModel = model;

        srcpath_label = new Label("srcpath");
        srcpath_label.setOutputMarkupId(true);
        add(srcpath_label);
        filename_label = new Label("filename");
        filename_label.setOutputMarkupId(true);
        add(filename_label);

        destpath_label = new Label("destpath");
        destpath_label.setOutputMarkupId(true);
        add(destpath_label);

        destpath_label_dirty = new Label("dirty");
        destpath_label_dirty.setOutputMarkupId(true);
        add(destpath_label_dirty);

        tpl = new TextArea<>("content");
        tpl.setOutputMarkupId(true);
        add(tpl);
        tpl.add(
                new OnChangeAjaxBehavior() {
                    /** serialVersionUID */
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        ajaxSetDirty(target);
                    }
                });

        save_btn =
                new AjaxLink<String>("save_btn", Model.of("Save")) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (templateEditorPage != null) {
                            templateEditorPage.saveTemplate(model.getObject());
                            ajaxReload(target);
                        }
                    }
                };
        save_btn.setEnabled(false);
        save_btn.setOutputMarkupId(true);
        add(save_btn);
        //        save_btn.setEnabled(true);

        reload_btn =
                new AjaxLink<String>("reload_btn", Model.of("Reload")) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        ajaxReload(target);
                    }
                };
        add(reload_btn);

        delete_btn =
                new AjaxLink<String>("delete_btn", Model.of("Delete this template file")) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (templateEditorPage != null) {
                            templateEditorPage.deleteTemplate(model.getObject());
                            ajaxReload(target);
                        }
                    }
                };

        if (((TemplateResourceObject) model.getObject()).isLocalPath()) {
            delete_btn.add(AttributeModifier.remove("disabled"));
        } else {
            delete_btn.add(AttributeModifier.replace("disabled", "disabled"));
        }
        add(delete_btn);
    }

    private void ajaxSetDirty(AjaxRequestTarget target) {
        templateModel.getObject().setDirty("*");
        srcpath_label.add(AttributeModifier.replace("class", ""));
        destpath_label.add(AttributeModifier.replace("class", "active"));
        destpath_label_dirty.add(AttributeModifier.replace("class", "active"));
        save_btn.add(AttributeModifier.remove("disabled"));
        target.add(srcpath_label);
        target.add(destpath_label);
        target.add(destpath_label_dirty);
        target.add(save_btn);
    }

    private void ajaxReload(AjaxRequestTarget target) {
        TemplateResourceObject obj = (TemplateResourceObject) templateModel.getObject();
        obj.resetContent();
        srcpath_label.add(AttributeModifier.replace("class", "active"));
        destpath_label.add(AttributeModifier.replace("class", ""));
        templateModel.getObject().setDirty("");
        destpath_label_dirty.add(AttributeModifier.replace("class", ""));
        save_btn.add(AttributeModifier.replace("disabled", "disabled"));
        if (obj.isLocalPath()) {
            delete_btn.add(AttributeModifier.remove("disabled"));
        } else {
            delete_btn.add(AttributeModifier.replace("disabled", "disabled"));
        }
        target.add(tpl);
        target.add(srcpath_label);
        target.add(destpath_label);
        target.add(destpath_label_dirty);
        target.add(save_btn);
        target.add(delete_btn);
    }

    public void setParent(AbstractTemplateEditorPage page) {
        this.templateEditorPage = page;
    }
}
