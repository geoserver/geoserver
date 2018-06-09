/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.csw.kvp;

import net.opengis.cat.csw20.Csw20Factory;
import org.geoserver.ows.kvp.EMFKvpRequestReader;

/**
 * CSW KVP Request Reader base class
 *
 * @author Andrea Aime - GeoSolutions
 */
public class CSWKvpRequestReader extends EMFKvpRequestReader {

    public CSWKvpRequestReader(Class<?> requestBean) {
        super(requestBean, Csw20Factory.eINSTANCE);
    }

    protected Csw20Factory getCSW20Factory() {
        return (Csw20Factory) factory;
    }
}
