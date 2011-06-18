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