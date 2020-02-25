/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.web.panel;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.IAjaxCallListener;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

public abstract class SimpleAjaxSubmitLink extends Panel {

    private static final long serialVersionUID = -8153202504953573164L;

    private AjaxSubmitLink link;
    private Label label;

    public SimpleAjaxSubmitLink(String id, IModel<?> labelModel) {
        super(id);
        add(link = buildAjaxLink());
        link.add(label = new Label("label", labelModel));
    }

    protected AjaxSubmitLink buildAjaxLink() {
        return new AjaxSubmitLink("link") {

            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                SimpleAjaxSubmitLink.this.onSubmit(target, form);
            }

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                attributes.getAjaxCallListeners().add(SimpleAjaxSubmitLink.this.getAjaxListener());
            }
        };
    }

    protected IAjaxCallListener getAjaxListener() {
        return null;
    }

    public AjaxSubmitLink getLink() {
        return link;
    }

    public Label getLabel() {
        return label;
    }

    /** Subclasses should override and provide the behaviour for */
    protected abstract void onSubmit(AjaxRequestTarget target, Form<?> form);
}
