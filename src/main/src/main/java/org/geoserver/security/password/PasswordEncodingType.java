/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.password;

/**
 * Enumeration for password encoding type.
 *
 * <p>
 *
 * <ul>
 *   <li>{@link #EMPTY} - empty, only for null or empty ("") passwords
 *   <li>{@link #PLAIN} - plain text
 *   <li>{@link #ENCRYPT} - symmetric encryption
 *   <li>{@link #DIGEST} - password hashing (recommended)
 *
 * @author christian
 */
public enum PasswordEncodingType {
    EMPTY,
    PLAIN,
    ENCRYPT,
    DIGEST;
}
