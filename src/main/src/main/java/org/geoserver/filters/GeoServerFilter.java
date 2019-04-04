/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.filters;

import javax.servlet.Filter;

/**
 * Marks all filters that {@link SpringDelegatingFilter} will pick up from the Spring application
 * context and run against each servlet request
 *
 * @author Andrea Aime - GeoSolutions
 */
public interface GeoServerFilter extends Filter {}
