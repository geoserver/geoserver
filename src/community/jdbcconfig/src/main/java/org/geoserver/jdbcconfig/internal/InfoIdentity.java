/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcconfig.internal;

import java.io.Serializable;
import java.util.Arrays;
import org.geoserver.catalog.Info;

public class InfoIdentity implements Serializable {

    private static final long serialVersionUID = -1756381133395681156L;

    private Class<? extends Info> clazz;

    private String[] descriptor;

    private String[] values;

    public InfoIdentity(Class<? extends Info> clazz, String[] descriptor, String[] values) {
        this.clazz = clazz;
        this.descriptor = descriptor;
        this.values = values;
    }

    public Class<? extends Info> getClazz() {
        return clazz;
    }

    public String[] getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(String[] descriptor) {
        this.descriptor = descriptor;
    }

    public String[] getValues() {
        return values;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
        result = prime * result + Arrays.hashCode(descriptor);
        result = prime * result + Arrays.hashCode(values);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        InfoIdentity other = (InfoIdentity) obj;
        if (clazz == null) {
            if (other.clazz != null) return false;
        } else if (!clazz.equals(other.clazz)) return false;
        if (!Arrays.equals(descriptor, other.descriptor)) return false;
        if (!Arrays.equals(values, other.values)) return false;
        return true;
    }
}
