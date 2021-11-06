/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;
import org.geoserver.security.GeoServerSecurityManager;

class EncryptedFieldConverter extends AbstractSingleValueConverter {

    private GeoServerSecurityManager securityManager;

    public EncryptedFieldConverter(GeoServerSecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    @Override
    public boolean canConvert(Class type) {
        return String.class.equals(type);
    }

    @Override
    public Object fromString(String str) {
        return securityManager.getConfigPasswordEncryptionHelper().decode(str);
    }

    @Override
    public String toString(Object obj) {
        return securityManager.getConfigPasswordEncryptionHelper().encode((String) obj);
    }
}
