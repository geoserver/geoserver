/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.gwc.web.layer;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.util.visit.IVisitor;
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
    protected void convertInput() {
        visitChildren(new IVisitor<Component, IVisit<Void>>() {

            @Override
            public void component(Component c, IVisit<IVisit<Void>> visit) {
                if(c instanceof FormComponent<?>) {
                    FormComponent<?> fc = (FormComponent<?>) c;
                    fc.processInput();
                }
                
                
            }
        });
        T filter = getModelObject();
        setConvertedInput(filter);
    }

}
