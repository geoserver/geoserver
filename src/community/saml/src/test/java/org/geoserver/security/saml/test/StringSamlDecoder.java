/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.saml.test;

import java.io.InputStream;
import org.opensaml.common.SAMLObject;
import org.opensaml.saml2.binding.decoding.HTTPRedirectDeflateDecoder;

public class StringSamlDecoder extends HTTPRedirectDeflateDecoder {

    public SAMLObject decode(String message) throws Exception {
        InputStream samlMessageIns = decodeMessage(message);
        SAMLObject samlMessage = (SAMLObject) unmarshallMessage(samlMessageIns);
        return samlMessage;
    }
}
