/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Jorge Gustavo Rocha / Universidade do Minho
 * @author Nuno Carvalho Oliveira / Universidade do Minho 
 */

package org.geoserver.w3ds.service;

import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.platform.GeoServerResourceLoader;

public class W3DSXStreamLoader extends XStreamServiceLoader<W3DSInfo> {

    public W3DSXStreamLoader(GeoServerResourceLoader resourceLoader) {
        super(resourceLoader, "w3ds");
    }

    @Override
    protected void initXStreamPersister(XStreamPersister xp, GeoServer gs) {
        xp.getXStream().alias( "w3ds", ServiceInfo.class, W3DSInfoImpl.class );
    }
    
    protected W3DSInfo createServiceFromScratch(GeoServer gs) {
    	
    	W3DSInfoImpl w3ds = new W3DSInfoImpl();
    	w3ds.setId("w3ds");
    	w3ds.setName("W3DS");
    	
        return w3ds;
    }

    public Class<W3DSInfo> getServiceClass() {
        return W3DSInfo.class;
    }
    
    @Override
    protected W3DSInfo initialize(W3DSInfo service) {
        super.initialize(service);
        return service;
    }

}
