/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.auth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.RequestFilterChain;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.filter.GeoServerRoleFilter;
import org.geoserver.security.validation.SecurityConfigValidator;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.HelpLink;
import org.geotools.util.logging.Logging;

/**
 * Class for configuration panels of {@link RequestFilterChain} objects
 *
 * @author christan
 */
public class SecurityFilterChainPage extends AbstractSecurityPage {

    private static final long serialVersionUID = 1L;

    /** logger */
    protected static Logger LOGGER = Logging.getLogger("org.geoserver.web.security");

    protected RequestFilterChainWrapper chainWrapper;
    SecurityManagerConfig secMgrConfig;

    Form<? extends RequestFilterChainWrapper> form;
    CheckBox methodList[] = new CheckBox[7];;

    protected boolean isNew;

    public SecurityFilterChainPage(
            RequestFilterChain chain, SecurityManagerConfig secMgrConfig, boolean isNew) {

        RequestFilterChainWrapper wrapper = new RequestFilterChainWrapper(chain);
        Form<RequestFilterChainWrapper> theForm =
                new Form<RequestFilterChainWrapper>(
                        "form", new CompoundPropertyModel<RequestFilterChainWrapper>(wrapper));

        initialize(chain, secMgrConfig, isNew, theForm, wrapper);
    }

    protected SecurityFilterChainPage() {
        super();
    }

    protected void initialize(
            RequestFilterChain chain,
            SecurityManagerConfig secMgrConfig,
            boolean isNew,
            Form<? extends RequestFilterChainWrapper> theForm,
            RequestFilterChainWrapper wrapper) {

        this.chainWrapper = wrapper;
        this.isNew = isNew;
        this.secMgrConfig = secMgrConfig;

        form = theForm;
        add(form);

        // check for administrator, if not disable the panel and emit warning message
        boolean isAdmin = getSecurityManager().checkAuthenticationForAdminRole();
        setEnabled(isAdmin);

        form.add(
                new Label(
                        "message",
                        isAdmin ? new Model() : new StringResourceModel("notAdmin", this, null)));
        if (!isAdmin) {
            form.get("message").add(new AttributeAppender("class", new Model("info-link"), " "));
        }

        setOutputMarkupId(true);

        form.add(new TextField<String>("name").setEnabled(isNew));
        form.add(new TextField<String>("patternString"));
        form.add(new CheckBox("disabled"));
        form.add(new CheckBox("allowSessionCreation"));
        form.add(new CheckBox("requireSSL"));
        form.add(
                new CheckBox("matchHTTPMethod")
                        .add(
                                new OnChangeAjaxBehavior() {
                                    @Override
                                    protected void onUpdate(AjaxRequestTarget target) {
                                        for (CheckBox cb : methodList) {
                                            cb.setEnabled(chainWrapper.isMatchHTTPMethod());
                                            target.add(cb);
                                        }
                                    }
                                }));

        List<String> filterNames = new ArrayList<String>();
        try {
            filterNames.addAll(getSecurityManager().listFilters(GeoServerRoleFilter.class));
            for (GeoServerRoleFilter filter :
                    GeoServerExtensions.extensions(GeoServerRoleFilter.class)) {
                filterNames.add(filter.getName());
            }
            form.add(
                    new DropDownChoice<String>(
                                    "roleFilterName",
                                    new PropertyModel<String>(
                                            chainWrapper.getChain(), "roleFilterName"),
                                    filterNames)
                            .setNullValid(true));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        form.add(methodList[0] = new CheckBox("GET"));
        form.add(methodList[1] = new CheckBox("POST"));
        form.add(methodList[2] = new CheckBox("PUT"));
        form.add(methodList[3] = new CheckBox("DELETE"));
        form.add(methodList[4] = new CheckBox("OPTIONS"));
        form.add(methodList[5] = new CheckBox("HEAD"));
        form.add(methodList[6] = new CheckBox("TRACE"));

        for (CheckBox cb : methodList) {
            cb.setOutputMarkupPlaceholderTag(true);
            cb.setEnabled(chain.isMatchHTTPMethod());
        }

        form.add(dialog = new GeoServerDialog("dialog"));

        form.add(new HelpLink("chainConfigHelp").setDialog(dialog));
        form.add(new HelpLink("chainConfigMethodHelp").setDialog(dialog));

        form.add(
                new SubmitLink("close", form) {
                    @Override
                    public void onSubmit() {
                        handleSubmit(getForm());
                    }
                });
        form.add(
                new Link("cancel") {
                    @Override
                    public void onClick() {
                        doReturn();
                    }
                });
    }

    protected void handleSubmit(Form<?> form) {
        RequestFilterChain chain = chainWrapper.getChain();
        try {
            new SecurityConfigValidator(getSecurityManager())
                    .validateRequestFilterChain(chainWrapper.getChain());
            if (isNew) secMgrConfig.getFilterChain().getRequestChains().add(chain);
            // getSecurityManager().saveSecurityConfig(secMgrConfig);
            doReturn();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error saving config", e);
            error(e);
        }
    }

    protected void doReturn() {
        ((AuthenticationPage) returnPage).updateChainComponents();
        super.doReturn();
    };

    protected boolean isNew() {
        return isNew;
    }
}
