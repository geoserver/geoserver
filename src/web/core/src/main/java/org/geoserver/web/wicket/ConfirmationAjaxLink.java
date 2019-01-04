/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.core.util.string.JavaScriptUtils;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * A {@link SimpleAjaxLink} that asks a confirmation by using a Javascript confirm dialog before
 * carrying out its job
 */
@SuppressWarnings("serial")
public abstract class ConfirmationAjaxLink<T> extends SimpleAjaxLink<T> {
    IModel<String> confirm;

    public ConfirmationAjaxLink(String id, IModel<T> linkModel, String label, String confirm) {
        this(id, linkModel, new Model<String>(label), new Model<String>(confirm));
    }

    public ConfirmationAjaxLink(
            String id, IModel<T> linkModel, IModel<String> labelModel, IModel<String> confirm) {
        super(id, linkModel, labelModel);
        this.confirm = confirm;
    }

    @Override
    protected AjaxLink<T> buildAjaxLink(IModel<T> linkModel) {
        return new AjaxLink<T>("link", linkModel) {

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                attributes
                        .getAjaxCallListeners()
                        .add(
                                new AjaxCallListener() {
                                    @Override
                                    public CharSequence getPrecondition(Component component) {
                                        CharSequence message =
                                                JavaScriptUtils.escapeQuotes(confirm.getObject());
                                        return "if(!confirm('" + message + "')) return false;";
                                    }
                                });
            }

            @Override
            public void onClick(AjaxRequestTarget target) {
                ConfirmationAjaxLink.this.onClick(target);
            }
        };
    }
}
