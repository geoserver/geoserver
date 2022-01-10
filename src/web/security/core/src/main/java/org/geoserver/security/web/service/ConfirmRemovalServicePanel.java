/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.security.impl.ServiceAccessRule;
import org.geoserver.security.web.AbstractConfirmRemovalPanel;

public class ConfirmRemovalServicePanel extends AbstractConfirmRemovalPanel<ServiceAccessRule> {

    private static final long serialVersionUID = 1L;

    public ConfirmRemovalServicePanel(String id, List<ServiceAccessRule> roots) {
        super(id, roots);
    }

    public ConfirmRemovalServicePanel(String id, ServiceAccessRule... roots) {
        this(id, Arrays.asList(roots));
    }

    @Override
    protected String getConfirmationMessage(ServiceAccessRule object) throws Exception {
        return OwsUtils.property(object, "service", String.class)
                + "."
                + OwsUtils.property(object, "method", String.class)
                + "="
                + OwsUtils.property(object, "roles", Set.class);
    }
}
