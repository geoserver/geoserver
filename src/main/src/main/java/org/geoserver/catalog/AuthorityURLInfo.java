/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.io.Serializable;
import org.geoserver.catalog.impl.AuthorityURL;

/**
 * Support interface for publication of AuthorityURL elements in WMS capabilities, assigned to
 * either the root WMS layer or any individual layer, including layer gorups.
 *
 * @author groldan
 * @see AuthorityURL
 * @see LayerInfo#getAuthorityURLs()
 * @see LayerGroupInfo#getAuthorityURLs()
 */
public interface AuthorityURLInfo extends Serializable {

    /** @return the authority name */
    public String getName();

    /** @param name the authority name */
    public void setName(String name);

    /** @return the authority URL online resource link */
    public String getHref();

    /** @param href the authority URL online resource link */
    public void setHref(String href);
}
