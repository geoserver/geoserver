/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss.config;

import org.geoserver.config.impl.ServiceInfoImpl;

/**
 * @see GSSInfo
 * 
 */
@SuppressWarnings("unchecked")
public class GSSInfoImpl extends ServiceInfoImpl implements GSSInfo {

    private static final long serialVersionUID = -7105447790034105261L;

    public GSSInfoImpl() {
        setId("gss");
        setName("GSS");
    }
}
