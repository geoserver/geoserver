/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor.hib;

import static org.geoserver.monitor.MonitorTestData.assertCovered;
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.apache.commons.io.IOUtils;
import org.geoserver.hibernate.HibUtil;
import org.geoserver.monitor.Filter;
import org.geoserver.monitor.MonitorConfig.Mode;
import org.geoserver.monitor.MonitorDAOTestSupport;
import org.geoserver.monitor.Query;
import org.geoserver.monitor.Query.Comparison;
import org.geoserver.monitor.Query.SortOrder;
import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.RequestDataVisitor;
import org.geoserver.monitor.hib.HibernateMonitorDAO2.Sync;
import org.h2.tools.DeleteDbFiles;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.web.context.support.XmlWebApplicationContext;

public class HibernateMonitorDAO2Test extends MonitorDAOTestSupport {

    private static XmlWebApplicationContext ctx;

    @BeforeClass
    public static void initHibernate() throws Exception {

        // setup in memory h2 db
        Properties p = new Properties();
        p.put("driver", "org.h2.Driver");
        p.put("url", "jdbc:h2:mem:monitoring");
        File file = new File("./target/monitoring/db.properties");
        
        if (!file.getParentFile().exists()) {
            assertTrue(file.getParentFile().mkdirs());
        }
        try (FileOutputStream fos = new FileOutputStream(file)) {
            p.store(fos, null);
        }

        ctx =
                new XmlWebApplicationContext() {
                    public String[] getConfigLocations() {
                        return new String[] {
                            "classpath*:applicationContext-hibtest.xml",
                            "classpath*:applicationContext-hib2.xml"
                        };
                    }
                };
        ctx.refresh();
        HibernateMonitorDAO2 hibdao = (HibernateMonitorDAO2) ctx.getBean("hibMonitorDAO");
        hibdao.setSync(Sync.SYNC);
        hibdao.setMode(Mode.HYBRID);
        dao = hibdao;

        setUpData();
    }

    @AfterClass
    public static void destroy() throws Exception {
        dao.dispose();
        ctx.close();
        DeleteDbFiles.execute("target/monitoring", "monitoring", false);
    }

    @Before
    public void setUpSession() throws Exception {
        HibUtil.setUpSession(((HibernateMonitorDAO2) dao).getSessionFactory());
    }

    @After
    public void tearDownSession() throws Exception {
        HibUtil.tearDownSession(((HibernateMonitorDAO2) dao).getSessionFactory(), null);
    }

    @Test
    public void testGetRequestsFilterIN3() throws Exception {
        List<RequestData> datas =
                dao.getRequests(new Query().filter("widgets", "resources", Comparison.IN));
        assertCovered(datas, 11, 14, 18);
    }

    @Test
    public void testGetRequestsAggregate() throws Exception {
        final List<RequestData> datas = new ArrayList();
        final List<Object> aggs = new ArrayList();

        RequestDataVisitor v =
                new RequestDataVisitor() {
                    public void visit(RequestData data, Object... aggregates) {
                        datas.add(data);
                        aggs.addAll(Arrays.asList(aggregates));
                    }
                };
        dao.getRequests(
                new Query()
                        .properties("path")
                        .filter("path", "/foo", Comparison.EQ)
                        .aggregate("count()")
                        .group("path"),
                v);

        assertEquals(1, datas.size());
        assertEquals("/foo", datas.get(0).getPath());
        assertEquals(4, ((Number) aggs.get(0)).intValue());

        datas.clear();
        aggs.clear();

        dao.getRequests(
                new Query()
                        .properties("service", "operation")
                        .filter("service", null, Comparison.NEQ)
                        .aggregate("count()")
                        .group("service", "operation")
                        .sort("count()", SortOrder.DESC),
                v);

        RequestData r = datas.get(0);
        assertEquals("foo", r.getService());
        assertEquals("x", r.getOperation());
        assertEquals(4, ((Number) aggs.get(0)).intValue());

        r = datas.get(1);
        assertEquals("bam", r.getService());
        assertEquals("y", r.getOperation());
        assertEquals(2, ((Number) aggs.get(1)).intValue());
    }

    @Test
    public void testGetRequestsCount() throws Exception {
        final List<Object> aggs = new ArrayList();

        RequestDataVisitor v =
                new RequestDataVisitor() {
                    public void visit(RequestData data, Object... aggregates) {
                        aggs.addAll(Arrays.asList(aggregates));
                    }
                };
        dao.getRequests(new Query().aggregate("count()").filter("path", "/foo", Comparison.EQ), v);

        assertEquals(1, aggs.size());
        assertEquals(4, ((Number) aggs.get(0)).intValue());
    }

    @Test
    public void testGetRequestsFilterAnd() throws Exception {
        assertEquals(
                1,
                dao.getRequests(
                                new Query()
                                        .filter("path", "/foo", Comparison.EQ)
                                        .filter("widgets", "resources", Comparison.IN))
                        .size());
    }

    @Test
    public void testGetRequestsFilterOr() throws Exception {
        assertEquals(
                4,
                dao.getRequests(
                                new Query()
                                        .filter("path", "/seven", Comparison.EQ)
                                        .or("widgets", "resources", Comparison.IN))
                        .size());
    }

    @Test
    public void testGetRequestsJoin() throws Exception {
        List<RequestData> datas =
                dao.getRequests(
                        new Query()
                                .properties("path", "resource")
                                .filter("path", "/foo", Comparison.EQ)
                                .group("path", "resource")
                                .sort("resource", SortOrder.ASC));

        assertEquals(3, datas.size());
        assertEquals("stuff", datas.get(0).getResources().get(0));
        assertEquals("things", datas.get(1).getResources().get(0));
        assertEquals("widgets", datas.get(2).getResources().get(0));
    }

    @Test
    public void testGetRequestsJoinVisitor() throws Exception {
        final List<RequestData> datas = new ArrayList();
        final List<Object> aggs = new ArrayList();

        RequestDataVisitor v =
                new RequestDataVisitor() {
                    public void visit(RequestData data, Object... aggregates) {
                        datas.add(data);
                        // aggs.addAll(Arrays.asList(aggregates));
                    }
                };
        dao.getRequests(
                new Query()
                        .properties("path", "resource")
                        .filter("path", "/foo", Comparison.EQ)
                        .group("path", "resource")
                        .sort("resource", SortOrder.ASC),
                v);

        assertEquals(3, datas.size());
        assertEquals(1, datas.get(0).getResources().size());
        assertEquals("stuff", datas.get(0).getResources().get(0));
        assertEquals(1, datas.get(1).getResources().size());
        assertEquals("things", datas.get(1).getResources().get(0));
        assertEquals(1, datas.get(2).getResources().size());
        assertEquals("widgets", datas.get(2).getResources().get(0));
    }

    @Test
    public void testGetRequestsJoin2() throws Exception {
        final List<RequestData> datas = new ArrayList();
        final List<Object> aggs = new ArrayList();

        dao.getRequests(
                new Query()
                        .properties("resource")
                        .aggregate("count()")
                        .filter("resource", null, Comparison.NEQ)
                        .group("resource"),
                new RequestDataVisitor() {
                    public void visit(RequestData data, Object... aggregates) {
                        datas.add(data);
                        aggs.add(aggregates[0]);
                    }
                });

        // assertEquals(3, datas.size());
        for (RequestData data : datas) {
            System.out.println(data.getResources());
        }
    }

    @Test
    public void testGetRequestsJoinIN() throws Exception {
        List<String> resources = Arrays.asList("widgets", "things");
        List<RequestData> datas =
                dao.getRequests(
                        new Query()
                                .properties("resource")
                                .aggregate("count()")
                                .filter("resource", resources, Comparison.IN)
                                .group("resource")
                                .sort("resource", SortOrder.ASC));

        assertEquals(2, datas.size());
        assertEquals("things", datas.get(0).getResources().get(0));
        assertEquals("widgets", datas.get(1).getResources().get(0));
    }

    @Test
    public void testGetRequestsAdvancedFilter() throws Exception {
        Filter filter =
                new Filter("path", "/four", Comparison.EQ)
                        .or(
                                new Filter("service", "foo", Comparison.EQ)
                                        .and(
                                                new Filter(
                                                        "resource",
                                                        Arrays.asList("widgets"),
                                                        Comparison.IN)));

        List<RequestData> datas = dao.getRequests(new Query().filter(filter));
        assertEquals(2, datas.size());
        assertCovered(datas, 4, 11);
    }

    //    @Test
    //    public void testFoo() throws Exception {
    //        SessionFactory sessionFactory = ((HibernateMonitorDAO2)dao).getSessionFactory();
    //        Session session = sessionFactory.getCurrentSession();
    //
    //        /*Query q = session.createQuery("SELECT rd.path FROM RequestData rd, LayerData ld " +
    //                "WHERE ld in elements(rd.layers) " +
    //                "AND ld.name = 'things'");*/
    //        /*Query q = session.createQuery("SELECT rd.path FROM RequestData rd " +
    //            "INNER JOIN rd.layers as layer WITH layer.name = 'things'");*/
    //        Query q = session.createQuery(
    //            "SELECT r.path, layer FROM RequestData r LEFT JOIN r.layers as layer " +
    //            " WHERE r.path = '/foo' GROUP BY r.path, layer");
    //
    //        for (Object o : q.list()) {
    //            Object[] vals = (Object[]) o;
    //            System.out.println(String.format("%s, %s", vals[0].toString(),
    // vals[1].toString()));
    //        }
    //
    //    }
}
