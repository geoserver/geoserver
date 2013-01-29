/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
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
