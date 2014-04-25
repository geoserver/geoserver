package org.geoserver.extension.wfsng;

import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterInitializer;
import org.geotools.data.wfs.internal.v2_0.storedquery.ParameterMappingDefaultValue;
import org.geotools.data.wfs.internal.v2_0.storedquery.ParameterMappingExpressionValue;
import org.geotools.data.wfs.internal.v2_0.storedquery.StoredQueryConfiguration;

public class WfsNgXStreamPersisterInitializer implements
		XStreamPersisterInitializer {

	@Override
	public void init(XStreamPersister persister) {
		persister.getXStream().alias("storedQueryConfiguration", StoredQueryConfiguration.class);
		persister.getXStream().alias("storedQueryParameterMappingExpressionValue", ParameterMappingExpressionValue.class);
		persister.getXStream().alias("storedQueryParameterMappingDefaultValue", ParameterMappingDefaultValue.class);
		
		persister.registerBreifMapComplexType("storedQueryConfiguration", StoredQueryConfiguration.class);
	}

}
