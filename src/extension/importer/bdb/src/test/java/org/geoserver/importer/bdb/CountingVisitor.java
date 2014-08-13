/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
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
