/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geopkg.wps;

/** Represents a generalized table */
public class GeneralizedTable {

    String primaryTable;
    String generalizedTable;
    String provenance;
    double distance;

    public GeneralizedTable(String primaryTable, String generalizedTable, double distance) {
        this.primaryTable = primaryTable;
        this.generalizedTable = generalizedTable;
        this.distance = distance;
    }

    public String getGeneralizedTable() {
        return generalizedTable;
    }

    public void setGeneralizedTable(String generalizedTable) {
        this.generalizedTable = generalizedTable;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public String getPrimaryTable() {
        return primaryTable;
    }

    public void setPrimaryTable(String primaryTable) {
        this.primaryTable = primaryTable;
    }

    public String getProvenance() {
        return provenance;
    }

    public void setProvenance(String provenance) {
        this.provenance = provenance;
    }
}
