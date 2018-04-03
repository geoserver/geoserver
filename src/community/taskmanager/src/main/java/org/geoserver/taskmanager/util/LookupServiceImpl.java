/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.util;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class LookupServiceImpl<T extends Named> implements LookupService<T> {
    
    private Map<String, T> map = new TreeMap<String, T>();

    @Override
    public T get(String name) {
        return map.get(name);
    }
    
    protected void setNamed(List<T> list) {
        for (T o : list) {
           map.put(o.getName(), o);
        }
    }

    @Override
    public Set<String> names() {
        return map.keySet();
    }

}
