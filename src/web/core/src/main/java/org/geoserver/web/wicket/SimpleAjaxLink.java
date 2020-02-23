/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.IAjaxCallListener;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

/**
 * A simple ajax link with a label inside. This is a utility component, avoid some boilerplate code
 * in case the link is really just
 *
 * @author Andrea Aime - OpenGeo
 */
@SuppressWarnings("serial")
public abstract class SimpleAjaxLink<T> extends Panel {
    AjaxLink<T> link;
    Label label;

    public SimpleAjaxLink(String id) {
        this(id, null);
    }

    public SimpleAjaxLink(String id, IModel<T> model) {
        this(id, model, model);
    }

    public SimpleAjaxLink(String id, IModel<T> linkModel, IModel<?> labelModel) {
        super(id, linkModel);

        add(link = buildAjaxLink(linkModel));
        link.add(label = new Label("label", labelModel));
    }

    protected AjaxLink<T> buildAjaxLink(IModel<T> linkModel) {
        return new AjaxLink<T>("link", linkModel) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                SimpleAjaxLink.this.onClick(target);
            }

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                attributes.getAjaxCallListeners().add(SimpleAjaxLink.this.getAjaxListener());
            }
        };
    }

    protected IAjaxCallListener getAjaxListener() {
        return null;
    }

    public AjaxLink<T> getLink() {
        return link;
    }

    public Label getLabel() {
        return label;
    }

    public T getModelObject() {
        return link.getModelObject();
    }

    public IModel<T> getModel() {
        return link.getModel();
    }

    /** Subclasses should override and provide the behaviour for */
    protected abstract void onClick(AjaxRequestTarget target);
}
