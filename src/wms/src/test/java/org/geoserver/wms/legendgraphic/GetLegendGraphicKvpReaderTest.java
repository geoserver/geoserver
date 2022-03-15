/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.feature.NameImpl;
import org.geotools.styling.Style;
import org.geotools.util.GrowableInternationalString;
import org.junit.Before;
import org.springframework.mock.web.MockHttpServletRequest;

public class GetLegendGraphicKvpReaderTest extends WMSTestSupport {
    /**
     * request reader to test against, initialized by default with all parameters from <code>
     * requiredParameters</code> and <code>optionalParameters</code>
     */
    GetLegendGraphicKvpReader requestReader;

    /** test values for required parameters */
    Map<String, String> requiredParameters;

    /** test values for optional parameters */
    Map<String, String> optionalParameters;

    /** both required and optional parameters joint up */
    Map<String, String> allParameters;

    /** mock request */
    MockHttpServletRequest httpRequest;

    /** mock config object */
    WMS wms;

    /**
     * Remainder:
     *
     * <ul>
     *   <li>VERSION/Required
     *   <li>REQUEST/Required
     *   <li>LAYER/Required
     *   <li>FORMAT/Required
     *   <li>STYLE/Optional
     *   <li>FEATURETYPE/Optional
     *   <li>RULE/Optional
     *   <li>SCALE/Optional
     *   <li>SLD/Optional
     *   <li>SLD_BODY/Optional
     *   <li>WIDTH/Optional
     *   <li>HEIGHT/Optional
     *   <li>LANGUAGE/Optional
     *   <li>EXCEPTIONS/Optional
     * </ul>
     */
    @Before
    public void setParameters() throws Exception {
        requiredParameters = new HashMap<>();
        requiredParameters.put("VERSION", "1.0.0");
        requiredParameters.put("REQUEST", "GetLegendGraphic");
        requiredParameters.put("LAYER", "cite:Ponds");
        requiredParameters.put("FORMAT", "image/png");

        optionalParameters = new HashMap<>();
        optionalParameters.put("STYLE", "Ponds");
        optionalParameters.put("FEATURETYPE", "fake_not_used");
        // optionalParameters.put("RULE", "testRule");
        optionalParameters.put("SCALE", "1000");
        optionalParameters.put("WIDTH", "120");
        optionalParameters.put("HEIGHT", "90");
        optionalParameters.put("LANGUAGE", "en");
        // ??optionalParameters.put("EXCEPTIONS", "");
        allParameters = new HashMap<>(requiredParameters);
        allParameters.putAll(optionalParameters);

        wms = getWMS();

        this.requestReader = new GetLegendGraphicKvpReader(wms);
        this.httpRequest = createRequest("wms", allParameters);
    }

    /**
     * This test ensures that when a SLD parameter has been passed that refers to a SLD document
     * with multiple styles, the required one is choosed based on the LAYER parameter.
     *
     * <p>This is the case where a remote SLD document is used in "library" mode.
     */
    @org.junit.Test
    public void testRemoteSLDMultipleStyles() throws Exception {
        final URL remoteSldUrl = getClass().getResource("MultipleStyles.sld");
        this.allParameters.put("SLD", remoteSldUrl.toExternalForm());

        this.allParameters.put("LAYER", "cite:Ponds");
        this.allParameters.put("STYLE", "Ponds");

        GetLegendGraphicRequest request =
                requestReader.read(new GetLegendGraphicRequest(), allParameters, allParameters);

        // the style names Ponds is declared in third position on the sld doc
        Style selectedStyle = request.getLegends().get(0).getStyle();
        assertNotNull(selectedStyle);
        assertEquals("Ponds", selectedStyle.getName());

        this.allParameters.put("LAYER", "cite:Lakes");
        this.allParameters.put("STYLE", "Lakes");

        request = requestReader.read(new GetLegendGraphicRequest(), allParameters, allParameters);

        // the style names Ponds is declared in third position on the sld doc
        selectedStyle = request.getLegends().get(0).getStyle();
        assertNotNull(selectedStyle);
        assertEquals("Lakes", selectedStyle.getName());
    }

    @org.junit.Test
    public void testRemoteSLDDisabled() {
        WMSInfo info = wms.getServiceInfo();
        info.setDynamicStylingDisabled(true);
        wms.getGeoServer().save(info);
        final URL remoteSldUrl = getClass().getResource("MultipleStyles.sld");
        this.allParameters.put("SLD", remoteSldUrl.toExternalForm());
        this.allParameters.put("LAYER", "cite:Ponds");
        ServiceException exception =
                assertThrows(
                        ServiceException.class,
                        () ->
                                requestReader.read(
                                        new GetLegendGraphicRequest(),
                                        allParameters,
                                        allParameters));
        assertEquals("Dynamic style usage is forbidden", exception.getMessage());
    }

    @org.junit.Test
    public void testMissingLayerParameter() throws Exception {
        requiredParameters.remove("LAYER");
        try {
            requestReader.read(
                    new GetLegendGraphicRequest(), requiredParameters, requiredParameters);
            fail("Expected ServiceException");
        } catch (ServiceException e) {
            assertEquals("LayerNotDefined", e.getCode());
        }
    }

    @org.junit.Test
    public void testMissingFormatParameter() throws Exception {
        requiredParameters.remove("FORMAT");
        try {
            requestReader.read(
                    new GetLegendGraphicRequest(), requiredParameters, requiredParameters);
            fail("Expected ServiceException");
        } catch (ServiceException e) {
            assertEquals("MissingFormat", e.getCode());
        }
    }

    @org.junit.Test
    public void testStrictParameter() throws Exception {

        // default value
        GetLegendGraphicRequest request =
                requestReader.read(new GetLegendGraphicRequest(), allParameters, allParameters);
        assertTrue(request.isStrict());

        allParameters.put("STRICT", "false");
        allParameters.remove("LAYER");
        request = requestReader.read(new GetLegendGraphicRequest(), allParameters, allParameters);
        assertFalse(request.isStrict());
    }

    @org.junit.Test
    public void testLayerGroup() throws Exception {

        GetLegendGraphicRequest request =
                requestReader.read(
                        new GetLegendGraphicRequest(), requiredParameters, requiredParameters);
        assertEquals(1, request.getLegends().size());

        requiredParameters.put("LAYER", NATURE_GROUP);
        request =
                requestReader.read(
                        new GetLegendGraphicRequest(), requiredParameters, requiredParameters);
        assertTrue(request.getLegends().size() > 1);
    }

    @org.junit.Test
    public void testLanguage() throws Exception {

        GetLegendGraphicRequest request =
                requestReader.read(
                        new GetLegendGraphicRequest(), requiredParameters, requiredParameters);
        assertNull(request.getLocale());

        request = requestReader.read(new GetLegendGraphicRequest(), allParameters, allParameters);
        assertEquals(Locale.ENGLISH, request.getLocale());
    }

    @org.junit.Test
    public void testStylesForLayerGroup() throws Exception {
        requiredParameters.put("LAYER", NATURE_GROUP);
        requiredParameters.put("STYLE", "");
        GetLegendGraphicRequest request =
                requestReader.read(
                        new GetLegendGraphicRequest(), requiredParameters, requiredParameters);
        assertEquals(2, request.getLegends().size());
    }

    @org.junit.Test
    public void testRulesForLayerGroup() throws Exception {

        requiredParameters.put("LAYER", NATURE_GROUP);
        requiredParameters.put("RULE", "rule1,rule2");
        GetLegendGraphicRequest request =
                requestReader.read(
                        new GetLegendGraphicRequest(), requiredParameters, requiredParameters);
        assertEquals(2, request.getLegends().size());
    }

    @org.junit.Test
    public void testLabelsForLayerGroup() throws Exception {

        requiredParameters.put("LAYER", NATURE_GROUP);
        GetLegendGraphicRequest request =
                requestReader.read(
                        new GetLegendGraphicRequest(), requiredParameters, requiredParameters);
        assertNotNull(
                request.getLegend(new NameImpl("http://www.opengis.net/cite", "Lakes")).getTitle());
    }

    @org.junit.Test
    public void testI18nLabelAndDescription() throws Exception {
        FeatureTypeInfo old = getCatalog().getFeatureTypeByName("cite", "Ponds");
        try {
            FeatureTypeInfo fti = getCatalog().getFeatureTypeByName("cite", "Ponds");
            GrowableInternationalString i18nTitle = new GrowableInternationalString();
            i18nTitle.add(Locale.ENGLISH, "en title");
            i18nTitle.add(Locale.ITALIAN, "it title");
            fti.setInternationalTitle(i18nTitle);
            getCatalog().save(fti);

            this.allParameters.put("LAYER", "cite:Ponds");
            this.allParameters.put("STYLE", "Ponds");
            this.allParameters.put("LANGUAGE", "it");
            GetLegendGraphicRequest request =
                    requestReader.read(new GetLegendGraphicRequest(), allParameters, allParameters);
            GetLegendGraphicRequest.LegendRequest legend =
                    request.getLegend(new NameImpl("http://www.opengis.net/cite", "Ponds"));
            assertEquals(legend.getTitle(), "it title");

            this.allParameters.put("LANGUAGE", "en");
            request =
                    requestReader.read(new GetLegendGraphicRequest(), allParameters, allParameters);
            legend = request.getLegend(new NameImpl("http://www.opengis.net/cite", "Ponds"));
            assertEquals(legend.getTitle(), "en title");
        } finally {
            getCatalog().save(old);
        }
    }
}
