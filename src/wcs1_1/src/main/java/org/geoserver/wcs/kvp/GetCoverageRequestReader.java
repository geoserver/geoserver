/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.kvp;

import static org.vfny.geoserver.wcs.WcsException.WcsExceptionCode.MissingParameterValue;

import java.util.Map;
import net.opengis.ows11.BoundingBoxType;
import net.opengis.wcs11.DomainSubsetType;
import net.opengis.wcs11.GetCoverageType;
import net.opengis.wcs11.GridCrsType;
import net.opengis.wcs11.OutputType;
import net.opengis.wcs11.TimeSequenceType;
import net.opengis.wcs11.Wcs111Factory;
import org.geoserver.catalog.Catalog;
import org.geoserver.ows.kvp.EMFKvpRequestReader;
import org.vfny.geoserver.wcs.WcsException;
import org.vfny.geoserver.wcs.WcsException.WcsExceptionCode;

public class GetCoverageRequestReader extends EMFKvpRequestReader {

    Catalog catalog;

    public GetCoverageRequestReader(Catalog catalog) {
        super(GetCoverageType.class, Wcs111Factory.eINSTANCE);
        this.catalog = catalog;
    }

    @Override
    public Object read(Object request, Map kvp, Map rawKvp) throws Exception {
        GetCoverageType getCoverage = (GetCoverageType) super.read(request, kvp, rawKvp);

        // grab coverage info to perform further checks
        if (getCoverage.getIdentifier() == null)
            throw new WcsException(
                    "identifier parameter is mandatory", MissingParameterValue, "identifier");

        // build the domain subset
        getCoverage.setDomainSubset(parseDomainSubset(kvp));

        // build output element
        getCoverage.setOutput(parseOutputElement(kvp));

        return getCoverage;
    }

    private DomainSubsetType parseDomainSubset(Map kvp) {
        final DomainSubsetType domainSubset = Wcs111Factory.eINSTANCE.createDomainSubsetType();

        // either bbox or timesequence must be there
        BoundingBoxType bbox = (BoundingBoxType) kvp.get("BoundingBox");
        TimeSequenceType timeSequence = (TimeSequenceType) kvp.get("TimeSequence");
        if (timeSequence == null && bbox == null)
            throw new WcsException(
                    "Bounding box cannot be null, TimeSequence has not been specified",
                    WcsExceptionCode.MissingParameterValue,
                    "BoundingBox");

        domainSubset.setBoundingBox(bbox);
        domainSubset.setTemporalSubset(timeSequence);

        return domainSubset;
    }

    private OutputType parseOutputElement(Map kvp) throws Exception {
        final OutputType output = Wcs111Factory.eINSTANCE.createOutputType();
        output.setGridCRS(Wcs111Factory.eINSTANCE.createGridCrsType());

        // check and set store
        Boolean store = (Boolean) kvp.get("store");
        if (store != null) output.setStore(store.booleanValue());

        // check and set format
        String format = (String) kvp.get("format");
        if (format == null)
            throw new WcsException(
                    "format parameter is mandatory", MissingParameterValue, "format");
        output.setFormat(format);

        // set the other gridcrs properties
        final GridCrsType gridCRS = output.getGridCRS();
        gridCRS.setGridBaseCRS((String) kvp.get("gridBaseCrs"));

        String gridType = (String) kvp.get("gridType");
        if (gridType == null) {
            gridType = gridCRS.getGridType();
        }
        gridCRS.setGridType(gridType);

        String gridCS = (String) kvp.get("gridCS");
        if (gridCS == null) {
            gridCS = gridCRS.getGridCS();
        }
        gridCRS.setGridCS(gridCS);
        gridCRS.setGridOrigin((Double[]) kvp.get("GridOrigin"));
        gridCRS.setGridOffsets((Double[]) kvp.get("GridOffsets"));

        return output;
    }
}
