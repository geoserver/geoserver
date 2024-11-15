/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Optional;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.IAjaxCallListener;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.PackageResourceReference;

/**
 * A XML editor based on CodeMirror
 *
 * @author Andrea Aime
 */
// TODO WICKET8 - Verify this page works OK
@SuppressWarnings("serial")
public class CodeMirrorEditor extends FormComponentPanel<String> {

    public static final PackageResourceReference REFERENCE =
            new PackageResourceReference(CodeMirrorEditor.class, "js/codemirror/js/codemirror.js");

    public static final PackageResourceReference[] CSS_REFERENCE = {
        new PackageResourceReference(CodeMirrorEditor.class, "js/codemirror/css/codemirror.css"),
        new PackageResourceReference(CodeMirrorEditor.class, "js/codemirror/css/show-hint.css"),
        new PackageResourceReference(
                CodeMirrorEditor.class, "js/codemirror/addon/dialog/dialog.css"),
        new PackageResourceReference(
                CodeMirrorEditor.class, "js/codemirror/addon/search/matchesonscrollbar.css")
    };

    public static final PackageResourceReference[] MODES = {
        new PackageResourceReference(CodeMirrorEditor.class, "js/codemirror/js/xml.js"),
        new PackageResourceReference(CodeMirrorEditor.class, "js/codemirror/js/clike.js"),
        new PackageResourceReference(CodeMirrorEditor.class, "js/codemirror/js/groovy.js"),
        new PackageResourceReference(CodeMirrorEditor.class, "js/codemirror/js/javascript.js"),
        new PackageResourceReference(CodeMirrorEditor.class, "js/codemirror/js/python.js"),
        new PackageResourceReference(CodeMirrorEditor.class, "js/codemirror/js/ruby.js"),
        new PackageResourceReference(CodeMirrorEditor.class, "js/codemirror/js/css.js"),
        new PackageResourceReference(CodeMirrorEditor.class, "js/codemirror/js/show-hint.js"),
        new PackageResourceReference(CodeMirrorEditor.class, "js/codemirror/js/geocss-hint.js"),
        new PackageResourceReference(CodeMirrorEditor.class, "js/codemirror/js/xml-hint.js"),
        new PackageResourceReference(
                CodeMirrorEditor.class, "js/codemirror/addon/dialog/dialog.js"),
        new PackageResourceReference(
                CodeMirrorEditor.class, "js/codemirror/addon/search/searchcursor.js"),
        new PackageResourceReference(
                CodeMirrorEditor.class, "js/codemirror/addon/search/search.js"),
        new PackageResourceReference(
                CodeMirrorEditor.class, "js/codemirror/addon/search/matchesonscrollbar.js"),
        new PackageResourceReference(
                CodeMirrorEditor.class, "js/codemirror/addon/scroll/annotatescrollbar.js")
    };

    private TextArea<String> editor;

    private WebMarkupContainer container;

    private String mode;

    private RepeatingView customButtons;

    public CodeMirrorEditor(String id, String mode, IModel<String> model) {
        super(id, model);
        this.mode = mode;

        container = new WebMarkupContainer("editorContainer");
        container.setOutputMarkupId(true);
        add(container);

        WebMarkupContainer toolbar = new WebMarkupContainer("toolbar");
        toolbar.setVisible(true);
        container.add(toolbar);

        customButtons = new RepeatingView("custom-buttons");
        toolbar.add(customButtons);

        WebMarkupContainer editorParent = new WebMarkupContainer("editorParent");
        editorParent.add(AttributeModifier.replace("class", "codemirror"));
        container.add(editorParent);
        editor = new TextArea<>("editor", model);
        editorParent.add(editor);
        editor.setOutputMarkupId(true);

        editor.add(new CodeMirrorBehavior());
    }

    public void addCustomButton(String title, String cssClass, CustomButtonAction action) {
        customButtons.add(
                new AjaxLink<>(customButtons.newChildId()) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        action.onClick(target);
                    }

                    @Override
                    protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                        super.updateAjaxAttributes(attributes);
                        CharSequence dynamicExtraParameters =
                                "var result = {'cmSelection': getSelection()};" + "return result;";
                        attributes.getDynamicExtraParameters().add(dynamicExtraParameters);
                    }
                }.add(new AttributeAppender("class", cssClass, " "))
                        .add(new AttributeAppender("title", title, " ")));
    }

    public CodeMirrorEditor(String id, IModel<String> model) {
        this(id, "xml", model);
    }

    @Override
    public void convertInput() {
        editor.processInput();
        setConvertedInput(editor.getConvertedInput());
    }

    @Override
    public String getInput() {
        return editor.getInput();
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        // Make the line numbers look good
        response.render(
                CssHeaderItem.forReference(
                        new PackageResourceReference(
                                CodeMirrorEditor.class,
                                "js/codemirror/css/codemirrorlinenos.css")));
    }

    public void setTextAreaMarkupId(String id) {
        editor.setMarkupId(id);
    }

    public String getTextAreaMarkupId() {
        return editor.getMarkupId();
    }

    public void setMode(String mode) {
        this.mode = mode;
        Optional<AjaxRequestTarget> requestTarget =
                RequestCycle.get().find(AjaxRequestTarget.class);
        if (requestTarget.isPresent()) {
            String javascript =
                    "document.gsEditors."
                            + editor.getMarkupId()
                            + ".setOption('mode', '"
                            + mode
                            + "');";
            requestTarget.get().appendJavaScript(javascript);
        }
    }

    public void setModeAndSubMode(String mode, String subMode) {
        this.mode = mode;
        Optional<AjaxRequestTarget> requestTarget =
                RequestCycle.get().find(AjaxRequestTarget.class);
        if (requestTarget.isPresent()) {
            String javascript = "document.gsEditors." + editor.getMarkupId() + ".setOption('mode',";
            String modeObj = "{name: \"" + mode + "\", " + subMode + ": true}";
            javascript += modeObj + ");";
            requestTarget.get().appendJavaScript(javascript);
            editor.modelChanged();
            requestTarget.get().add(editor);
        }
    }

    public void reset() {
        super.validate();
        editor.validate();
        editor.clearInput();
    }

    public IAjaxCallListener getSaveDecorator() {
        // we need to force CodeMirror to update the textarea contents (which it hid)
        // before submitting the form, otherwise the validation will use the old contents
        return new AjaxCallListener() {

            @Override
            public CharSequence getBeforeHandler(Component component) {
                String id = getTextAreaMarkupId();
                return "if (document.gsEditors) { document.getElementById('"
                        + id
                        + "').value = document.gsEditors."
                        + id
                        + ".getValue(); }";
            }
        };
    }

    class CodeMirrorBehavior extends Behavior {

        @Override
        public void renderHead(Component component, IHeaderResponse response) {
            super.renderHead(component, response);
            // Add CSS
            for (PackageResourceReference css : CSS_REFERENCE) {
                response.render(CssHeaderItem.forReference(css));
            }
            // Add JS
            response.render(JavaScriptHeaderItem.forReference(REFERENCE));
            // Add Modes
            for (PackageResourceReference mode : MODES) {
                response.render(JavaScriptHeaderItem.forReference(mode));
            }

            response.render(OnDomReadyHeaderItem.forScript(getInitJavascript()));
        }

        private String getInitJavascript() {
            try (InputStream is =
                    CodeMirrorEditor.class.getResourceAsStream("CodeMirrorEditor.js")) {
                String js = convertStreamToString(is);
                js = js.replaceAll("\\$componentId", editor.getMarkupId());
                js = js.replaceAll("\\$codeMirrorEditorId", getMarkupId());
                js = js.replaceAll("\\$mode", mode);
                js = js.replaceAll("\\$container", container.getMarkupId());
                return js;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @SuppressWarnings("PMD.UseTryWithResources")
        public String convertStreamToString(InputStream is) {
            /*
             * To convert the InputStream to String we use the Reader.read(char[] buffer) method. We
             * iterate until the Reader return -1 which means there's no more data to read. We use
             * the StringWriter class to produce the string.
             */
            try {
                if (is != null) {
                    try (Writer writer = new StringWriter();
                            Reader reader = new BufferedReader(new InputStreamReader(is, UTF_8))) {

                        char[] buffer = new char[1024];
                        int n;
                        while ((n = reader.read(buffer)) != -1) {
                            writer.write(buffer, 0, n);
                        }
                        return writer.toString();
                    } finally {
                        is.close();
                    }
                } else {
                    return "";
                }
            } catch (IOException e) {
                throw new RuntimeException("Did not expect this one...", e);
            }
        }
    }

    public interface CustomButtonAction extends Serializable {
        void onClick(AjaxRequestTarget target);
    }
}
