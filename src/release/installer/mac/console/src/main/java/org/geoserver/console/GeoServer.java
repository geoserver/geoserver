/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.console;

import org.geoserver.console.GeoServerConsole.DebugHandler;
import org.geoserver.console.GeoServerConsole.Handler;
import org.geoserver.console.GeoServerConsole.ProductionHandler;

/**
 * Entrance point to {@link GeoServerConsole} application.
 * 
 * @author Justin Deoliveira, OpenGEO
 */
public class GeoServer {

    public static void main(String[] args) throws Exception {
        // System.out.println(System.getProperty("user.dir"));
        System.out.println(System.getProperty("java.class.path"));
        Handler h = null;
        for ( int i = 0; args != null && i < args.length; i++) {
            if ( "--debug".equalsIgnoreCase( args[i] ) ) {
                h = new DebugHandler();
            }
        }
        if ( h == null ) {
            h = new ProductionHandler();
        }
        GeoServerConsole console = new GeoServerConsole( h );
    }
}
