/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Service;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.security.impl.ServiceAccessRule;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.security.RolesFormComponent;

/**
 * Abstract page binding a {@link DataAccessRule}
 */
@SuppressWarnings("serial")
public abstract class AbstractServiceAccessRulePage extends GeoServerSecuredPage {

    DropDownChoice service;

    DropDownChoice method;

    RolesFormComponent rolesForComponent;

    Form form;

    public AbstractServiceAccessRulePage(ServiceAccessRule rule) {
        setDefaultModel(new CompoundPropertyModel(new ServiceAccessRule(rule)));

        // build the form
        form = new Form("ruleForm");
        add(form);
        form.add(service = new DropDownChoice("service", getServiceNames()));
        service.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                method.setChoices(new Model(getMethod((String) service.getConvertedInput())));
                method.modelChanged();
                target.addComponent(method);
            }
        });
        setOutputMarkupId(true);
        form.add(method = new DropDownChoice("method", getMethod(rule.getService())));
        method.setOutputMarkupId(true);

        form.add(rolesForComponent = new RolesFormComponent("roles", new RolesModel(rule), form,
                true));

        // build the submit/cancel
        form.add(new BookmarkablePageLink("cancel", ServiceAccessRulePage.class));
        form.add(saveLink());

        // add the validators
        service.setRequired(true);
        method.setRequired(true);
    }

    SubmitLink saveLink() {
        return new SubmitLink("save") {
            @Override
            public void onSubmit() {
                onFormSubmit();
            }
        };
    }

    /**
     * Implements the actual save action
     */
    protected abstract void onFormSubmit();

    /**
     * Returns a sorted list of workspace names
     */
    ArrayList<String> getServiceNames() {
        ArrayList<String> result = new ArrayList<String>();
        for (Service ows : GeoServerExtensions.extensions(Service.class)) {
            if (!result.contains(ows.getId()))
                result.add(ows.getId());
        }
        Collections.sort(result);
        result.add(0, "*");

        return result;
    }

    /**
     * Returns a sorted list of layer names in the specified workspace (or * if the workspace is *)
     */
    ArrayList<String> getMethod(String service) {
        ArrayList<String> result = new ArrayList<String>();
        boolean flag = true;
        for (Service ows : GeoServerExtensions.extensions(Service.class)) {
            if (service.equals(ows.getId()) && !result.contains(ows.getOperations()) && flag) {
                flag = false;
                result.addAll(ows.getOperations());
            }
        }
        Collections.sort(result);
        result.add(0, "*");
        return result;
    }

    /**
     * Bridge between Set and List
     */
    static class RolesModel implements IModel {

        ServiceAccessRule rule;

        RolesModel(ServiceAccessRule rule) {
            this.rule = rule;
        }

        public Object getObject() {
            return new ArrayList<String>(rule.getRoles());
        }

        public void setObject(Object object) {
            rule.getRoles().clear();
            rule.getRoles().addAll((List<String>) object);
        }

        public void detach() {
            // nothing to do

        }

    }

}
