/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.geoserver.csw.store.CatalogStore;
import org.geoserver.platform.ServiceException;

/**
 * Runs the GetRepositoryItem request
 * 
 * @author Alessio Fabiani - GeoSolutions
 */
public class GetRepositoryItem {

    CSWInfo csw;

    CatalogStore store;

    public GetRepositoryItem(CSWInfo csw, CatalogStore store) {
        this.csw = csw;
        this.store = store;
    }

    /**
     * Returns the requested RepositoryItem
     * 
     * @param request
     * @return
     */
    public RepositoryItem run(GetRepositoryItemBean request) {
        try {
            return new RepositoryItem(){

                @Override
                public String getMime() {
                    return "application/xml";
                }

                @Override
                public InputStream getContents() {
                    String theString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Foo/>";
                    try {
                        return new ByteArrayInputStream(theString.getBytes("UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        throw new ServiceException(e, "Failed to parse the requested Repository Item",
                                ServiceException.NO_APPLICABLE_CODE);
                    }
                }};
        } catch (Exception e) {
            throw new ServiceException(e, "Failed to retrieve the requested Repository Item",
                    ServiceException.NO_APPLICABLE_CODE);
        }
    }
}
