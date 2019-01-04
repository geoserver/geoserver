/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.auth;

import java.util.Arrays;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geoserver.security.config.J2eeAuthenticationBaseFilterConfig;
import org.geoserver.security.config.J2eeAuthenticationBaseFilterConfig.J2EERoleSource;
import org.geoserver.security.config.RoleSource;

/**
 * Base Configuration panel for J2EE supporting filters
 *
 * @author Mauro Bartolomeoli (mauro.bartolomeoli@geo-solutions.it)
 */
public abstract class J2eeBaseAuthFilterPanel<T extends J2eeAuthenticationBaseFilterConfig>
        extends PreAuthenticatedUserNameFilterPanel<T> {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    public J2eeBaseAuthFilterPanel(String id, IModel<T> model) {
        super(id, model);
    }

    @Override
    protected void addRoleSourceDropDown(WebMarkupContainer container, RoleSource rs) {
        if (J2EERoleSource.J2EE.equals(rs)) {
            container.addOrReplace(new RoleServicePanel("panel"));
        } else {
            super.addRoleSourceDropDown(container, rs);
        }
    }

    @Override
    protected Panel getRoleSourcePanel(RoleSource model) {
        if (J2EERoleSource.J2EE.equals(model)) {
            return new RoleServicePanel("panel");
        } else {
            return super.getRoleSourcePanel(model);
        }
    }

    @Override
    protected void createRoleSourceDropDown() {
        add(
                roleSourceChoice =
                        new DropDownChoice<RoleSource>(
                                "roleSource",
                                Arrays.asList(J2EERoleSource.values()),
                                new RoleSourceChoiceRenderer()));
    }
}
