/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gmx.iderc.geoserver.tjs.kvp;

import gmx.iderc.geoserver.tjs.TJSException;
import net.opengis.tjs10.AttributeDataType;
import net.opengis.tjs10.JoinDataType;
import net.opengis.tjs10.MapStylingType;
import net.opengis.tjs10.Tjs10Factory;

import java.util.Map;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author root
 */
public class JoinDataKvpRequestReader extends TJSKvpRequestReader {

    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(JoinDataKvpRequestReader.class.getPackage().getName());

    public JoinDataKvpRequestReader() {
        super(JoinDataType.class);
    }

    /*
    Service=TJS&
    Version=1.0&
    Request=JoinData&
    Language=en-CA&
    FrameworkURI=http://foo.bar/foo&
    GetDataURL=http://foo.bar2/foo&
    StylingURL=http://foo.bar3/foo&
    StylingIdentifier=SLD_1.0
    */
    @Override
    public Object read(Object request, Map kvp, Map rawKvp) throws Exception {
        request = super.read(request, kvp, rawKvp);

        JoinDataType joinDataRequest = (JoinDataType) request;

        AttributeDataType adt = Tjs10Factory.eINSTANCE.createAttributeDataType();
        if (kvp.containsKey("GetDataURL")) {
            adt.setGetDataURL(kvp.get("GetDataURL").toString());
        } else {
            throw new TJSException("Request must define a GetDataURL element");
        }
        joinDataRequest.setAttributeData(adt);

        if (kvp.containsKey("StylingURL")) {
            LOGGER.log(Level.INFO, "StylingURL: " + kvp.get("StylingURL").toString());
            MapStylingType mapStylingType = Tjs10Factory.eINSTANCE.createMapStylingType();
            mapStylingType.setStylingURL(kvp.get("StylingURL").toString());
            joinDataRequest.setMapStyling(mapStylingType);
        }

        return request;
    }


}
