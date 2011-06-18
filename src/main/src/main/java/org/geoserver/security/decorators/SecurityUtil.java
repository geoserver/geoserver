/**
 * 
 */
package org.geoserver.security.decorators;

import org.geoserver.security.AccessLevel;
import org.geoserver.security.VectorAccessLimits;
import org.geoserver.security.WrapperPolicy;
import org.geotools.data.Query;
import org.opengis.filter.Filter;

/**
 * Package local class factoring out some common security wrapper code
 * 
 * @author Andrea Aime - GeoSolutions
 */
class SecurityUtils {

    /**
     * Builds the write query based on the access limits class
     * 
     * @return
     */
    static Query getWriteQuery(WrapperPolicy policy) {
        if(policy.getAccessLevel() != AccessLevel.READ_WRITE) {
            return new Query(null, Filter.EXCLUDE);
        } else if (policy.getLimits() == null) {
            return Query.ALL;
        } else if (policy.getLimits() instanceof VectorAccessLimits) {
            VectorAccessLimits val = (VectorAccessLimits) policy.getLimits();
            return val.getWriteQuery();
        } else {
            throw new IllegalArgumentException("SecureFeatureStore has been fed "
                    + "with unexpected AccessLimits class " + policy.getLimits().getClass());
        }
    }
}
