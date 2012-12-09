/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

/**
 * The kind of access we can give the user for a given resource
 */
public enum AccessLevel {
    HIDDEN,
    METADATA,
    READ_ONLY,
    READ_WRITE
}