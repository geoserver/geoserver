/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A list that is meant to contain either {@link java.util.Date} or {@link
 * org.geotools.util.DateRange} objects. Currently does not check its contents, it's used as a type
 * target for MVC converters only.
 */
public class DateTimeList extends ArrayList<Object> {

    public DateTimeList(int initialCapacity) {
        super(initialCapacity);
    }

    public DateTimeList() {}

    public DateTimeList(Collection<?> c) {
        super(c);
    }
}
