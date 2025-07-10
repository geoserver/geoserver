/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import javax.media.jai.ImageLayout;
import org.easymock.EasyMock;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageView;
import org.geoserver.catalog.CoverageViewReader;
import org.geoserver.catalog.Predicates;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.CoverageAccessLimits;
import org.geoserver.security.WrapperPolicy;
import org.geoserver.security.impl.SecureObjectsTest;
import org.geotools.api.coverage.grid.Format;
import org.geotools.api.filter.Filter;
import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.api.parameter.ParameterValue;
import org.geotools.coverage.grid.GeneralGridEnvelope;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.geometry.GeneralBounds;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;

public class SecuredGridCoverage2DReaderTest extends SecureObjectsTest {

    @Test
    public void testFilter() throws Exception {
        final Filter securityFilter = ECQL.toFilter("A > 10");
        final Filter requestFilter = ECQL.toFilter("B < 10");

        // create the mocks we need
        Format format = setupFormat();
        GridCoverage2DReader reader = createNiceMock(GridCoverage2DReader.class);
        expect(reader.getFormat()).andReturn(format).anyTimes();

        setupReadAssertion(reader, requestFilter, securityFilter);

        CoverageAccessLimits accessLimits = new CoverageAccessLimits(CatalogMode.HIDE, securityFilter, null, null);
        SecuredGridCoverage2DReader secured =
                new SecuredGridCoverage2DReader(reader, WrapperPolicy.readOnlyHide(accessLimits));

        final ParameterValue pv = ImageMosaicFormat.FILTER.createValue();
        pv.setValue(requestFilter);
        secured.read(pv);
    }

    @Test
    public void testFilterOnStructured() throws Exception {
        final Filter securityFilter = ECQL.toFilter("A > 10");
        final Filter requestFilter = ECQL.toFilter("B < 10");
        DefaultSecureDataFactory factory = new DefaultSecureDataFactory();

        // create the mocks we need
        Format format = setupFormat();
        StructuredGridCoverage2DReader reader = createNiceMock(StructuredGridCoverage2DReader.class);
        expect(reader.getFormat()).andReturn(format).anyTimes();

        setupReadAssertion(reader, requestFilter, securityFilter);

        CoverageAccessLimits accessLimits = new CoverageAccessLimits(CatalogMode.HIDE, securityFilter, null, null);
        Object securedObject = factory.secure(reader, WrapperPolicy.readOnlyHide(accessLimits));
        assertTrue(securedObject instanceof SecuredStructuredGridCoverage2DReader);
        SecuredStructuredGridCoverage2DReader secured = (SecuredStructuredGridCoverage2DReader) securedObject;

        final ParameterValue pv = ImageMosaicFormat.FILTER.createValue();
        pv.setValue(requestFilter);
        secured.read(pv);
    }

    @Test
    public void testCoverageViewSecured() throws Exception {
        DefaultSecureDataFactory factory = new DefaultSecureDataFactory();

        GridCoverage2DReader reader = createNiceMock(GridCoverage2DReader.class);
        expect(reader.getOriginalEnvelope())
                .andReturn(new GeneralBounds(new double[] {-90, -90}, new double[] {90, 90}))
                .anyTimes();
        expect(reader.getOriginalGridRange())
                .andReturn(new GeneralGridEnvelope(new Rectangle(0, 0, 100, 100)))
                .anyTimes();
        expect(reader.getCoordinateReferenceSystem())
                .andReturn(DefaultGeographicCRS.WGS84)
                .anyTimes();
        BufferedImage bi = new BufferedImage(100, 100, BufferedImage.TYPE_BYTE_GRAY);
        expect(reader.getImageLayout("coverageView"))
                .andReturn(new ImageLayout(bi))
                .anyTimes();
        replay(reader);

        CoverageView coverageView = createNiceMock(CoverageView.class);
        CoverageInfo coverageInfo = createNiceMock(CoverageInfo.class);
        expect(coverageView.getName()).andReturn("coverageView").anyTimes();
        CoverageView.CoverageBand coverageBand = new CoverageView.CoverageBand(
                Collections.singletonList(new CoverageView.InputCoverageBand("coverageView", "band1")),
                "band1",
                0,
                CoverageView.CompositionType.BAND_SELECT);
        List<CoverageView.CoverageBand> bands = Collections.singletonList(coverageBand);
        expect(coverageView.getCoverageBands()).andReturn(bands).anyTimes();
        expect(coverageView.getBand(0)).andReturn(coverageBand).anyTimes();
        expect(coverageView.getSelectedResolution())
                .andReturn(CoverageView.SelectedResolution.BEST)
                .anyTimes();
        expect(coverageView.getEnvelopeCompositionType())
                .andReturn(CoverageView.EnvelopeCompositionType.UNION)
                .anyTimes();
        replay(coverageView);

        CoverageViewReader viewReader = new CoverageViewReader(reader, coverageView, coverageInfo, null);
        CoverageAccessLimits accessLimits = new CoverageAccessLimits(CatalogMode.HIDE, null, null, null);

        Object securedObject = factory.secure(viewReader, WrapperPolicy.readOnlyHide(accessLimits));
        assertTrue(securedObject instanceof SecuredGridCoverage2DReader);

        securedObject = factory.secure(viewReader.getFormat(), WrapperPolicy.readOnlyHide(accessLimits));
        assertTrue(securedObject instanceof SecuredGridFormat);
    }

    private static void setupReadAssertion(
            GridCoverage2DReader reader, final Filter requestFilter, final Filter securityFilter) throws IOException {
        // the assertion
        expect(reader.read(isA(GeneralParameterValue[].class))).andAnswer(() -> {
            GeneralParameterValue[] params = (GeneralParameterValue[]) EasyMock.getCurrentArguments()[0];
            ParameterValue param = (ParameterValue) params[0];
            Filter filter = (Filter) param.getValue();
            assertEquals(Predicates.and(requestFilter, securityFilter), filter);
            return null;
        });
        EasyMock.replay(reader);
    }

    private Format setupFormat() {
        Format format = createNiceMock(Format.class);
        expect(format.getReadParameters())
                .andReturn(new ImageMosaicFormat().getReadParameters())
                .anyTimes();
        EasyMock.replay(format);
        return format;
    }
}
