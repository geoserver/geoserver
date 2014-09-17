package org.geoserver.community.css.web;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;

import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.StyleInfo;

class CssSubmitButton extends AjaxButton {
    private final String id;
    private final Form<?> styleEditor;
    private final CssDemoPage page;
    private final File cssFile;
    private final PropertyModel<String> styleBody;

    public CssSubmitButton(
        final String id,
        final Form<?> styleEditor,
        final CssDemoPage page,
        final File cssFile,
        final PropertyModel<String> styleBody)
    {
        super("submit", styleEditor);
        this.id = id;
        this.styleEditor = styleEditor;
        this.page = page;
        this.cssFile = cssFile;
        this.styleBody = styleBody;
    }

    @Override
    public void onSubmit(AjaxRequestTarget target, Form<?> form) {
        try {
            StyleInfo info = page.getStyleInfo();
            if (CssHandler.FORMAT.equals(info.getFormat())) {
                // write out directly
                page.catalog().getResourcePool().writeStyle(info,
                    new ByteArrayInputStream(styleBody.getObject().getBytes()));
            }
            else {
                // create the css side car file
                String sld = page.cssText2sldText(styleBody.getObject(), info);
                Writer writer = new FileWriter(cssFile);
                writer.write(styleBody.getObject());
                writer.close();
                page.catalog().getResourcePool().writeStyle(
                        page.getStyleInfo(), new ByteArrayInputStream(sld.getBytes()));
            }

            page.catalog().save(info);
            if (page.sldPreview != null) target.addComponent(page.sldPreview);
            if (page.map != null) target.appendJavascript(page.map.getUpdateCommand());
        } catch (Exception e) {
            throw new WicketRuntimeException(e);
        }

    }
}
