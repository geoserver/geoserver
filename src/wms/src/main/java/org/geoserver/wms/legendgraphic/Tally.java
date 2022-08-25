/* (c) 2014 - 2022 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2022 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

public class Tally {

    private int maxMemory;

    private int usedMemory;

    public Tally(int maxMemory) {
        this.maxMemory = maxMemory;
        this.usedMemory = 0;
    }

    public int getMaxMemory() {
        return maxMemory;
    }

    public void setMaxMemory(int maxMemory) {
        this.maxMemory = maxMemory;
    }

    public int getUsedMemory() {
        return usedMemory;
    }

    public void setUsedMemory(int usedMemory) {
        this.usedMemory = usedMemory;
    }
}
