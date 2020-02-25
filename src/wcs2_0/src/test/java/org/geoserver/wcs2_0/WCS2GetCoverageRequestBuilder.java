/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0;

import java.io.IOException;
import java.util.Arrays;
import javax.xml.datatype.XMLGregorianCalendar;
import net.opengis.ows11.BoundingBoxType;
import net.opengis.ows11.CodeType;
import net.opengis.ows11.Ows11Factory;
import net.opengis.wcs11.DomainSubsetType;
import net.opengis.wcs11.OutputType;
import net.opengis.wcs11.TimeSequenceType;
import net.opengis.wcs11.Wcs11Factory;
import net.opengis.wcs20.DimensionSliceType;
import net.opengis.wcs20.DimensionTrimType;
import net.opengis.wcs20.GetCoverageType;
import net.opengis.wcs20.Wcs20Factory;
import org.geotools.wcs.v2_0.WCS;
import org.geotools.wcs.v2_0.WCSConfiguration;
import org.geotools.xsd.Encoder;

/** A simple helper class to build WCS 1.1.1 and WCS 2.0 requests. */
public class WCS2GetCoverageRequestBuilder {

    private GetCoverageType getCoverageType;
    private net.opengis.wcs11.GetCoverageType wcs111GetCoverage;

    private WCS2GetCoverageRequestBuilder() {
        getCoverageType = Wcs20Factory.eINSTANCE.createGetCoverageType();
        wcs111GetCoverage = Wcs11Factory.eINSTANCE.createGetCoverageType();

        wcs111GetCoverage.setVersion("1.1.1");
        OutputType outputType = Wcs11Factory.eINSTANCE.createOutputType();
        outputType.setFormat("image/tiff");
        wcs111GetCoverage.setOutput(outputType);

        getCoverageType.setVersion("2.0.0");
        getCoverageType.setFormat("image/tiff");
    }

    /**
     * Sets the id of the coverage to request.
     *
     * @param coverageId A coverage id
     * @return this builder
     */
    public WCS2GetCoverageRequestBuilder coverageId(String coverageId) {
        getCoverageType.setCoverageId(coverageId);
        CodeType codeType = Ows11Factory.eINSTANCE.createCodeType();
        codeType.setValue(coverageId);
        wcs111GetCoverage.setIdentifier(codeType);
        return this;
    }

    /**
     * Request only a given date, currently fails for WCS 1.1.1
     *
     * @return this builder
     */
    public WCS2GetCoverageRequestBuilder date(XMLGregorianCalendar date) {
        DimensionSliceType dimensionTrim = Wcs20Factory.eINSTANCE.createDimensionSliceType();
        dimensionTrim.setSlicePoint(date.toXMLFormat());
        dimensionTrim.setDimension("time");
        dimensionTrim.setCRS("http://www.opengis.net/def/trs/ISO-8601/0/Gregorian UTC");
        getCoverageType.getDimensionSubset().add(dimensionTrim);

        TimeSequenceType timeSequenceType = Wcs11Factory.eINSTANCE.createTimeSequenceType();
        timeSequenceType.getTimePosition().add(date);
        // TODO the encoder throws an exception on this one
        // getWCS11DomainSubset().setTemporalSubset(timeSequenceType);
        return this;
    }

    /** Sets a lat/lon bounding box. */
    public WCS2GetCoverageRequestBuilder bbox(
            double minLon, double maxLon, double minLat, double maxLat) {
        DimensionTrimType latTrim = Wcs20Factory.eINSTANCE.createDimensionTrimType();
        latTrim.setCRS("http://www.opengis.net/def/crs/EPSG/0/4326");
        latTrim.setTrimLow(minLat + "");
        latTrim.setTrimHigh(maxLat + "");
        latTrim.setDimension("Lat");
        getCoverageType.getDimensionSubset().add(latTrim);

        DimensionTrimType lonTrim = Wcs20Factory.eINSTANCE.createDimensionTrimType();
        lonTrim.setCRS("http://www.opengis.net/def/crs/EPSG/0/4326");
        lonTrim.setTrimLow(minLon + "");
        lonTrim.setTrimHigh(maxLon + "");
        lonTrim.setDimension("Long");
        getCoverageType.getDimensionSubset().add(lonTrim);

        DomainSubsetType domainSubset = getWCS11DomainSubset();
        BoundingBoxType boundingbox = Ows11Factory.eINSTANCE.createBoundingBoxType();
        boundingbox.setCrs("http://www.opengis.net/def/crs/EPSG/0/4326");
        boundingbox.setLowerCorner(Arrays.asList(minLat, minLon));
        boundingbox.setUpperCorner(Arrays.asList(maxLat, maxLon));
        domainSubset.setBoundingBox(boundingbox);
        return this;
    }

    private DomainSubsetType getWCS11DomainSubset() {
        DomainSubsetType domainSubset = wcs111GetCoverage.getDomainSubset();
        if (domainSubset == null) {
            domainSubset = Wcs11Factory.eINSTANCE.createDomainSubsetType();
            wcs111GetCoverage.setDomainSubset(domainSubset);
        }
        return domainSubset;
    }

    /** Returns the xml encoding of the created request. */
    public String asXML(String version) throws IOException {
        if ("1.1.1".equals(version)) {
            Encoder encoder = new Encoder(new org.geotools.wcs.v1_1.WCSConfiguration());
            encoder.setIndenting(true);
            encoder.setOmitXMLDeclaration(true);
            // prefix is set to 'null' if we don't declare it explicitly
            encoder.getNamespaces().declarePrefix("ows", "http://www.opengis.net/ows/1.1");
            return encoder.encodeAsString(wcs111GetCoverage, org.geotools.wcs.v1_1.WCS.GetCoverage);
        } else {
            Encoder encoder = new Encoder(new WCSConfiguration());
            encoder.setIndenting(true);
            encoder.setOmitXMLDeclaration(true);
            return encoder.encodeAsString(getCoverageType, WCS.GetCoverage);
        }
    }

    /**
     * Create a new builder
     *
     * @return a new builder
     */
    public static WCS2GetCoverageRequestBuilder newBuilder() {
        return new WCS2GetCoverageRequestBuilder();
    }
}
