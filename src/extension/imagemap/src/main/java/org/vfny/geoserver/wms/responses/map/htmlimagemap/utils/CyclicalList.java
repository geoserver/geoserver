/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.wms.responses.map.htmlimagemap.utils;
/**
 * Cyclical implementation of ArrayList: when a not available index is used, then index is wrapped
 * until it falls in tre available index range.
 *
 * @author m.bartolomeoli
 */
public class CyclicalList<T> extends java.util.ArrayList<T> {

    private static final long serialVersionUID = 5468175713393008920L;

    protected int wrapIndex(int index) {
        // perform the index wrapping
        while (index < 0) index = size() + index;
        if (index >= size()) index %= size();
        return index;
    }

    @Override
    public T get(int index) {
        return super.get(wrapIndex(index));
    }

    @Override
    public T set(int index, T value) {
        return super.set(wrapIndex(index), value);
    }

    public CyclicalList() {}
}
