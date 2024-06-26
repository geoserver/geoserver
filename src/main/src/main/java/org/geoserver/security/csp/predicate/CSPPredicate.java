/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.csp.predicate;

import java.util.function.Predicate;
import org.geoserver.security.csp.CSPHttpRequestWrapper;

/** Predicate interfaces to test {@link CSPHttpRequestWrapper} objects. */
public interface CSPPredicate extends Predicate<CSPHttpRequestWrapper> {}
