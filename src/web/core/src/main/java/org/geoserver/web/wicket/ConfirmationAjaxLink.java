/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;


import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.IAjaxCallListener;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * A {@link SimpleAjaxLink} that asks a confirmation by using a Javascript confirm
 * dialog before carrying out its job
 */
@SuppressWarnings("serial")
public abstract class ConfirmationAjaxLink extends SimpleAjaxLink {
    IModel confirm;

    public ConfirmationAjaxLink(String id, IModel linkModel, String label, String confirm) {
        this( id, linkModel, new Model( label ), new Model( confirm ) );
    }
    
    public ConfirmationAjaxLink(String id, IModel linkModel, IModel labelModel,
            IModel confirm) {
        super(id, linkModel, labelModel);
        this.confirm = confirm;
    }

    @Override
    protected AjaxLink buildAjaxLink(IModel linkModel) {
        return new AjaxLink("link", linkModel) {

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                attributes.getAjaxCallListeners().add(new AjaxCallListener() {
                    @Override
                    public CharSequence getPrecondition(Component component) {
                        return "if(!confirm('" + confirm.getObject() + "')) return false;";
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
