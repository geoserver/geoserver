/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.kvp;

/**
 * Enumeration of supported interpolation methods 
 * @author Andrea Aime
 */
public enum InterpolationMethod {
    nearest, linear, quadratic, cubic, none;
}
