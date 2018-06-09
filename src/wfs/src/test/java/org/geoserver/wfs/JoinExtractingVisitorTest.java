/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.List;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.data.Join;
import org.geotools.factory.CommonFactoryFinder;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

public class JoinExtractingVisitorTest {

    private FeatureTypeInfo lakes;

    private FeatureTypeInfo forests;

    private FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

    private FeatureTypeInfo buildings;

    @Before
    public void setup() {
        lakes = createNiceMock(FeatureTypeInfo.class);
        expect(lakes.prefixedName()).andReturn("gs:Lakes").anyTimes();
        expect(lakes.getNativeName()).andReturn("Lakes").anyTimes();
        expect(lakes.getName()).andReturn("Lakes").anyTimes();

        forests = createNiceMock(FeatureTypeInfo.class);
        expect(forests.prefixedName()).andReturn("gs:Forests").anyTimes();
        expect(forests.getNativeName()).andReturn("Forests").anyTimes();
        expect(forests.getName()).andReturn("Forests").anyTimes();

        buildings = createNiceMock(FeatureTypeInfo.class);
        expect(buildings.prefixedName()).andReturn("gs:Buildings").anyTimes();
        expect(buildings.getNativeName()).andReturn("Buildings").anyTimes();
        expect(buildings.getName()).andReturn("Buildings").anyTimes();

        replay(lakes, forests, buildings);
    }

    @Test
    public void testTwoWayJoin() {
        JoinExtractingVisitor visitor =
                new JoinExtractingVisitor(Arrays.asList(lakes, forests), Arrays.asList("a", "b"));
        Filter f = ff.equals(ff.property("a/FID"), ff.property("b/FID"));
        f.accept(visitor, null);

        assertEquals("a", visitor.getPrimaryAlias());

        Filter primary = visitor.getPrimaryFilter();
        assertNull(primary);

        List<Join> joins = visitor.getJoins();
        assertEquals(1, joins.size());
        Join join = joins.get(0);
        assertEquals("Forests", join.getTypeName());
        assertEquals("b", join.getAlias());
        assertEquals(ff.equals(ff.property("a.FID"), ff.property("b.FID")), join.getJoinFilter());
    }

    @Test
    public void testThreeWayJoinWithAliases() {
        JoinExtractingVisitor visitor =
                new JoinExtractingVisitor(
                        Arrays.asList(lakes, forests, buildings), Arrays.asList("a", "b", "c"));
        Filter f1 = ff.equals(ff.property("a/FID"), ff.property("b/FID"));
        Filter f2 = ff.equals(ff.property("b/FID"), ff.property("c/FID"));
        Filter f = ff.and(Arrays.asList(f1, f2));
        testThreeWayJoin(visitor, f);
    }

    @Test
    public void testThreeWayJoinNoAliasesUnqualified() {
        JoinExtractingVisitor visitor =
                new JoinExtractingVisitor(Arrays.asList(lakes, forests, buildings), null);
        Filter f1 = ff.equals(ff.property("Lakes/FID"), ff.property("Forests/FID"));
        Filter f2 = ff.equals(ff.property("Forests/FID"), ff.property("Buildings/FID"));
        Filter f = ff.and(Arrays.asList(f1, f2));
        testThreeWayJoin(visitor, f);
    }

    @Test
    public void testThreeWayJoinNoAliasesQualified() {
        JoinExtractingVisitor visitor =
                new JoinExtractingVisitor(Arrays.asList(lakes, forests, buildings), null);
        Filter f1 = ff.equals(ff.property("gs:Lakes/FID"), ff.property("gs:Forests/FID"));
        Filter f2 = ff.equals(ff.property("gs:Forests/FID"), ff.property("gs:Buildings/FID"));
        Filter f = ff.and(Arrays.asList(f1, f2));
        testThreeWayJoin(visitor, f);
    }

    private void testThreeWayJoin(JoinExtractingVisitor visitor, Filter f) {
        f.accept(visitor, null);

        assertEquals("b", visitor.getPrimaryAlias());

        Filter primary = visitor.getPrimaryFilter();
        assertNull(primary);

        List<Join> joins = visitor.getJoins();
        assertEquals(2, joins.size());

        Join j1 = joins.get(0);
        assertEquals("Lakes", j1.getTypeName());
        assertEquals("a", j1.getAlias());
        assertEquals(ff.equals(ff.property("a.FID"), ff.property("b.FID")), j1.getJoinFilter());

        Join j2 = joins.get(1);
        assertEquals("Buildings", j2.getTypeName());
        assertEquals("c", j2.getAlias());
        assertEquals(ff.equals(ff.property("b.FID"), ff.property("c.FID")), j2.getJoinFilter());
    }

    @Test
    public void testThreeWayJoinPrimaryFilters() {
        JoinExtractingVisitor visitor =
                new JoinExtractingVisitor(
                        Arrays.asList(lakes, forests, buildings), Arrays.asList("a", "b", "c"));
        Filter fj1 = ff.equals(ff.property("a/FID"), ff.property("b/FID"));
        Filter fj2 = ff.equals(ff.property("b/FID"), ff.property("c/FID"));
        Filter f1 = ff.equals(ff.property("a/FID"), ff.literal("Lakes.10"));
        Filter f2 = ff.equals(ff.property("b/FID"), ff.literal("Forests.10"));
        Filter f3 = ff.equals(ff.property("c/FID"), ff.literal("Buildings.10"));
        Filter f = ff.and(Arrays.asList(f1, f2, f3, fj1, fj2));
        f.accept(visitor, null);

        assertEquals("b", visitor.getPrimaryAlias());

        Filter primary = visitor.getPrimaryFilter();
        assertEquals(ff.equals(ff.property("FID"), ff.literal("Forests.10")), primary);

        List<Join> joins = visitor.getJoins();
        assertEquals(2, joins.size());

        Join j1 = joins.get(0);
        assertEquals("Lakes", j1.getTypeName());
        assertEquals("a", j1.getAlias());
        assertEquals(ff.equals(ff.property("a.FID"), ff.property("b.FID")), j1.getJoinFilter());
        assertEquals(ff.equals(ff.property("FID"), ff.literal("Lakes.10")), j1.getFilter());

        Join j2 = joins.get(1);
        assertEquals("Buildings", j2.getTypeName());
        assertEquals("c", j2.getAlias());
        assertEquals(ff.equals(ff.property("b.FID"), ff.property("c.FID")), j2.getJoinFilter());
        assertEquals(ff.equals(ff.property("FID"), ff.literal("Buildings.10")), j2.getFilter());
    }

    @Test
    public void testThreeWayJoinWithSelf() {
        JoinExtractingVisitor visitor =
                new JoinExtractingVisitor(
                        Arrays.asList(forests, lakes, lakes), Arrays.asList("a", "b", "c"));
        Filter f1 = ff.equals(ff.property("a/FID"), ff.property("b/FID"));
        Filter f2 = ff.equals(ff.property("b/FID"), ff.property("c/FID"));
        Filter f = ff.and(Arrays.asList(f1, f2));
        f.accept(visitor, null);

        assertEquals("b", visitor.getPrimaryAlias());

        Filter primary = visitor.getPrimaryFilter();
        assertNull(primary);

        List<Join> joins = visitor.getJoins();
        assertEquals(2, joins.size());

        Join j1 = joins.get(0);
        assertEquals("Forests", j1.getTypeName());
        assertEquals("a", j1.getAlias());
        assertEquals(ff.equals(ff.property("a.FID"), ff.property("b.FID")), j1.getJoinFilter());

        Join j2 = joins.get(1);
        assertEquals("Lakes", j2.getTypeName());
        assertEquals("c", j2.getAlias());
        assertEquals(ff.equals(ff.property("b.FID"), ff.property("c.FID")), j2.getJoinFilter());
    }
}
