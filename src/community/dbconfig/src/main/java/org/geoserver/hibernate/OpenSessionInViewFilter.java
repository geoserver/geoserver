/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */

package org.geoserver.hibernate;

import org.geoserver.filters.GeoServerFilter;
import org.geoserver.filters.SpringDelegatingFilter;

/**
 * Makes the Hibernate {@link OpenSessionInViewFilter} be picked up by the {@link SpringDelegatingFilter}
 * @author Andrea Aime - GeoSolutions
 * 
 * TODO: This is copied from the monitoring module, merge them
 */
public class OpenSessionInViewFilter extends
        org.springframework.orm.hibernate3.support.OpenSessionInViewFilter implements GeoServerFilter {

}
