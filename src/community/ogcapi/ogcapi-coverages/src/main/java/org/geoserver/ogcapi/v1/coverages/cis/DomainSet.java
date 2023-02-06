/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.coverages.cis;

/**
 * The domainSet describes the direct positions of the coverage, i.e., the locations for which
 * values are available.
 */
public class DomainSet {

    String type = "DomainSetType";
    GeneralGrid generalGrid;

    public DomainSet(GeneralGrid generalGrid) {
        this.generalGrid = generalGrid;
    }

    public GeneralGrid getGeneralGrid() {
        return generalGrid;
    }

    public String getType() {
        return type;
    }
}
