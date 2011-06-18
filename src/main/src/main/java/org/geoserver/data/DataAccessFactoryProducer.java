/* Copyright (c) 2001 - 2010 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.data;

import java.util.List;

import org.geotools.data.DataAccessFactory;

/**
 * Provider of data access factories.
 * <p>
 * This extension point allows for the addition of data access factories outside of 
 * the geotools spi framework.
 * </p>
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public interface DataAccessFactoryProducer {

    /**
     * Returns the list of factories.  
     */
    List<DataAccessFactory> getDataStoreFactories();
}
