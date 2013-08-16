/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map.quantize;

import java.awt.image.IndexColorModel;

/**
 * A tool transforming a generic RGBA color into an index into a palette represented by a
 * IndexedColorModel
 * 
 * @author Andrea Aime - GeoSolutions
 */
public interface ColorIndexer {

    public IndexColorModel toIndexColorModel();

    public int getClosestIndex(int r, int g, int b, int a);
}
