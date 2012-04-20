/* Copyright (c) 2001, 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.map.quantize;

import java.awt.image.IndexColorModel;

/**
 * Wraps around another palette and adds last match caching. This speeds up significantly lookups on
 * maps that have large areas with constant color
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class CachingColorIndexer implements ColorIndexer {
    ColorIndexer delegate;

    int lr, lg, lb, la;

    int idx = -1;

    public CachingColorIndexer(ColorIndexer delegate) {
        this.delegate = delegate;
    }

    @Override
    public IndexColorModel toIndexColorModel() {
        return delegate.toIndexColorModel();
    }

    @Override
    public int getClosestIndex(int r, int g, int b, int a) {
        synchronized (this) {
            if (r == lr && g == lg && b == lb && a == la && idx >= 0) {
                return idx;
            }
        }

        int delegateIdx = delegate.getClosestIndex(r, g, b, a);

        synchronized (this) {
            lr = r;
            lg = g;
            lb = b;
            la = a;
            idx = delegateIdx;
        }

        return delegateIdx;
    }

}
