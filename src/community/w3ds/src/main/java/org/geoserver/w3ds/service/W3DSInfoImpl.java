/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Jorge Gustavo Rocha / Universidade do Minho
 * @author Nuno Carvalho Oliveira / Universidade do Minho 
 */

package org.geoserver.w3ds.service;

import org.geoserver.config.impl.ServiceInfoImpl;

public class W3DSInfoImpl extends ServiceInfoImpl implements W3DSInfo {

    public W3DSInfoImpl() {
        setId( "w3ds" );
    }
    
}
