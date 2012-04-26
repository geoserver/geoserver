/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss.service;

/**
 * GSS GetCapabilities request object.
 * 
 * @author Gabriel Roldan
 * 
 */
public class GetCapabilities extends BaseRequest {

    public GetCapabilities() {
        super("GetCapabilities");
    }

}
