/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.data.store.virtualfk;

/** Simple descriptor for the column used as join key in a virtual relationship. */
public class Key {
    private String column;

    /** @param column non-empty column name participating in the relationship */
    public Key(String column) {
        this.column = column;
    }

    /** Returns the column name used as join key. */
    public String getColumn() {
        return column;
    }

    /** Sets the column name used as join key. */
    public void setColumn(String column) {
        this.column = column;
    }
}
