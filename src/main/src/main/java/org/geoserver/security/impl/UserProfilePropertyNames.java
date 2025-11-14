/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

/**
 * Common user profile property names used in {@link GeoServerUser#getProperties()}.
 *
 * <p>These keys standardize access to user attributes such as e-mail and display name, typically provided by
 * authentication providers.
 */
public abstract class UserProfilePropertyNames {

    /** The user's first name (given name). */
    public static final String FIRST_NAME = "first_name";

    /** The user's last name (family name). */
    public static final String LAST_NAME = "last_name";

    /** The user's preferred username (from external provider). */
    public static final String PREFERRED_USERNAME = "preferred_username";

    /** The user's e-mail address. */
    public static final String EMAIL = "email";
}
