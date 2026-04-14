/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.web;

public enum DataIcon {
    FOLDER("gs-icon-folder"),
    VECTOR("gs-icon-vector"),
    RASTER("gs-icon-raster"),
    FILE("gs-icon-page-white-text"),
    FILE_VECTOR("gs-icon-page-white-vector"),
    FILE_RASTER("gs-icon-page-white-raster"),
    DATABASE("gs-icon-database-vector"),
    POSTGIS("gs-icon-postgis");

    final String cssClass;

    DataIcon(String cssClass) {
        this.cssClass = cssClass;
    }

    public String getIcon() {
        return cssClass;
    }
}
