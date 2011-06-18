/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import org.apache.wicket.Component;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.protocol.http.WebRequest;

/**
 * Turns a normal text area into a an editor via the EditArea javascript library that supprots
 * syntax highlighting.
 * <p>
 * See {@linkplain http://www.cdolivet.com/editarea/editarea/docs/} for info about EditArea.
 * The syntax scheme can be set by passing the string to the constructor. 
 * </p>
 * 
 * @author aaime
 */
@SuppressWarnings("serial")
public class EditAreaBehavior extends AbstractBehavior {
    
    public static final ResourceReference REFERENCE = new ResourceReference(
            EditAreaBehavior.class, "js/editarea/edit_area_full.js");
    private Component component;

    private String syntax;

    public EditAreaBehavior() {
        this("xml");
    }
    
    public EditAreaBehavior(String syntax) {
        this.syntax = syntax;
    }
    
    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.renderJavascriptReference(REFERENCE);
        
        String renderOnDomReady = getRenderOnDomReadyJavascript(response);
        if (renderOnDomReady != null)
            response.renderOnDomReadyJavascript(renderOnDomReady);

        String renderJavaScript = getRenderJavascript(response);
        if (renderJavaScript != null)
            response.renderJavascript(renderJavaScript, null);
    }
    
    protected String getRenderOnDomReadyJavascript(IHeaderResponse response) {
        if (component == null)
            throw new IllegalStateException("TinyMceBehavior is not bound to a component");
        if (!mayRenderJavascriptDirect())
            return getEditAreaInitJavascript();
        return null;
    }

    private String getEditAreaInitJavascript() {
        return "editAreaLoader.init({" +
                "id : \"" + component.getMarkupId() + "\"," +
                "syntax: \""+syntax+"\"," +
                "start_highlight: true," +
                "allow_toggle: false," +
                "font_size: 8," + 
                "min_width: 700," + 
                "min_height: 500," + 
                "allow_resize: true," + 
                "cursor_position: \"begin\"});";
    }

    private boolean mayRenderJavascriptDirect() {
        return RequestCycle.get().getRequest() instanceof WebRequest && !((WebRequest)RequestCycle.get().getRequest()).isAjax();
    }

    protected String getRenderJavascript(IHeaderResponse response) {
        if (component == null)
            throw new IllegalStateException("EditAreaBehavior is not bound to a component");
        if (mayRenderJavascriptDirect())
            return getEditAreaInitJavascript();
        return null;
    }

    public void bind(Component component) {
        if (this.component != null)
            throw new IllegalStateException("TinyMceBehavior can not bind to more than one component");
        super.bind(component);
        component.setOutputMarkupId(true);
        this.component = component;
    }

}
