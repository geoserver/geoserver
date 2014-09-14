/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.hib.types;

import org.geoserver.catalog.LayerInfo;
import org.geoserver.hibernate.types.EnumUserType;

public class LayerType extends EnumUserType<LayerInfo.Type> {

    public LayerType() {
        super(LayerInfo.Type.class);
    }

}
