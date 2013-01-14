/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.monitor;

import java.util.List;

public class Or extends CompositeFilter {
    public Or(Filter... filters) {
        super(filters);
    }

    public Or(List<Filter> filters) {
        super(filters);
    }
}
