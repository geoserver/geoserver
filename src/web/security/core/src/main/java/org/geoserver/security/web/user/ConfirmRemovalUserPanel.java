/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.user;

import java.util.List;
import java.util.SortedSet;
import org.apache.wicket.model.Model;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.web.AbstractConfirmRemovalPanel;
import org.geoserver.web.GeoServerApplication;

public class ConfirmRemovalUserPanel extends AbstractConfirmRemovalPanel<GeoServerUser> {

    private static final long serialVersionUID = 1L;

    public ConfirmRemovalUserPanel(String id, Model<Boolean> model, List<GeoServerUser> roots) {
        super(id, model, roots);
    }

    public ConfirmRemovalUserPanel(String id, Model<Boolean> model, GeoServerUser... roots) {
        super(id, model, roots);
    }

    @Override
    protected String getConfirmationMessage(GeoServerUser object) throws Exception {
        StringBuffer buffer = new StringBuffer(OwsUtils.property(object, "username", String.class));
        if ((Boolean) getDefaultModelObject()) {
            SortedSet<GeoServerRole> roles =
                    GeoServerApplication.get()
                            .getSecurityManager()
                            .getActiveRoleService()
                            .getRolesForUser(object.getUsername());
            buffer.append(" [");
            for (GeoServerRole role : roles) {
                buffer.append(role.getAuthority());
                buffer.append(" ");
            }
            if (buffer.length() > 0) { // remove last delimiter
                buffer.setLength(buffer.length() - 1);
            }
            buffer.append("]");
        }
        return buffer.toString();
    }
}
