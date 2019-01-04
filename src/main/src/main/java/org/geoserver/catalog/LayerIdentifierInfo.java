/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.io.Serializable;
import org.geoserver.catalog.impl.LayerIdentifier;

/**
 * Support interface for the publication of layer Indentifier elements in WMS capabilities.
 *
 * <p>ID numbers or labels defined by a particular Authority. For example, the Global Change Master
 * Directory (gcmd.gsfc.nasa.gov) defines a DIF_ID label for every dataset.
 *
 * @author groldan
 * @see LayerIdentifier
 * @see LayerInfo#getIdentifiers()
 * @see LayerGroupInfo#getIdentifiers()
 * @see AuthorityURLInfo
 */
public interface LayerIdentifierInfo extends Serializable {

    /** @return the name of the Authority defining identifiers or labels */
    public String getAuthority();

    /** @param authorityName name of the authority for this identifier */
    public void setAuthority(String authorityName);

    /** @return identifier for a specific authority on a published WMS layer */
    public String getIdentifier();

    /** @param identifier the identifier for a specific authority on a published WMS layer */
    public void setIdentifier(String identifier);
}
