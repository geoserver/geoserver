/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

/** Used by StyleHandler and StyleGenerator to specify which template to use */
public enum StyleType {
    POINT("Point"), // Point Feature Type
    LINE("Line"), // Line Feature Type
    POLYGON("Polygon"), // Polygon Feature Type
    RASTER("Raster"), // Coverage Type
    GENERIC("Generic"); // Unknown Type

    private final String name;

    private StyleType(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }
}
