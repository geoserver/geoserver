/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps.kvp;

import net.opengis.wps10.Wps10Factory;
import org.geoserver.ows.kvp.EMFKvpRequestReader;

/**
 * WPS KVP Request Reader
 *
 * @author Lucas Reed, Refractions Research Inc
 */
public class WPSKvpRequestReader extends EMFKvpRequestReader {
    public WPSKvpRequestReader(Class<?> requestBean) {
        super(requestBean, Wps10Factory.eINSTANCE);
    }

    protected Wps10Factory getWps10Factory() {
        return (Wps10Factory) factory;
    }
}
