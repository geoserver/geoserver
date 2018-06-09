package org.geoserver.security.onelogin.test;

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
