/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.csw.kvp;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.opengis.cat.csw20.Csw20Factory;
import net.opengis.cat.csw20.ElementSetNameType;
import net.opengis.cat.csw20.ElementSetType;
import net.opengis.cat.csw20.GetRecordByIdType;
import org.geoserver.platform.ServiceException;

/**
 * GetRecordById KVP request reader
 *
 * @author Niels Charlier
 */
public class GetRecordByIdKvpRequestReader extends CSWKvpRequestReader {

    public GetRecordByIdKvpRequestReader() {
        super(GetRecordByIdType.class);
    }

    @Override
    public Object read(Object request, Map kvp, Map rawKvp) throws Exception {

        ElementSetType ent = (ElementSetType) kvp.get("elementsetname");
        if (ent != null) {
            ElementSetNameType esnt = Csw20Factory.eINSTANCE.createElementSetNameType();
            esnt.setValue(ent);
            kvp.put("elementsetname", esnt);
        }

        List<URI> idsuri = new ArrayList<URI>();
        String rawId = (String) kvp.get("id");
        if (rawId == null) {
            throw new ServiceException(
                    "Missing required parameter id",
                    ServiceException.MISSING_PARAMETER_VALUE,
                    "id");
        }
        String[] ids = rawId.split("\\s*,\\s*");
        for (String id : ids) {
            idsuri.add(new URI(id));
        }
        kvp.put("id", idsuri);

        // proceed with the normal reflective setup
        return super.read(request, kvp, rawKvp);
    }
}
