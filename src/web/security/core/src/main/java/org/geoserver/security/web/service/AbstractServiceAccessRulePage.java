/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.validation.AbstractFormValidator;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Service;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.security.impl.ServiceAccessRule;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.security.web.role.RuleRolesFormComponent;
import org.geoserver.web.wicket.ParamResourceModel;

/** Abstract page binding a {@link DataAccessRule} */
@SuppressWarnings("serial")
public abstract class AbstractServiceAccessRulePage extends AbstractSecurityPage {

    protected DropDownChoice<String> serviceChoice, methodChoice;
    protected RuleRolesFormComponent rolesFormComponent;

    public AbstractServiceAccessRulePage(final ServiceAccessRule rule) {

        // build the form
        Form form = new Form<Serializable>("form", new CompoundPropertyModel(rule));
        add(form);
        form.add(new EmptyRolesValidator());

        form.add(serviceChoice = new DropDownChoice<String>("service", getServiceNames()));
        serviceChoice.add(
                new OnChangeAjaxBehavior() {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        methodChoice.updateModel();
                        target.add(methodChoice);
                    }
                });
        serviceChoice.setRequired(true);

        form.add(methodChoice = new DropDownChoice<String>("method", new MethodsModel(rule)));

        // we add on change behavior to ensure the underlying model is updated but don't actually
        // do anything on change... this allows us to keep state when someone adds a new role,
        // leaving the page, TODO: find a better way to do this
        // methodChoice.add(new OnChangeAjaxBehavior() {
        //    @Override
        //    protected void onUpdate(AjaxRequestTarget target) {}
        // });
        methodChoice.setOutputMarkupId(true);
        methodChoice.setRequired(true);

        form.add(
                rolesFormComponent =
                        new RuleRolesFormComponent(
                                "roles", new PropertyModel<Collection<String>>(rule, "roles")));
        // new Model((Serializable)new ArrayList(rule.getRoles()))));

        // build the submit/cancel
        form.add(
                new SubmitLink("save") {
                    @Override
                    public void onSubmit() {
                        onFormSubmit((ServiceAccessRule) getForm().getModelObject());
                    }
                });
        form.add(new BookmarkablePageLink("cancel", ServiceAccessRulePage.class));
    }

    /** Implements the actual save action */
    protected abstract void onFormSubmit(ServiceAccessRule rule);

    /** Returns a sorted list of workspace names */
    ArrayList<String> getServiceNames() {
        ArrayList<String> result = new ArrayList<String>();
        for (Service ows : GeoServerExtensions.extensions(Service.class)) {
            if (!result.contains(ows.getId())) result.add(ows.getId());
        }
        Collections.sort(result);
        result.add(0, "*");

        return result;
    }

    class EmptyRolesValidator extends AbstractFormValidator {

        @Override
        public FormComponent<?>[] getDependentFormComponents() {
            return new FormComponent[] {rolesFormComponent};
        }

        @Override
        public void validate(Form<?> form) {
            // only validate on final submit
            if (form.findSubmittingButton() != form.get("save")) {
                return;
            }
            updateModels();
            String roleInputString =
                    rolesFormComponent.getPalette().getRecorderComponent().getInput();
            if ((roleInputString == null || roleInputString.trim().isEmpty())
                    && !rolesFormComponent.isHasAnyRole()) {
                form.error(new ParamResourceModel("emptyRoles", getPage()).getString());
            }
        }
    }

    class MethodsModel implements IModel<List<String>> {

        ServiceAccessRule rule;

        MethodsModel(ServiceAccessRule rule) {
            this.rule = rule;
        }

        @Override
        public List<String> getObject() {
            List<String> result = new ArrayList<String>();
            for (Service ows : GeoServerExtensions.extensions(Service.class)) {
                String service = rule.getService();
                if (ows.getId().equals(service)) {
                    for (String operation : ows.getOperations()) {
                        if (!result.contains(operation)) {
                            result.add(operation);
                        }
                    }
                }
            }
            Collections.sort(result);
            result.add(0, "*");
            return result;
        }

        @Override
        public void setObject(List<String> object) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void detach() {}
    }

    protected void updateModels() {
        serviceChoice.updateModel();
        methodChoice.updateModel();
        rolesFormComponent.updateModel();
    }
}
