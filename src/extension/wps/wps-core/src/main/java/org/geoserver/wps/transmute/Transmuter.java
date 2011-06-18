/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps.transmute;

/**
 * Root transmuter interface
 *
 * @author Lucas Reed, Refractions Research Inc
 */
public interface Transmuter {
    /**
     * Returns the Java type the transmuter decodes to
     */
    Class<?> getType();
}