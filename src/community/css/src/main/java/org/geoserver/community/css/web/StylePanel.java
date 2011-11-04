package org.geoserver.community.css.web;

import java.io.File;
import org.apache.wicket.Component;
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
        String cssSource
    ) {
        super(id, model);
        File cssFile = page.findStyleFile(cssSource);
        if (cssFile != null && cssFile.exists()) {
            styleBody = ""; // readWholeFile(cssFile);
        } else {
            styleBody =
                "No CSS file was found for this style. Please make sure " +
                "this is the style you intended to edit, since saving " + 
                "the CSS will destroy the existing SLD.";
        }

        Form styleEditor = new Form("style-editor");
        styleEditor.add(new Label("label", "The stylesheet for this map")); // TODO: i18n
        UpdatingTextArea textArea =
            new UpdatingTextArea("editor", new PropertyModel(this, "styleBody"), feedback);
        textArea.add(new CssValidator());
        styleEditor.add(textArea);
        styleEditor.add(new CssSubmitButton(
            "submit", styleEditor, page, cssSource, styleBody));
        AjaxFormValidatingBehavior.addToAllFormComponents(styleEditor, "onkeyup", Duration.ONE_SECOND);
        add(styleEditor);
    }
}
