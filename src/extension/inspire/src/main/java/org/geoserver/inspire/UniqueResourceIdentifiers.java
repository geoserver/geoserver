/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.inspire;

import java.io.Serial;
import java.util.ArrayList;

/**
 * A collection of INSPIRE unique resource identifiers, from code to namespace
 *
 * @author Andrea Aime - GeoSolutions
 */
public class UniqueResourceIdentifiers extends ArrayList<UniqueResourceIdentifier> {

    @Serial
    private static final long serialVersionUID = -6132343935725006351L;

    public UniqueResourceIdentifiers() {}

    public UniqueResourceIdentifiers(UniqueResourceIdentifiers identifiers) {
        for (UniqueResourceIdentifier identifier : identifiers) {
            add(new UniqueResourceIdentifier(identifier));
        }
    }
}
