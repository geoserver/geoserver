/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wfs;

import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterInitializer;
import org.geotools.data.wfs.internal.v2_0.storedquery.ParameterMappingBlockValue;
import org.geotools.data.wfs.internal.v2_0.storedquery.ParameterMappingDefaultValue;
import org.geotools.data.wfs.internal.v2_0.storedquery.ParameterMappingExpressionValue;
import org.geotools.data.wfs.internal.v2_0.storedquery.StoredQueryConfiguration;

/**
 * Configure XStreamPersisters for WFS
 * 
 * @author Sampo Savolainen (Spatineo)
 */
public class WFSXStreamPersisterInitializer implements
		XStreamPersisterInitializer {

	@Override
	public void init(XStreamPersister persister) {
		persister.getXStream().alias("storedQueryConfiguration", StoredQueryConfiguration.class);
		persister.getXStream().alias("storedQueryParameterMappingExpressionValue", ParameterMappingExpressionValue.class);
		persister.getXStream().alias("storedQueryParameterMappingDefaultValue", ParameterMappingDefaultValue.class);
		persister.getXStream().alias("storedQueryParameterMappingBlockValue", ParameterMappingBlockValue.class);
		
		persister.registerBreifMapComplexType("storedQueryConfiguration", StoredQueryConfiguration.class);
	}

}
