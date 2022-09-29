/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.server.rest.xml;

import java.util.LinkedList;
import java.util.List;

/** This class represent a Batch operation to be consumed by the rest endpoint /batch/exec. */
public class Batch {

    private List<BatchOperation> operations;

    public Batch() {
        operations = new LinkedList<>();
    }

    /** @return the list of operations. */
    public List<BatchOperation> getOperations() {
        return operations;
    }

    /**
     * Sets the list of operations.
     *
     * @param operations the list of operations.
     */
    public void setOperations(List<BatchOperation> operations) {
        this.operations = operations;
    }

    /**
     * Add an operation to the list.
     *
     * @param op the {@link BatchOperation} to add.
     */
    public void add(BatchOperation op) {
        operations.add(op);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + operations.size() + " ops]";
    }
}
