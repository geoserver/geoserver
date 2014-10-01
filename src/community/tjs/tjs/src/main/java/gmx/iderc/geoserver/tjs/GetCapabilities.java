/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package gmx.iderc.geoserver.tjs;

import gmx.iderc.geoserver.tjs.catalog.TJSCatalog;
import net.opengis.tjs10.GetCapabilitiesType;
import org.geoserver.ows.util.RequestUtils;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Web Feature Service GetCapabilities operation.
 * <p>
 * This operation returns a {@link org.geotools.xml.transform.TransformerBase} instance
 * which will serialize the wfs capabilities document. This class uses ows version negotiation
 * to determine which version of the wfs capabilities document to return.
 * </p>
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public class GetCapabilities {
    /**
     * WFS service configuration
     */
    TJSInfo tjs;

    /**
     * The catalog
     */
    TJSCatalog catalog;

    /**
     * Creates a new wfs GetCapabilitis operation.
     *
     * @param tjs     The tjs configuration
     * @param catalog The geoserver catalog.
     */
    public GetCapabilities(TJSInfo tjs, TJSCatalog catalog) {
        this.tjs = tjs;
        this.catalog = catalog;
    }

    public CapabilitiesTransformer run(GetCapabilitiesType request)
            throws TJSException {
        //cite requires that we fail when we see an "invalid" update sequence,
        // since we dont support update sequences, all are invalid, but we take
        // our more lax approach and just ignore it when not doint the cite thing
        if (tjs.isCiteCompliant()) {
            if (request.getUpdateSequence() != null) {
                throw new TJSException("Invalid update sequence", "InvalidUpdateSequence");
            }
        }

        //TODO: the rest of this routine should be done by the dispatcher
        //make sure service is set, cite conformance thing
        //JD - We wrap this in a cite conformance check because cite stricly
        // tests that every request includes the 'service=WFS' key value pair.
        // However often the the context of the request is good enough to
        // determine what the service is, like in 'geoserver/wfs?request=GetCapabilities'
        if (tjs.isCiteCompliant()) {
            if (!request.isSetService()) {
                //give up
                throw new TJSException("Service not set", "MissingParameterValue", "service");
            }
        }

        //do the version negotiation dance
        List<String> provided = new ArrayList<String>();
        provided.add("1.0.0");
//        provided.add("1.1.0");
        List<String> accepted = null;
        if (request.getAcceptVersions() != null)
            accepted = request.getAcceptVersions().getVersion();
        String version = RequestUtils.getVersionPreOws(provided, accepted);

        final CapabilitiesTransformer capsTransformer;
        if ("1.0.0".equals(version)) {
            capsTransformer = new CapabilitiesTransformer.TJS1_0(tjs, catalog);
        } else {
            throw new TJSException("Could not understand version:" + version);
        }
        try {
            capsTransformer.setEncoding(Charset.forName(tjs.getGeoServer().getGlobal().getCharset()));
        } catch (Exception ex) {
            Logger.getLogger(GetCapabilities.class.getName()).log(Level.SEVERE, ex.getMessage());
        }
        return capsTransformer;
    }


}
