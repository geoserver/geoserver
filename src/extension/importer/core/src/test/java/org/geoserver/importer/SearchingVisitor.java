/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

public class SearchingVisitor implements ImportStore.ImportVisitor {
    long id;
    boolean found = false;

    public SearchingVisitor(long id) {
        this.id = id;
    }

    public void visit(ImportContext context) {
        if (context.getId().longValue() == id) {
            found = true;
        }
    }

    public boolean isFound() {
        return found;
    }
}
