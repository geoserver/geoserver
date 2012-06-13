package org.geoserver.rest;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;

import org.geoserver.security.RESTfulDefinitionSource;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.RESTAccessRuleDAO;
import org.geoserver.test.GeoServerTestSupport;
import org.springframework.security.access.ConfigAttribute;

public class RESTSecurityRulesTest extends GeoServerTestSupport {

    RESTfulDefinitionSource defSource = null;
    RESTAccessRuleDAO dao = null;
    
    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();

        defSource = (RESTfulDefinitionSource) applicationContext.getBean("restFilterDefinitionMap");
        dao = (RESTAccessRuleDAO) applicationContext.getBean("restRulesDao");
    }

    public void testDefault() throws Exception {
        Collection<ConfigAttribute> atts = defSource.lookupAttributes("/foo", "GET");
        assertEquals(1, atts.size());
        assertEquals(GeoServerRole.ADMIN_ROLE.getAuthority(), atts.iterator().next().getAttribute());
    }

    public void testException() throws Exception {
        FileWriter fw = writer();
        fw.write("/foo;GET=IS_AUTHENTICATED_ANONYMOUSLY\n");
        fw.write("/**;GET=ROLE_ADMINISTRATOR\n");
        fw.flush();
        fw.close();

        //seems to be a delay of updating the timestamp on file or something, causing written 
        // fules to not be reloaded, so just do it manually
        dao.reload();
        defSource.reload();

        Collection<ConfigAttribute> atts = defSource.lookupAttributes("/foo", "GET");
        assertEquals(1, atts.size());
        assertEquals("IS_AUTHENTICATED_ANONYMOUSLY", atts.iterator().next().getAttribute());
    }

    public void testExceptionAfter() throws Exception {
        FileWriter fw = writer();
        fw.write("/**;GET=ROLE_ADMINISTRATOR\n");
        fw.write("/foo;GET=IS_AUTHENTICATED_ANONYMOUSLY\n");
        fw.flush();
        fw.close();

        dao.reload();
        defSource.reload();

        Collection<ConfigAttribute> atts = defSource.lookupAttributes("/foo", "GET");
        assertEquals(1, atts.size());
        assertEquals("ROLE_ADMINISTRATOR", atts.iterator().next().getAttribute());
    }

    FileWriter writer() throws IOException {
        return new FileWriter(new File(getDataDirectory().findSecurityRoot(), "rest.properties"));
    }
}
