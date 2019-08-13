/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Annotates controller service methods to indicate the freemarker template to be used */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface HTMLResponseBody {
    String templateName();

    String fileName();
}
