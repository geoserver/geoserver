/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */

package org.geoserver.security.password;

/**
 * Enumeration for password encoding type.
 * <p>
 * <ul>
 * <li>{@link #NULL} - null, raw password maintained</li>
 * <li>{@link #EMPTY} - empty, only for null or empty ("") passwords</li>
 * <li>{@link #PLAIN} -  plain text</li>
 * <li>{@link #ENCRYPT} - symmetric encryption</li>
 * <li>{@link #DIGEST} - password hashing (recommended)</li>
 *</p>
 * @author christian
 *
 */
public enum PasswordEncodingType {
    NULL,EMPTY,PLAIN,ENCRYPT,DIGEST;

}
