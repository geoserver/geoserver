/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

/**
 * The response to be used when the user tries to go beyond the level
 * that he's authorized to see
 */
public enum Response {
    HIDE,
    CHALLENGE
}
