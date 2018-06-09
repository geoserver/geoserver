/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs;

import net.opengis.wcs11.DescribeCoverageType;
import net.opengis.wcs11.GetCapabilitiesType;
import net.opengis.wcs11.GetCoverageType;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.ows.Request;
import org.geoserver.ows.WorkspaceQualifyingCallback;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;

public class WCS11WorkspaceQualifier extends WorkspaceQualifyingCallback {

    public WCS11WorkspaceQualifier(Catalog catalog) {
        super(catalog);
    }

    @Override
    protected void qualifyRequest(
            WorkspaceInfo ws, PublishedInfo l, Service service, Request request) {}

    @Override
    protected void qualifyRequest(
            WorkspaceInfo ws, PublishedInfo l, Operation operation, Request request) {

        GetCapabilitiesType caps = parameter(operation, GetCapabilitiesType.class);
        if (caps != null) {
            return;
        }

        DescribeCoverageType dcov = parameter(operation, DescribeCoverageType.class);
        if (dcov != null) {
            qualifyLayerNames(dcov.getIdentifier(), ws);
            return;
        }

        GetCoverageType gcov = parameter(operation, GetCoverageType.class);
        if (gcov != null) {
            if (gcov.getIdentifier() != null && gcov.getIdentifier().getValue() != null) {
                gcov.getIdentifier().setValue(qualifyName(gcov.getIdentifier().getValue(), ws));
            }
        }
    }
}
