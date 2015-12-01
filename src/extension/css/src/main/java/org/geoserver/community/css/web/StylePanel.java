/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.community.css.web;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.apache.commons.io.IOUtils;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.calldecorator.AjaxPreprocessingCallDecorator;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.web.wicket.CodeMirrorEditor;

public class StylePanel extends Panel {
    private String styleBody;

    public StylePanel(String id, IModel<CssDemoPage> model, final CssDemoPage page,
            final Resource cssFile) {
        super(id, model);
        if (cssFile != null && Resources.exists(cssFile)) {
            try (InputStream is = cssFile.in()) {
                styleBody = IOUtils.toString(is, "UTF-8");
            } catch (IOException ioe) {
                throw new WicketRuntimeException("Error loading CSS: ", ioe);
            }
        } else {
            styleBody = "No CSS file was found for this style. Please make sure "
                    + "this is the style you intended to edit, since saving "
                    + "the CSS will destroy the existing SLD.";
        }

        Form styleEditorForm = new Form("style-editor");
        final PropertyModel<String> styleBodyModel = new PropertyModel(this, "styleBody");

        final CodeMirrorEditor editor = new CodeMirrorEditor("editor", styleBodyModel);
        editor.setMode("css");
        // force the id otherwise this blasted thing won't be usable from other forms
        editor.setTextAreaMarkupId("editor");
        editor.setOutputMarkupId(true);
        editor.setRequired(true);
        editor.add(new CssValidator());
        styleEditorForm.add(editor);

        final FeedbackPanel feedback2 = new FeedbackPanel("feedback-low");
        feedback2.setOutputMarkupId(true);
        styleEditorForm.add(feedback2);

        styleEditorForm.add(new AjaxSubmitLink("submit") {
            @Override
            public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                try {
                    StyleInfo info = page.getStyleInfo();
                    editor.processInput();
                    String body = styleBodyModel.getObject();
                    if (CssHandler.FORMAT.equals(info.getFormat())) {
                        // write out directly
                        page.catalog().getResourcePool()
                                .writeStyle(info, new ByteArrayInputStream(body.getBytes()));
                    } else {
                        // create the sld side car file
                        String sld = page.cssText2sldText(body, info);
                        Writer writer = new OutputStreamWriter(cssFile.out());
                        writer.write(body);
                        writer.close();
                        page.catalog()
                                .getResourcePool()
                                .writeStyle(page.getStyleInfo(),
                                        new ByteArrayInputStream(sld.getBytes()));
                    }

                    page.catalog().save(info);
                    target.addComponent(feedback2);
                    if (page.sldPreview != null)
                        target.addComponent(page.sldPreview);
                    if (page.map != null)
                        target.appendJavascript(page.map.getUpdateCommand());
                } catch (Exception e) {
                    throw new WicketRuntimeException(e);
                }

            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.addComponent(feedback2);
            }

            @Override
            protected IAjaxCallDecorator getAjaxCallDecorator() {
                return new AjaxPreprocessingCallDecorator(super.getAjaxCallDecorator()) {

                    @Override
                    public CharSequence preDecorateScript(CharSequence script) {
                        return "if(event.view.document.gsEditors) { "
                                + "event.view.document.gsEditors." + editor.getTextAreaMarkupId()
                                + ".save(); } \n" + script;
                    }
                };
            }
        });
        add(styleEditorForm);


    }
}
