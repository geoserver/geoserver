/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web;

import java.io.Serial;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.geoserver.security.WebServiceBodyResponseUserGroupServiceConfig;
import org.geoserver.security.web.role.RoleServiceChoice;
import org.geoserver.security.web.usergroup.UserGroupServicePanel;
import org.geoserver.web.wicket.HelpLink;

/** @author Alessio Fabiani, GeoSolutions S.A.S. */
public class WebServiceBodyResponseUserGroupServicePanel
        extends UserGroupServicePanel<WebServiceBodyResponseUserGroupServiceConfig> {

    /** serialVersionUID */
    @Serial
    private static final long serialVersionUID = -5204330095571014979L;

    public WebServiceBodyResponseUserGroupServicePanel(
            String id, IModel<WebServiceBodyResponseUserGroupServiceConfig> model) {
        super(id, model);
        add(new HelpLink("webServiceBodyResponseUserGroupParametersHelp", this).setDialog(dialog));
        add(new TextField<String>("searchRoles").setRequired(true));
        add(new TextField<String>("availableGroups").setRequired(false));

        add(new RoleServiceChoice("roleServiceName"));
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        String css = " ul.horizontal div {\n" + "    display:inline;\n" + "  }";
        response.render(
                CssHeaderItem.forCSS(css, "org-geoserver-security-web-WebServiceBodyResponseUserGroupServicePanel"));
    }
}
