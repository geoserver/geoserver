/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.data;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.security.web.AbstractConfirmRemovalPanel;

public class ConfirmRemovalDataAccessRulePanel extends AbstractConfirmRemovalPanel<DataAccessRule> {

    private static final long serialVersionUID = 1L;

    public ConfirmRemovalDataAccessRulePanel(String id, List<DataAccessRule> roots) {
        super(id, roots);
    }

    public ConfirmRemovalDataAccessRulePanel(String id, DataAccessRule... roots) {
        this(id, Arrays.asList(roots));
    }

    @Override
    protected String getConfirmationMessage(DataAccessRule object) throws Exception {
        return OwsUtils.property(object, "root", String.class)
                + "."
                + OwsUtils.property(object, "layer", String.class)
                + "."
                + OwsUtils.property(object, "accessMode", String.class)
                + "="
                + OwsUtils.property(object, "roles", Set.class);
    }
}
