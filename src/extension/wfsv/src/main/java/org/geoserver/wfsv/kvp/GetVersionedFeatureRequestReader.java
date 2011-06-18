package org.geoserver.wfsv.kvp;

import net.opengis.wfs.WfsFactory;
import net.opengis.wfsv.GetVersionedFeatureType;
import net.opengis.wfsv.WfsvFactory;

import org.geoserver.catalog.Catalog;
import org.geoserver.wfs.kvp.GetFeatureKvpRequestReader;
import org.opengis.filter.FilterFactory;

public class GetVersionedFeatureRequestReader extends GetFeatureKvpRequestReader {

    public GetVersionedFeatureRequestReader(Catalog catalog,
            FilterFactory filterFactory) {
        super(GetVersionedFeatureType.class, catalog, filterFactory);
        factory = WfsvFactory.eINSTANCE;
    }
    
    protected WfsFactory getWfsFactory() {
        return WfsFactory.eINSTANCE;
    }

}
