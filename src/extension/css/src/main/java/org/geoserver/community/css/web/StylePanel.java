/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.community.css.web;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.apache.wicket.Component;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.form.AjaxFormValidatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.time.Duration;

public class StylePanel extends Panel {
    private String styleBody;

    public StylePanel(
        String id,
        IModel<CssDemoPage> model,
        CssDemoPage page,
        Component feedback,
        File cssFile
    ) {
        super(id, model);
        if (cssFile != null && cssFile.exists()) {
            try {
                styleBody = FileUtils.readFileToString(cssFile);
            } catch (IOException ioe) {
                throw new WicketRuntimeException("Error loading CSS: ", ioe);
            }
        } else {
            styleBody =
                "No CSS file was found for this style. Please make sure " +
                "this is the style you intended to edit, since saving " + 
                "the CSS will destroy the existing SLD.";
        }

        Form styleEditor = new Form("style-editor");
        styleEditor.add(new Label("label", "The stylesheet for this map")); // TODO: i18n
        PropertyModel<String> styleBodyModel = new PropertyModel(this, "styleBody");
        UpdatingTextArea textArea =
            new UpdatingTextArea("editor", styleBodyModel, feedback);
        textArea.add(new CssValidator());
        styleEditor.add(textArea);
        styleEditor.add(new CssSubmitButton(
            "submit", styleEditor, page, cssFile, styleBodyModel));
        AjaxFormValidatingBehavior.addToAllFormComponents(styleEditor, "onkeyup", Duration.ONE_SECOND);
        add(styleEditor);
    }
}
