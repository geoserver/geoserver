/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.coverages;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static org.geoserver.ogcapi.APIException.INVALID_PARAMETER_VALUE;
import static org.geoserver.ogcapi.MappingJackson2YAMLMessageConverter.APPLICATION_YAML_VALUE;
import static org.geoserver.ogcapi.OpenAPIMessageConverter.OPEN_API_MEDIA_TYPE_VALUE;

import io.swagger.v3.oas.models.OpenAPI;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.opengis.wcs20.DimensionSliceType;
import net.opengis.wcs20.DimensionTrimType;
import net.opengis.wcs20.GetCoverageType;
import net.opengis.wcs20.Wcs20Factory;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.config.GeoServer;
import org.geoserver.crs.CapabilitiesCRSProvider;
import org.geoserver.ogcapi.APIBBoxParser;
import org.geoserver.ogcapi.APIDispatcher;
import org.geoserver.ogcapi.APIException;
import org.geoserver.ogcapi.APIFilterParser;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.ogcapi.APIService;
import org.geoserver.ogcapi.ConformanceClass;
import org.geoserver.ogcapi.ConformanceDocument;
import org.geoserver.ogcapi.DefaultContentType;
import org.geoserver.ogcapi.HTMLResponseBody;
import org.geoserver.ogcapi.v1.coverages.cis.DomainSet;
import org.geoserver.ows.kvp.TimeParser;
import org.geoserver.platform.ServiceException;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wcs2_0.WCS20Const;
import org.geoserver.wcs2_0.WebCoverageService20;
import org.geoserver.wcs2_0.util.EnvelopeAxesLabelsMapper;
import org.geotools.api.coverage.grid.GridCoverage;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.DateRange;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/** Implementation of OGC Coverages API service */
@APIService(
        service = "Coverages",
        version = "1.0.0",
        landingPage = "ogc/coverages/v1",
        serviceClass = WCSInfo.class)
@RequestMapping(path = APIDispatcher.ROOT_PATH + "/coverages/v1")
public class CoveragesService {

    static final Pattern INTEGER = Pattern.compile("\\d+");
    public static final String DEFAULT_CRS = "http://www.opengis.net/def/crs/OGC/1.3/CRS84";
    public static final String CONF_CLASS_COVERAGE =
            "http://www.opengis.net/spec/ogcapi-coverages-1/1.0/conf/geodata-coverage";
    public static final String CONF_CLASS_GEOTIFF =
            "http://www.opengis.net/spec/ogcapi-coverages-1/1.0/conf/geotiff";
    public static final String CONF_CLASS_SUBSET =
            "http://www.opengis.net/spec/ogcapi-coverages-1/1.0/req/coverage-subset";

    public static final String GEOTIFF_MIME = "image/tiff;application=geotiff";

    private final GeoServer geoServer;
    private final WebCoverageService20 wcs20;
    private final APIFilterParser filterParser;
    private TimeParser timeParser = new TimeParser();
    private SubsetsParser subsetParser = new SubsetsParser();

    public CoveragesService(
            GeoServer geoServer,
            @Qualifier("wcs20Service") WebCoverageService20 wcs20,
            APIFilterParser filterParser) {
        this.geoServer = geoServer;
        this.wcs20 = wcs20;
        this.filterParser = filterParser;
    }

    public static List<String> getCoverageCRS(CoverageInfo coverage, List<String> defaultCRS) {
        if (coverage.getResponseSRS() != null) {
            List<String> result =
                    coverage.getResponseSRS().stream()
                            // the GUI allows to enter codes as "EPSG:XYZW"
                            .map(c -> mapResponseSRS(c))
                            .collect(Collectors.toList());
            result.remove(CoveragesService.DEFAULT_CRS);
            result.add(0, CoveragesService.DEFAULT_CRS);
            return result;
        }
        return defaultCRS;
    }

    public WCSInfo getService() {
        return geoServer.getService(WCSInfo.class);
    }

    private Catalog getCatalog() {
        return geoServer.getCatalog();
    }

    @GetMapping(name = "getLandingPage")
    @ResponseBody
    @HTMLResponseBody(templateName = "landingPage.ftl", fileName = "landingPage.html")
    public CoveragesLandingPage getLandingPage() {
        return new CoveragesLandingPage(getService(), getCatalog(), "ogc/coverages/v1");
    }

    @GetMapping(path = "conformance", name = "getConformanceDeclaration")
    @ResponseBody
    @HTMLResponseBody(templateName = "conformance.ftl", fileName = "conformance.html")
    public ConformanceDocument conformance() {
        List<String> classes =
                Arrays.asList(
                        ConformanceClass.CORE,
                        ConformanceClass.COLLECTIONS,
                        ConformanceClass.HTML,
                        ConformanceClass.JSON,
                        ConformanceClass.OAS3,
                        ConformanceClass.GEODATA,
                        CONF_CLASS_COVERAGE,
                        CONF_CLASS_GEOTIFF,
                        CONF_CLASS_SUBSET);
        return new ConformanceDocument("OGC API Coverages", classes);
    }

    @GetMapping(
            path = {"openapi", "openapi.json", "openapi.yaml"},
            name = "getApi",
            produces = {
                OPEN_API_MEDIA_TYPE_VALUE,
                APPLICATION_YAML_VALUE,
                MediaType.TEXT_XML_VALUE
            })
    @ResponseBody
    @HTMLResponseBody(templateName = "api.ftl", fileName = "api.html")
    public OpenAPI api() throws IOException {
        return new CoveragesAPIBuilder().build(getService());
    }

    @GetMapping(path = "collections", name = "getCollections")
    @ResponseBody
    @HTMLResponseBody(templateName = "collections.ftl", fileName = "collections.html")
    public CollectionsDocument getCollections() {
        return new CollectionsDocument(geoServer, getServiceCRSList());
    }

    @GetMapping(path = "collections/{collectionId}", name = "describeCollection")
    @ResponseBody
    @HTMLResponseBody(templateName = "collection.ftl", fileName = "collection.html")
    public CollectionDocument getCollection(
            @PathVariable(name = "collectionId") String collectionId) throws IOException {
        CoverageInfo ci = getCoverage(collectionId);
        CollectionDocument collection =
                new CollectionDocument(geoServer, ci, getCoverageCRS(ci, getServiceCRSList()));

        return collection;
    }

    @ResponseBody
    @GetMapping(path = "collections/{collectionId}/coverage", name = "getCoverage")
    @DefaultContentType(GEOTIFF_MIME)
    public CoveragesResponse items(
            @PathVariable(name = "collectionId") String collectionId,
            @RequestParam(name = "bbox", required = false) String bbox,
            @RequestParam(name = "bbox-crs", required = false) String bboxCRS,
            @RequestParam(name = "datetime", required = false) String datetime,
            @RequestParam(name = "filter", required = false) String filter,
            @RequestParam(name = "filter-lang", required = false) String filterLanguage,
            @RequestParam(name = "crs", required = false) String crs,
            @RequestParam(name = "subset", required = false) String subset)
            throws Exception {
        // side effect, checks existence
        CoverageInfo coverage = getCoverage(collectionId);

        Wcs20Factory wf = Wcs20Factory.eINSTANCE;
        GetCoverageType request = wf.createGetCoverageType();
        request.setBaseUrl(APIRequestInfo.get().getBaseURL());
        request.setService("WCS");
        request.setCoverageId(collectionId);
        request.setVersion(WCS20Const.V201);
        request.setFilter(filterParser.parse(filter, filterLanguage));
        if (bbox != null) setBBOXDimensionSubset(bbox, bboxCRS, request);
        if (datetime != null) setupTimeSubset(datetime, coverage, request);
        if (subset != null) setupSubsets(subset, coverage, request);

        GridCoverage gridCoverage = wcs20.getCoverage(request);
        return new CoveragesResponse(request, gridCoverage);
    }

    private void setupSubsets(String subsetsSpec, CoverageInfo coverage, GetCoverageType request) {
        subsetParser.parse(subsetsSpec).forEach(ss -> request.getDimensionSubset().add(ss));
    }

    private void setupTimeSubset(String datetime, CoverageInfo coverage, GetCoverageType request)
            throws ParseException {
        Wcs20Factory wf = Wcs20Factory.eINSTANCE;
        DimensionInfo time = coverage.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
        if (time == null || !time.isEnabled()) {
            throw new APIException(
                    INVALID_PARAMETER_VALUE,
                    "Time dimension is not enabled in this coverage",
                    HttpStatus.BAD_REQUEST);
        }
        Collection times = timeParser.parse(datetime);
        if (times.isEmpty() || times.size() > 1) {
            throw new APIException(
                    INVALID_PARAMETER_VALUE,
                    "Invalid datetime specification, must be a single time, or a time range",
                    HttpStatus.BAD_REQUEST);
        }
        Object timeSpec = times.iterator().next();
        if (timeSpec instanceof Date) {
            DimensionSliceType slice = wf.createDimensionSliceType();
            slice.setDimension("time");
            slice.setSlicePoint(ISO_INSTANT.format(((Date) timeSpec).toInstant()));
            request.getDimensionSubset().add(slice);
        } else if (timeSpec instanceof DateRange) {
            DimensionTrimType timeTrim = wf.createDimensionTrimType();
            DateRange range = (DateRange) timeSpec;
            timeTrim.setDimension("time");
            timeTrim.setTrimLow(ISO_INSTANT.format(range.getMinValue().toInstant()));
            timeTrim.setTrimHigh(ISO_INSTANT.format(range.getMaxValue().toInstant()));
            request.getDimensionSubset().add(timeTrim);
        }
    }

    private void setBBOXDimensionSubset(String bbox, String bboxCRS, GetCoverageType request)
            throws FactoryException {
        Wcs20Factory wf = Wcs20Factory.eINSTANCE;
        CoordinateReferenceSystem bcrs = getCRS(bboxCRS, DefaultGeographicCRS.WGS84);
        ReferencedEnvelope[] envelopes = APIBBoxParser.parse(bbox, bcrs);
        if (envelopes.length > 1)
            throw new APIException(
                    APIException.NO_APPLICABLE_CODE,
                    "Cannot deal with bounding boxes crossing the dateline yet",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        ReferencedEnvelope envelope = envelopes[0];

        EnvelopeAxesLabelsMapper mapper = new EnvelopeAxesLabelsMapper();
        DimensionTrimType xTrim = wf.createDimensionTrimType();
        xTrim.setDimension(mapper.getAxisLabel(bcrs.getCoordinateSystem().getAxis(0)));
        xTrim.setTrimLow(String.valueOf(envelope.getMinimum(0)));
        xTrim.setTrimHigh(String.valueOf(envelope.getMaximum(0)));

        DimensionTrimType yTrim = wf.createDimensionTrimType();
        yTrim.setDimension(mapper.getAxisLabel(bcrs.getCoordinateSystem().getAxis(1)));
        yTrim.setTrimLow(String.valueOf(envelope.getMinimum(1)));
        yTrim.setTrimHigh(String.valueOf(envelope.getMaximum(1)));

        request.getDimensionSubset().add(xTrim);
        request.getDimensionSubset().add(yTrim);
    }

    private CoordinateReferenceSystem getCRS(String crsSpec, CoordinateReferenceSystem defaultValue)
            throws FactoryException {
        if (crsSpec == null) return defaultValue;
        return CRS.decode(crsSpec);
    }

    private CoverageInfo getCoverage(String collectionId) {
        CoverageInfo coverage = getCatalog().getCoverageByName(collectionId);
        if (coverage == null) {
            throw new ServiceException(
                    "Unknown collection " + collectionId,
                    ServiceException.INVALID_PARAMETER_VALUE,
                    "collectionId");
        }
        return coverage;
    }

    protected List<String> getServiceCRSList() {
        List<String> result = getService().getSRS();

        if (result == null || result.isEmpty()) {
            // consult the referencing database
            CapabilitiesCRSProvider provider = new CapabilitiesCRSProvider();
            provider.getAuthorityExclusions().add("CRS");
            provider.setCodeMapper(CoveragesService::mapCRSCode);
            result = new ArrayList<>(provider.getCodes());
        } else {
            // the configured ones are just numbers, prefix
            result = result.stream().map(c -> mapResponseSRS(c)).collect(Collectors.toList());
        }
        // the Features API default CRS (cannot be contained due to the different prefixing)
        result.add(0, DEFAULT_CRS);
        return result;
    }

    /** Maps authority and code to a CRS URI */
    static String mapCRSCode(String authority, String code) {
        return "http://www.opengis.net/def/crs/" + authority + "/0/" + code;
    }

    /** Returns the CRS-URI for a given CRS. */
    public static String getCRSURI(CoordinateReferenceSystem crs) throws FactoryException {
        if (CRS.equalsIgnoreMetadata(crs, DefaultGeographicCRS.WGS84)) {
            return DEFAULT_CRS;
        }
        String identifier = ResourcePool.lookupIdentifier(crs, false);
        return mapResponseSRS(identifier);
    }

    private static String mapResponseSRS(String srs) {
        int idx = srs.indexOf(":");
        if (idx == -1) return mapCRSCode("EPSG", srs);
        String authority = srs.substring(0, idx);
        String code = srs.substring(idx + 1);
        return mapCRSCode(authority, code);
    }

    @ResponseBody
    @GetMapping(
            path = "collections/{collectionId}/coverage/domainset",
            name = "getCoverageDomainSet")
    @DefaultContentType("application/json")
    public DomainSet domainSet(@PathVariable(name = "collectionId") String collectionId)
            throws Exception {
        // side effect, checks existence
        CoverageInfo coverage = getCoverage(collectionId);
        return new DomainSetBuilder(coverage).build();
    }
}
