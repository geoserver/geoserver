/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.geoserver.opensearch.eo.response.AtomSearchResponse;
import org.geoserver.ows.util.ResponseUtils;
import org.junit.Test;
import org.w3c.dom.Document;

public class MetadataTest extends OSEOTestSupport {

    private static final String ENCODED_ATOM_MIME =
            ResponseUtils.urlEncode(AtomSearchResponse.MIME);

    @Test
    public void testGetSentinel2Metadata() throws Exception {
        Document d = getAsDOM("oseo/metadata?uid=SENTINEL2", 200, MetadataRequest.ISO_METADATA);
        print(d);

        String meta = "/gmi:MI_Metadata/";
        assertThat(
                d, hasXPath(meta + "gmd:fileIdentifier/gco:CharacterString", equalTo("SENTINEL2")));
        assertThat(d, hasXPath(meta + "gmd:dateStamp/gco:Date", equalTo("2015-07-01T10:20:21Z")));
        String citation =
                meta
                        + "gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/";
        assertThat(d, hasXPath(citation + "gmd:title/gco:CharacterString", equalTo("SENTINEL2")));
        assertThat(
                d,
                hasXPath(
                        citation + "gmd:date/gmd:CI_Date/gmd:date/gco:Date",
                        equalTo("2015-07-01T10:20:21Z")));
        String time =
                meta
                        + "gmd:identificationInfo/gmd:MD_DataIdentification/gmd:extent/"
                        + "gmd:EX_Extent/gmd:temporalElement/"
                        + "gmd:EX_TemporalExtent/gmd:extent/gml:TimePeriod/";
        assertThat(d, hasXPath(time + "gml:beginPosition", equalTo("2015-07-01T10:20:21Z")));
        assertThat(d, hasXPath(time + "gml:endPosition", equalTo("2016-02-26T10:20:21Z")));
        String platform =
                meta
                        + "gmi:acquisitionInformation/gmi:MI_AcquisitionInformation/gmi:platform/gmi:MI_Platform/";
        assertThat(
                d,
                hasXPath(
                        platform + "gmi:identifier/gmd:MD_Identifier/gmd:code/gmx:Anchor",
                        equalTo("Sentinel-2")));
        String instrument = platform + "gmi:instrument/gmi:MI_Instrument/";
        assertThat(
                d,
                hasXPath(
                        instrument + "gmi:identifier/gmd:MD_Identifier/gmd:code/gmx:Anchor",
                        equalTo("MSI")));
        assertThat(d, hasXPath(instrument + "gmi:type/gmi:MI_SensorTypeCode", equalTo("OPTICAL")));

        // check he bbox too
        String bbox =
                meta
                        + "gmd:identificationInfo/gmd:MD_DataIdentification/gmd:extent/gmd:EX_Extent/gmd:geographicElement/gmd:EX_GeographicBoundingBox/";
        assertThat(d, hasXPath(bbox + "gmd:westBoundLongitude/gco:Decimal", equalTo("-179")));
        assertThat(d, hasXPath(bbox + "gmd:eastBoundLongitude/gco:Decimal", equalTo("179")));
        assertThat(d, hasXPath(bbox + "gmd:southBoundLatitude/gco:Decimal", equalTo("-89")));
        assertThat(d, hasXPath(bbox + "gmd:northBoundLatitude/gco:Decimal", equalTo("89")));
    }

    @Test
    public void testGetSentinel1Metadata() throws Exception {
        Document d = getAsDOM("oseo/metadata?uid=SENTINEL1", 200, MetadataRequest.ISO_METADATA);

        String meta = "/gmi:MI_Metadata/";

        // static bit in the default template, to make sure the landsat8 template it not making a
        // mess
        assertThat(d, hasXPath(meta + "gmd:language/gmd:LanguageCode", equalTo("eng")));

        assertThat(
                d, hasXPath(meta + "gmd:fileIdentifier/gco:CharacterString", equalTo("SENTINEL1")));
        assertThat(d, hasXPath(meta + "gmd:dateStamp/gco:Date", equalTo("2015-02-26T10:20:21Z")));
        String citation =
                meta
                        + "gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/";
        assertThat(d, hasXPath(citation + "gmd:title/gco:CharacterString", equalTo("SENTINEL1")));
        assertThat(
                d,
                hasXPath(
                        citation + "gmd:date/gmd:CI_Date/gmd:date/gco:Date",
                        equalTo("2015-02-26T10:20:21Z")));
        String time =
                meta
                        + "gmd:identificationInfo/gmd:MD_DataIdentification/gmd:extent/"
                        + "gmd:EX_Extent/gmd:temporalElement/"
                        + "gmd:EX_TemporalExtent/gmd:extent/gml:TimePeriod/";
        assertThat(d, hasXPath(time + "gml:beginPosition", equalTo("2015-02-26T10:20:21Z")));
        assertThat(d, hasXPath(time + "gml:endPosition", equalTo("")));
        String platform =
                meta
                        + "gmi:acquisitionInformation/gmi:MI_AcquisitionInformation/gmi:platform/gmi:MI_Platform/";
        assertThat(
                d,
                hasXPath(
                        platform + "gmi:identifier/gmd:MD_Identifier/gmd:code/gmx:Anchor",
                        equalTo("Sentinel-1")));
        String instrument = platform + "gmi:instrument/gmi:MI_Instrument/";
        assertThat(
                d,
                hasXPath(
                        instrument + "gmi:identifier/gmd:MD_Identifier/gmd:code/gmx:Anchor",
                        equalTo("C-SAR")));
        assertThat(d, hasXPath(instrument + "gmi:type/gmi:MI_SensorTypeCode", equalTo("RADAR")));
    }

    @Test
    public void testGetLandsat8Metadata() throws Exception {
        Document d = getAsDOM("oseo/metadata?uid=LANDSAT8", 200, MetadataRequest.ISO_METADATA);
        print(d);

        String meta = "/gmi:MI_Metadata/";

        // the one bit that has been customized, the language, it's not dynamic
        assertThat(d, hasXPath(meta + "gmd:language/gmd:LanguageCode", equalTo("fr")));

        // everything else
        assertThat(
                d, hasXPath(meta + "gmd:fileIdentifier/gco:CharacterString", equalTo("LANDSAT8")));
        assertThat(d, hasXPath(meta + "gmd:dateStamp/gco:Date", equalTo("1988-02-26T10:20:21Z")));
        String citation =
                meta
                        + "gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/";
        assertThat(d, hasXPath(citation + "gmd:title/gco:CharacterString", equalTo("LANDSAT8")));
        assertThat(
                d,
                hasXPath(
                        citation + "gmd:date/gmd:CI_Date/gmd:date/gco:Date",
                        equalTo("1988-02-26T10:20:21Z")));
        String time =
                meta
                        + "gmd:identificationInfo/gmd:MD_DataIdentification/gmd:extent/"
                        + "gmd:EX_Extent/gmd:temporalElement/"
                        + "gmd:EX_TemporalExtent/gmd:extent/gml:TimePeriod/";
        assertThat(d, hasXPath(time + "gml:beginPosition", equalTo("1988-02-26T10:20:21Z")));
        assertThat(d, hasXPath(time + "gml:endPosition", equalTo("2013-03-01T10:20:21Z")));
        String platform =
                meta
                        + "gmi:acquisitionInformation/gmi:MI_AcquisitionInformation/gmi:platform/gmi:MI_Platform/";
        // this one is null in the database
        assertThat(
                d,
                hasXPath(
                        platform + "gmi:identifier/gmd:MD_Identifier/gmd:code/gmx:Anchor",
                        equalTo("")));
        String instrument1 = platform + "gmi:instrument[1]/gmi:MI_Instrument/";
        assertThat(
                d,
                hasXPath(
                        instrument1 + "gmi:identifier/gmd:MD_Identifier/gmd:code/gmx:Anchor",
                        equalTo("OLI")));
        assertThat(d, hasXPath(instrument1 + "gmi:type/gmi:MI_SensorTypeCode", equalTo("OPTICAL")));
        String instrument2 = platform + "gmi:instrument[2]/gmi:MI_Instrument/";
        assertThat(
                d,
                hasXPath(
                        instrument2 + "gmi:identifier/gmd:MD_Identifier/gmd:code/gmx:Anchor",
                        equalTo("TIRS")));
        assertThat(d, hasXPath(instrument1 + "gmi:type/gmi:MI_SensorTypeCode", equalTo("OPTICAL")));
    }

    @Test
    public void testProductMetadata() throws Exception {
        String uid = "S2A_OPER_MSI_L1C_TL_SGS__20160929T154211_A006640_T32TPP_N02.04";
        String path =
                "oseo/metadata?parentId=SENTINEL2&uid=" + uid + "&httpAccept=application/gml%2Bxml";
        Document d = getAsDOM(path, 200, MetadataRequest.OM_METADATA);

        String obs = "/opt:EarthObservation/";
        String timePeriod = obs + "om:phenomenonTime/gml:TimePeriod/";
        assertThat(d, hasXPath(timePeriod + "gml:beginPosition", equalTo("2016-09-29T10:20:22Z")));
        assertThat(d, hasXPath(timePeriod + "gml:endPosition", equalTo("2016-09-29T10:23:44Z")));

        String equipment = obs + "om:procedure/eop:EarthObservationEquipment/";
        String platform = equipment + "eop:platform/eop:Platform/";
        assertThat(d, hasXPath(platform + "eop:shortName", equalTo("Sentinel-2")));
        assertThat(d, hasXPath(platform + "eop:serialIdentifier", equalTo("A")));
        assertThat(d, hasXPath(platform + "eop:orbitType", equalTo("LEO")));
        String instrument = equipment + "eop:instrument/eop:Instrument/";
        assertThat(d, hasXPath(instrument + "eop:shortName", equalTo("MSI")));
        assertThat(
                d,
                hasXPath(instrument + "eop:description", equalTo("This is a customized property")));
        String sensor = equipment + "eop:sensor/eop:Sensor/";
        assertThat(d, hasXPath(sensor + "eop:sensorType", equalTo("OPTICAL")));

        String acq = equipment + "eop:acquisitionParameters/eop:Acquisition/";
        assertThat(d, hasXPath(acq + "eop:orbitNumber", equalTo("65")));
        assertThat(d, hasXPath(acq + "eop:orbitDirection", equalTo("DESCENDING")));

        String extent = obs + "om:featureOfInterest/eop:Footprint/eop:multiExtentOf/";
        String ring =
                extent
                        + "gml:MultiSurface/gml:surfaceMembers/gml:Polygon/gml:exterior/gml:LinearRing/";
        assertThat(
                d,
                hasXPath(
                        ring + "gml:posList",
                        equalTo(
                                "11.583808 43.235601 11.626646 44.22321 10.252481 44.246551 10.232033 43.258155 11.583808 43.235601")));

        String browse =
                obs + "om:result/opt:EarthObservationResult/eop:browse/eop:BrowseInformation/";
        assertThat(
                d,
                hasXPath(
                        browse + "eop:fileName/ows:ServiceReference/@xlink:href",
                        equalTo(
                                "http://localhost:8080/geoserver/oseo/quicklook?uid="
                                        + uid
                                        + "&parentId=SENTINEL2")));

        String meta = obs + "/eop:metaDataProperty/eop:EarthObservationMetaData/";
        assertThat(d, hasXPath(meta + "eop:identifier", equalTo(uid)));
    }

    @Test
    public void testGetCollectionMetadataInvalidFormat() throws Exception {
        Document dom =
                getAsOpenSearchException("oseo/metadata?uid=SENTINEL2&httpAccept=foo/bar", 400);
        assertThat(
                dom,
                hasXPath("/rss/channel/item/title", containsString(MetadataRequest.ISO_METADATA)));
    }

    @Test
    public void testGetDisableCollectionMetadata() throws Exception {
        Document dom = getAsOpenSearchException("oseo/metadata?uid=DISABLED_COLLECTION", 404);
        assertThat(
                dom,
                hasXPath(
                        "/rss/channel/item/title",
                        containsString("Could not locate the requested product")));
    }

    @Test
    public void testGetProductMetadataInvalidFormat() throws Exception {
        Document dom =
                getAsOpenSearchException(
                        "oseo/metadata?parentId=SENTINEL2&uid=123&httpAccept=foo/bar", 400);
        assertThat(
                dom,
                hasXPath("/rss/channel/item/title", containsString(MetadataRequest.OM_METADATA)));
    }

    @Test
    public void testGetDisabledProductMetadata() throws Exception {
        Document dom =
                getAsOpenSearchException(
                        "oseo/metadata?parentId=LANDSAT8&uid=LS8_TEST.DISABLED", 404);
        assertThat(
                dom,
                hasXPath(
                        "/rss/channel/item/title",
                        containsString("Could not locate the requested product")));
    }
}
