/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.gwc.web.layer;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.model.IModel;
import org.geowebcache.filter.parameters.ParameterFilter;

/**
 * Subform for a ParameterFilter
 * @author Kevin Smith, OpenGeo
 *
 */
public abstract class AbstractParameterFilterSubform<T extends ParameterFilter> extends FormComponentPanel<T> {


    /** serialVersionUID */
    private static final long serialVersionUID = 1L;


    public AbstractParameterFilterSubform(String id,
            IModel<T> model) {
        super(id, model);
        

    }

    
    @Override
    public void convertInput() {
        visitChildren(new Component.IVisitor<Component>() {

            @Override
            public Object component(Component component) {
                if (component instanceof FormComponent) {
                    FormComponent<?> formComponent = (FormComponent<?>) component;
                    formComponent.processInput();
                }
                return Component.IVisitor.CONTINUE_TRAVERSAL;
            }
        });
        T filter = getModelObject();
        setConvertedInput(filter);
    }

}
