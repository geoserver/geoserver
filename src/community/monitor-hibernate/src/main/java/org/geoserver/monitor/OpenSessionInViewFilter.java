/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.monitor;

import org.geoserver.filters.GeoServerFilter;
import org.geoserver.filters.SpringDelegatingFilter;

/**
 * Makes the Hibernate {@link OpenSessionInViewFilter} be picked up by the {@link
 * SpringDelegatingFilter}
 *
 * @author Andrea Aime - GeoSolutions
 */
public class OpenSessionInViewFilter
        extends org.springframework.orm.hibernate3.support.OpenSessionInViewFilter
        implements GeoServerFilter {}
