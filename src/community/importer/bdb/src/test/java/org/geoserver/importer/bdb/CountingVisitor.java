package org.geoserver.importer.bdb;

import org.geoserver.importer.ImportContext;
import org.geoserver.importer.ImportStore.ImportVisitor;

class CountingVisitor implements ImportVisitor {

    int count = 0;

    public void visit(ImportContext context) {
        count++;
    }

    public int getCount() {
        return count;
    }
}
