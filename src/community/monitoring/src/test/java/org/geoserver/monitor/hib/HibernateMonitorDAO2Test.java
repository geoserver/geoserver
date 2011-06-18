package org.geoserver.monitor.hib;

import org.geoserver.monitor.MonitorDAOTestSupport;
import org.geoserver.monitor.MonitorConfig.Mode;
import org.geoserver.monitor.MonitorConfig.Sync;
import org.h2.tools.DeleteDbFiles;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.springframework.web.context.support.XmlWebApplicationContext;

public class HibernateMonitorDAO2Test extends MonitorDAOTestSupport {

    @BeforeClass
    public static void initHibernate() throws Exception {
        XmlWebApplicationContext ctx = new XmlWebApplicationContext() {
            public String[] getConfigLocations() {
                return new String[]{
                    "classpath*:applicationContext-hibtest.xml",
                    "classpath*:applicationContext-hib2.xml"};
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
        DeleteDbFiles.execute("target/monitoring", "monitoring", false);
    }
}
