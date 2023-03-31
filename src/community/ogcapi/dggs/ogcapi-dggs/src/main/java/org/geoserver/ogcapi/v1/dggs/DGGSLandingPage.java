/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.dggs;

import org.geoserver.catalog.Catalog;
import org.geoserver.ogcapi.AbstractLandingPageDocument;
import org.geoserver.ogcapi.Link;
import org.geoserver.ogcapi.LinksBuilder;

public class DGGSLandingPage extends AbstractLandingPageDocument {

    public DGGSLandingPage(DGGSInfo dggs, Catalog catalog, String dggsBase) {
        super(
                (dggs.getTitle() == null) ? "DGGS 1.0 server" : dggs.getTitle(),
                (dggs.getAbstract() == null) ? "" : dggs.getAbstract(),
                dggsBase);

        // collections
        new LinksBuilder(CollectionsDocument.class, dggsBase)
                .segment("/collections")
                .title("Collections Metadata as ")
                .rel(Link.REL_DATA)
                .add(this);
    }
}
