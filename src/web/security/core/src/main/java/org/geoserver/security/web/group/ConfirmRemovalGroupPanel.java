/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.group;

import java.util.List;
import java.util.SortedSet;
import org.apache.wicket.model.Model;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.security.web.AbstractConfirmRemovalPanel;
import org.geoserver.web.GeoServerApplication;

public class ConfirmRemovalGroupPanel extends AbstractConfirmRemovalPanel<GeoServerUserGroup> {

    private static final long serialVersionUID = 1L;

    public ConfirmRemovalGroupPanel(
            String id, Model<Boolean> model, List<GeoServerUserGroup> roots) {
        super(id, model, roots);
    }

    public ConfirmRemovalGroupPanel(String id, Model<Boolean> model, GeoServerUserGroup... roots) {
        super(id, model, roots);
    }

    @Override
    protected String getConfirmationMessage(GeoServerUserGroup object) throws Exception {
        StringBuffer buffer =
                new StringBuffer(OwsUtils.property(object, "groupname", String.class));
        if ((Boolean) getDefaultModelObject()) {
            SortedSet<GeoServerRole> roles =
                    GeoServerApplication.get()
                            .getSecurityManager()
                            .getActiveRoleService()
                            .getRolesForGroup(object.getGroupname());
            buffer.append(" [");
            for (GeoServerRole role : roles) {
                buffer.append(role.getAuthority()).append(" ");
            }
            if (roles.size() > 0) { // remove last delimiter
                buffer.setLength(buffer.length() - 1);
            }
            buffer.append("]");
        }
        return buffer.toString();
    }
}
