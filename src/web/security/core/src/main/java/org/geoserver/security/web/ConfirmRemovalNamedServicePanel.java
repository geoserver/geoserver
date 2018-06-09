/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web;

import java.util.Arrays;
import java.util.List;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.security.config.SecurityNamedServiceConfig;

public class ConfirmRemovalNamedServicePanel<T extends SecurityNamedServiceConfig>
        extends AbstractConfirmRemovalPanel<T> {

    private static final long serialVersionUID = 1L;

    public ConfirmRemovalNamedServicePanel(String id, List<T> roots) {
        super(id, roots);
    }

    public ConfirmRemovalNamedServicePanel(String id, T... roots) {
        this(id, Arrays.asList(roots));
    }

    @Override
    protected String getConfirmationMessage(SecurityNamedServiceConfig object) throws Exception {
        return OwsUtils.property(object, "name", String.class);
    }
}
