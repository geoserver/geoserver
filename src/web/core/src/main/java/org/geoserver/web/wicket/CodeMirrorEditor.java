/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.calldecorator.AjaxCallDecorator;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * A XML editor based on CodeMirror
 * @author Andrea Aime 
 */
@SuppressWarnings("serial")
public class CodeMirrorEditor extends FormComponentPanel<String> {

    public static final ResourceReference REFERENCE = new ResourceReference(
            CodeMirrorEditor.class, "js/codemirror/js/codemirror.js");
    
    private TextArea<String> editor;

    private WebMarkupContainer container;

    public CodeMirrorEditor(String id, IModel<String> model) {
        super(id, model);
        
        container = new WebMarkupContainer("editorContainer");
        container.setOutputMarkupId(true);
        add(container);
        
        editor = new TextArea<String>("editor", model);
        container.add(editor);
        editor.setOutputMarkupId(true);
        editor.add(new CodeMirrorBehavior());
    }
    
    @Override
    protected void convertInput() {
        editor.processInput();
        setConvertedInput(editor.getConvertedInput());
    }
    
    @Override
    public String getInput() {
        return editor.getInput();
    }
    
    public void setTextAreaMarkupId(String id) {
        editor.setMarkupId(id);
    }
    
    public String getTextAreaMarkupId() {
        return editor.getMarkupId();
    }
    
    public void reset() {
        super.validate();
        editor.validate();
        editor.clearInput();
    }
    
    public IAjaxCallDecorator getSaveDecorator() {
        // we need to force CodeMirror to update the textarea contents (which it hid)
        // before submitting the form, otherwise the validation will use the old contents
        return new AjaxCallDecorator() {
            @Override
            public CharSequence decorateScript(CharSequence script) {
                // textarea.value = codemirrorinstance.getCode()
                String id = getTextAreaMarkupId();
                return "document.getElementById('" + id + "').value = document.gsEditors." + id + ".getCode();" + script;
            }
        };
    }
    
    class CodeMirrorBehavior extends AbstractBehavior {

        @Override
        public void renderHead(IHeaderResponse response) {
            super.renderHead(response);
            response.renderJavascriptReference(REFERENCE);

            response.renderOnDomReadyJavascript(getInitJavascript());
        }

        private String getInitJavascript() {
            InputStream is = CodeMirrorEditor.class.getResourceAsStream("CodeMirrorEditor.js");
            String js = convertStreamToString(is);
            js = js.replaceAll("\\$componentId", editor.getMarkupId());
            js = js.replaceAll("\\$syntax", "parsexml.js");
            js = js.replaceAll("\\$container", container.getMarkupId());
            js = js.replaceAll("\\$stylesheet", "./resources/org.geoserver.web.wicket.CodeMirrorEditor/js/codemirror/css/xmlcolors.css");
            return js;
        }

        public String convertStreamToString(InputStream is) {
            /*
             * To convert the InputStream to String we use the Reader.read(char[] buffer) method. We
             * iterate until the Reader return -1 which means there's no more data to read. We use
             * the StringWriter class to produce the string.
             */
            try {
                if (is != null) {
                    Writer writer = new StringWriter();

                    char[] buffer = new char[1024];
                    try {
                        Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                        int n;
                        while ((n = reader.read(buffer)) != -1) {
                            writer.write(buffer, 0, n);
                        }
                    } finally {
                        is.close();
                    }
                    return writer.toString();
                } else {
                    return "";
                }
            } catch (IOException e) {
                throw new RuntimeException("Did not expect this one...", e);
            }
        }

    }

}
