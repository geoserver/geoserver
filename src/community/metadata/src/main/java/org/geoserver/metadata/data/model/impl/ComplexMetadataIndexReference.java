/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.model.impl;

import java.io.Serializable;

public class ComplexMetadataIndexReference implements Serializable {

    private static final long serialVersionUID = -893882182247353028L;

    private int[] index;

    public ComplexMetadataIndexReference(int[] index) {
        this.index = index;
    }

    public int[] getIndex() {
        return index;
    }

    public void setIndex(int[] index) {
        this.index = index;
    }
}
