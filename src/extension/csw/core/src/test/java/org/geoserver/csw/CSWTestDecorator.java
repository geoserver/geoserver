/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw;

import net.opengis.cat.csw20.CapabilitiesType;
import net.opengis.ows10.DomainType;
import net.opengis.ows10.OperationType;
import net.opengis.ows10.OperationsMetadataType;
import org.geoserver.csw.store.CatalogStore;

/**
 * Adds a output format to the GetRecords element. If the generation is properly isolated, doing so
 * won't result in an accumulation of output formats as the decorate method is called across
 * different GetCapabilities requests
 *
 * @author Andrea Aime - GeoSolution
 */
public class CSWTestDecorator implements CapabilitiesDecorator {

    @Override
    public CapabilitiesType decorate(CapabilitiesType caps, CatalogStore store) {
        // amend GetRecords
        OperationsMetadataType operations = caps.getOperationsMetadata();
        OperationType gro = getOperation("GetRecords", operations);

        DomainType outputFormats = getParameter("outputFormat", gro);
        outputFormats.getValue().add("text/xml");

        return caps;
    }

    private OperationType getOperation(String operationName, OperationsMetadataType operations) {
        for (Object o : operations.getOperation()) {
            OperationType op = (OperationType) o;
            if (operationName.equals(op.getName())) {
                return op;
            }
        }

        throw new IllegalArgumentException("Could not find operation " + operationName);
    }

    private DomainType getParameter(String parameterName, OperationType operation) {
        DomainType result = getParameterIfExists(parameterName, operation);
        if (result == null) {
            throw new IllegalArgumentException("Could not find parameter " + parameterName);
        } else {
            return result;
        }
    }

    private DomainType getParameterIfExists(String parameterName, OperationType operation) {
        for (Object o : operation.getParameter()) {
            DomainType dt = (DomainType) o;
            if (parameterName.equals(dt.getName())) {
                return dt;
            }
        }

        return null;
    }
}
