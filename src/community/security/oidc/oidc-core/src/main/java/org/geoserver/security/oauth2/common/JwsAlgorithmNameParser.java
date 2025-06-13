/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.common;

import org.springframework.security.oauth2.jose.jws.JwsAlgorithm;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;

/**
 * Capable of turning JwsAlgorithm names into {@link JwsAlgorithm} objects.
 *
 * @author awaterme
 */
public class JwsAlgorithmNameParser {

    /**
     * @param pString
     * @return the algorithm or null if no match was found
     */
    public JwsAlgorithm parse(String pString) {
        if (pString == null || pString.isBlank()) {
            return null;
        }
        SignatureAlgorithm lAlg = SignatureAlgorithm.from(pString);
        if (lAlg != null) {
            return lAlg;
        }
        MacAlgorithm lMac = MacAlgorithm.from(pString);
        return lMac;
    }
}
