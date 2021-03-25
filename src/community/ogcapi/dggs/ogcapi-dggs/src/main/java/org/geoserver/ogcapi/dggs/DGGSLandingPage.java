/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.dggs;

import org.geoserver.catalog.Catalog;
import org.geoserver.ogcapi.AbstractLandingPageDocument;
import org.geoserver.ogcapi.Link;

public class DGGSLandingPage extends AbstractLandingPageDocument {

    public DGGSLandingPage(DGGSInfo dggs, Catalog catalog, String dggsBase) {
        super(
                (dggs.getTitle() == null) ? "DGGS 1.0 server" : dggs.getTitle(),
                (dggs.getAbstract() == null) ? "" : dggs.getAbstract(),
                dggsBase);

        // collections
        addLinksFor(
                dggsBase + "/collections",
                CollectionsDocument.class,
                "Collections Metadata as ",
                "collections",
                null,
                Link.REL_DATA);
    }
}
