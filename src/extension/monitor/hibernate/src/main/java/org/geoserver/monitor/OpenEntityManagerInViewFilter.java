/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */

package org.geoserver.monitor;

import org.geoserver.filters.GeoServerFilter;
/**
 * Makes the JPA {@link OpenEntityManagerInViewFilter} be picked up by the {@link SpringDelegatingFilter}
 * @author Mark Paxton
 */
public class OpenEntityManagerInViewFilter extends
	org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter implements GeoServerFilter {

}
