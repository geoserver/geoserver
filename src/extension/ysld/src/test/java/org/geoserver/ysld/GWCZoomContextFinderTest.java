/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 - 2016 Boundless Spatial Inc.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.ysld;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.geotools.ysld.TestUtils.rangeContains;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.*;

import org.geotools.ysld.parse.ScaleRange;
import org.geotools.ysld.parse.ZoomContext;
import org.geotools.ysld.parse.ZoomContextFinder;
import org.geowebcache.grid.Grid;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSetBroker;
import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;

public class GWCZoomContextFinderTest {

    private static final double EPSILON = 0.000000001d;

    Grid mockGrid(int level, double denom, GridSet mockGridset) {
        Grid grid = createMock(Grid.class);
        expect(mockGridset.getGrid(level)).andStubReturn(grid);
        expect(grid.getScaleDenominator()).andStubReturn(denom);
        return grid;
    }

    @Test
    public void testGetContext() throws Exception {
        GridSetBroker broker = createMock(GridSetBroker.class);
        GridSet set = createMock(GridSet.class);

        expect(broker.get("test")).andReturn(set);
        Grid grid1 = mockGrid(1, 500_000_000d, set);

        replay(broker, set, grid1);

        ZoomContextFinder finder = new GWCZoomContextFinder(broker);

        ZoomContext zContext = finder.get("test");

        assertThat(zContext, notNullValue());

        verify(broker, set, grid1);
    }

    @Test
    public void testCouldntFind() throws Exception {
        GridSetBroker broker = createMock(GridSetBroker.class);
        GridSet set = createMock(GridSet.class);

        expect(broker.get("test")).andStubReturn(set);
        expect(broker.get("doesntexist")).andStubReturn(null);
        Grid grid1 = mockGrid(1, 500_000_000d, set);

        replay(broker, set, grid1);

        ZoomContextFinder finder = new GWCZoomContextFinder(broker);

        ZoomContext zContext = finder.get("doesntexist");

        assertThat(zContext, nullValue());

        verify(broker, set, grid1);
    }

    @Test
    public void testCorrectScale() throws Exception {
        GridSetBroker broker = createMock(GridSetBroker.class);
        GridSet set = createMock(GridSet.class);

        expect(broker.get("test")).andStubReturn(set);
        Grid grid1 = mockGrid(1, 500_000_000d, set);
        expect(set.getNumLevels()).andStubReturn(5);

        replay(broker, set, grid1);

        ZoomContextFinder finder = new GWCZoomContextFinder(broker);

        ZoomContext zContext = finder.get("test");

        double denom = zContext.getScaleDenominator(1);

        assertThat(denom, closeTo(500_000_000d, EPSILON));

        verify(broker, set, grid1);
    }

    @Test
    public void testScaleNegativeLevel() throws Exception {
        GridSetBroker broker = createMock(GridSetBroker.class);
        GridSet set = createMock(GridSet.class);

        expect(broker.get("test")).andStubReturn(set);
        expect(set.getNumLevels()).andStubReturn(5);

        replay(broker, set);

        ZoomContextFinder finder = new GWCZoomContextFinder(broker);

        ZoomContext zContext = finder.get("test");

        double denom = zContext.getScaleDenominator(-1);

        assertThat(denom, is(Double.POSITIVE_INFINITY));

        verify(broker, set);
    }

    @Test
    public void testScalePastEnd() throws Exception {
        GridSetBroker broker = createMock(GridSetBroker.class);
        GridSet set = createMock(GridSet.class);

        expect(broker.get("test")).andStubReturn(set);
        expect(set.getNumLevels()).andStubReturn(5);

        replay(broker, set);

        ZoomContextFinder finder = new GWCZoomContextFinder(broker);

        ZoomContext zContext = finder.get("test");

        double denom = zContext.getScaleDenominator(5);

        assertThat(denom, is(0d));

        verify(broker, set);
    }

    @Test
    public void testRange() throws Exception {
        GridSetBroker broker = createMock(GridSetBroker.class);
        GridSet set = createMock(GridSet.class);

        expect(broker.get("test")).andStubReturn(set);
        Grid grid1 = mockGrid(1, 500_000_000d, set);
        Grid grid2 = mockGrid(2, 200_000_000d, set);
        Grid grid3 = mockGrid(3, 100_000_000d, set);
        expect(set.getNumLevels()).andStubReturn(5);

        replay(broker, set, grid1, grid2, grid3);

        ZoomContextFinder finder = new GWCZoomContextFinder(broker);

        ZoomContext zContext = finder.get("test");

        ScaleRange range = zContext.getRange(2, 2);

        assertThat(range, rangeContains(200_000_000d));
        assertThat(range, not(rangeContains(500_000_000d)));
        assertThat(range, not(rangeContains(100_000_000d)));

        verify(broker, set, grid1, grid2, grid3);
    }

    @Test
    public void testRangeStart() throws Exception {
        GridSetBroker broker = createMock(GridSetBroker.class);
        GridSet set = createMock(GridSet.class);

        expect(broker.get("test")).andStubReturn(set);
        Grid grid0 = mockGrid(0, 500_000_000d, set);
        Grid grid1 = mockGrid(1, 200_000_000d, set);
        Grid grid2 = mockGrid(2, 100_000_000d, set);
        expect(set.getNumLevels()).andStubReturn(5);

        replay(broker, set, grid0, grid1, grid2);

        ZoomContextFinder finder = new GWCZoomContextFinder(broker);

        ZoomContext zContext = finder.get("test");

        ScaleRange range = zContext.getRange(0, 1);

        assertThat(range, rangeContains(1 / EPSILON));
        assertThat(range, rangeContains(500_000_000d));
        assertThat(range, rangeContains(200_000_000d));
        assertThat(range, not(rangeContains(100_000_000d)));
        assertThat(range, not(rangeContains(EPSILON)));

        verify(broker, set, grid0, grid1, grid2);
    }

    @Test
    public void testRangeEnd() throws Exception {
        GridSetBroker broker = createMock(GridSetBroker.class);
        GridSet set = createMock(GridSet.class);

        expect(broker.get("test")).andStubReturn(set);
        Grid grid2 = mockGrid(2, 500_000_000d, set);
        Grid grid3 = mockGrid(3, 200_000_000d, set);
        Grid grid4 = mockGrid(4, 100_000_000d, set);
        expect(set.getNumLevels()).andStubReturn(5);

        replay(broker, set, grid2, grid3, grid4);

        ZoomContextFinder finder = new GWCZoomContextFinder(broker);

        ZoomContext zContext = finder.get("test");

        ScaleRange range = zContext.getRange(3, 4);

        assertThat(range, not(rangeContains(1 / EPSILON)));
        assertThat(range, not(rangeContains(500_000_000d)));
        assertThat(range, rangeContains(200_000_000d));
        assertThat(range, rangeContains(100_000_000d));
        assertThat(range, rangeContains(EPSILON));

        verify(broker, set, grid2, grid3, grid4);
    }

    @Test
    public void testRangePastEnd() throws Exception {
        GridSetBroker broker = createMock(GridSetBroker.class);
        GridSet set = createMock(GridSet.class);

        expect(broker.get("test")).andStubReturn(set);
        Grid grid2 = mockGrid(2, 500_000_000d, set);
        Grid grid3 = mockGrid(3, 200_000_000d, set);
        Grid grid4 = mockGrid(4, 100_000_000d, set);
        expect(set.getNumLevels()).andStubReturn(5);

        replay(broker, set, grid2, grid3, grid4);

        ZoomContextFinder finder = new GWCZoomContextFinder(broker);

        ZoomContext zContext = finder.get("test");

        ScaleRange range = zContext.getRange(6, 7);

        assertThat(range, not(rangeContains(1 / EPSILON)));
        assertThat(range, not(rangeContains(500_000_000d)));
        assertThat(range, not(rangeContains(200_000_000d)));
        assertThat(range, not(rangeContains(100_000_000d)));
        assertThat(range, not(rangeContains(EPSILON)));

        verify(broker, set, grid2, grid3, grid4);
    }

    @Test
    public void testRangePastStart() throws Exception {
        GridSetBroker broker = createMock(GridSetBroker.class);
        GridSet set = createMock(GridSet.class);

        expect(broker.get("test")).andStubReturn(set);
        Grid grid2 = mockGrid(2, 500_000_000d, set);
        Grid grid3 = mockGrid(3, 200_000_000d, set);
        Grid grid4 = mockGrid(4, 100_000_000d, set);
        expect(set.getNumLevels()).andStubReturn(5);

        replay(broker, set, grid2, grid3, grid4);

        ZoomContextFinder finder = new GWCZoomContextFinder(broker);

        ZoomContext zContext = finder.get("test");

        ScaleRange range = zContext.getRange(-2, -1);

        assertThat(range, not(rangeContains(1 / EPSILON)));
        assertThat(range, not(rangeContains(500_000_000d)));
        assertThat(range, not(rangeContains(200_000_000d)));
        assertThat(range, not(rangeContains(100_000_000d)));
        assertThat(range, not(rangeContains(EPSILON)));

        verify(broker, set, grid2, grid3, grid4);
    }

    @Test
    public void testRangeBoundaryLikeTileFuser() throws Exception {
        GridSetBroker broker = createMock(GridSetBroker.class);
        GridSet set = createMock(GridSet.class);

        expect(broker.get("test")).andStubReturn(set);
        Grid grid2 = mockGrid(2, 500_000_000d, set);
        Grid grid3 = mockGrid(3, 200_000_000d, set);
        Grid grid4 = mockGrid(4, 100_000_000d, set);
        expect(set.getNumLevels()).andStubReturn(5);

        replay(broker, set, grid2, grid3, grid4);

        ZoomContextFinder finder = new GWCZoomContextFinder(broker);

        ZoomContext zContext = finder.get("test");

        ScaleRange range = zContext.getRange(3, 3);

        assertThat(range.getMaxDenom(), Matchers.closeTo(500_000_000d / 1.005, EPSILON));
        assertThat(range.getMinDenom(), Matchers.closeTo(200_000_000d / 1.005, EPSILON));

        verify(broker, set, grid2, grid3, grid4);
    }

    @Test
    @Ignore
    public void testRangeBoundaryGeometric() throws Exception {
        GridSetBroker broker = createMock(GridSetBroker.class);
        GridSet set = createMock(GridSet.class);

        expect(broker.get("test")).andStubReturn(set);
        Grid grid2 = mockGrid(2, 500_000_000d, set);
        Grid grid3 = mockGrid(3, 200_000_000d, set);
        Grid grid4 = mockGrid(4, 100_000_000d, set);
        expect(set.getNumLevels()).andStubReturn(5);

        replay(broker, set, grid2, grid3, grid4);

        ZoomContextFinder finder = new GWCZoomContextFinder(broker);

        ZoomContext zContext = finder.get("test");

        ScaleRange range = zContext.getRange(3, 3);

        assertThat(
                range.getMaxDenom(),
                Matchers.closeTo(Math.sqrt(500_000_000d * 200_000_000), EPSILON));
        assertThat(
                range.getMinDenom(),
                Matchers.closeTo(Math.sqrt(200_000_000d * 100_000_000), EPSILON));

        verify(broker, set, grid2, grid3, grid4);
    }
}
