/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.params.extractor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Enumeration;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import org.geoserver.config.GeoServerDataDirectory;
import org.junit.Before;
import org.junit.Test;

public class FilterTest extends TestSupport {
    FilterConfig filterConfig;

    @Before
    public void setUp() {
        filterConfig =
                new FilterConfig() {

                    @Override
                    public ServletContext getServletContext() {
                        return null;
                    }

                    @Override
                    public Enumeration<String> getInitParameterNames() {
                        return null;
                    }

                    @Override
                    public String getInitParameter(String arg0) {
                        return null;
                    }

                    @Override
                    public String getFilterName() {
                        return null;
                    }
                };
        Rule ruleA =
                new RuleBuilder()
                        .withId("0")
                        .withActivated(true)
                        .withPosition(3)
                        .withParameter("cql_filter")
                        .withTransform("CFCC='$2'")
                        .build();
        RulesDao.saveOrUpdateRule(ruleA);

        Filter.USE_AS_SERVLET_FILTER = false;
    }

    @Test
    public void testAsServletFilter() throws ServletException {
        Filter filter = new Filter();
        filter.init(filterConfig);
        assertTrue(filter.isEnabled());
    }

    @Test
    public void testAsSpringFilter() throws Exception {
        GeoServerDataDirectory dataDirectory =
                APPLICATION_CONTEXT.getBean(GeoServerDataDirectory.class);
        Filter filter = new Filter(dataDirectory);
        assertTrue(filter.isEnabled());
    }

    @Test
    public void testServletFilterHasPriorityOverSpring() throws ServletException {
        Filter servletFilter = new Filter();
        servletFilter.init(filterConfig);

        GeoServerDataDirectory dataDirectory =
                APPLICATION_CONTEXT.getBean(GeoServerDataDirectory.class);
        Filter springFilter = new Filter(dataDirectory);

        assertTrue(servletFilter.isEnabled());
        assertFalse(springFilter.isEnabled());
    }
}
