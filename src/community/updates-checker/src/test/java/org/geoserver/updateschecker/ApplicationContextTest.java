/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.geoserver.platform.ModuleStatusImpl;
import org.geoserver.web.Category;
import org.geoserver.web.MenuPageInfo;
import org.junit.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationListener;
import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.core.io.ClassPathResource;

/**
 * The beans of {@code applicationContext.xml} that need no running GeoServer. The page goes in the first menu, which is
 * named differently in GeoServer 2 and 3, so each series is simulated with the category beans it defines.
 */
public class ApplicationContextTest {

    private static DefaultListableBeanFactory load(DefaultListableBeanFactory beans) {
        beans.setBeanExpressionResolver(new StandardBeanExpressionResolver());
        new XmlBeanDefinitionReader(beans).loadBeanDefinitions(new ClassPathResource("applicationContext.xml"));
        return beans;
    }

    private static MenuPageInfo page(DefaultListableBeanFactory beans) {
        return load(beans).getBean("updatesCheckerPage", MenuPageInfo.class);
    }

    @Test
    public void theModuleIsListedWithItsNameAndDocumentation() {
        ModuleStatusImpl status =
                load(new DefaultListableBeanFactory()).getBean("updatesCheckerModuleStatus", ModuleStatusImpl.class);
        assertEquals("gs-updates-checker", status.getModule());
        assertEquals("Check for Updates", status.getName());
        assertEquals(
                "community/updates-checker/index.html",
                status.getDocumentation().orElse(null));
    }

    /** The startup check runs from the context loaded event, which only reaches a bean that is a listener. */
    @Test
    public void theStartupCheckListensForTheContextLoadedEvent() {
        DefaultListableBeanFactory beans = load(new DefaultListableBeanFactory());
        assertTrue(ApplicationListener.class.isAssignableFrom(beans.getType("updatesCheckerStartupChecker")));
    }

    @Test
    public void geoServer3PutsThePageInTheGeoServerMenu() {
        DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
        Category first = new Category();
        beans.registerSingleton("geoserverCategory", first);
        // 3.x makes the old names aliases of its Server menu
        Category server = new Category();
        beans.registerSingleton("serverCategory", server);
        beans.registerAlias("serverCategory", "aboutStatusCategory");
        beans.registerAlias("serverCategory", "settingsCategory");

        assertSame(first, page(beans).getCategory());
    }

    @Test
    public void geoServer2PutsThePageInTheAboutAndStatusMenu() {
        DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
        Category first = new Category();
        beans.registerSingleton("aboutStatusCategory", first);
        beans.registerAlias("aboutStatusCategory", "serverCategory");
        beans.registerSingleton("settingsCategory", new Category());

        assertSame(first, page(beans).getCategory());
    }
}
