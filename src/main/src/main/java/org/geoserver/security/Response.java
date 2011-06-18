package org.geoserver.security;

/**
 * The response to be used when the user tries to go beyond the level
 * that he's authorized to see
 */
public enum Response {
    HIDE,
    CHALLENGE
}