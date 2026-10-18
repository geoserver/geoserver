/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

import static org.junit.Assert.assertSame;

import org.geoserver.web.Category;
import org.geoserver.web.MenuPageInfo;
import org.junit.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.core.io.ClassPathResource;

/**
 * The page goes in the first menu, which is named differently in GeoServer 2 and 3. Only the page bean is created, so
 * each series is simulated with the category beans it defines.
 */
public class UpdatesCheckerMenuTest {

    private static MenuPageInfo page(DefaultListableBeanFactory beans) {
        beans.setBeanExpressionResolver(new StandardBeanExpressionResolver());
        new XmlBeanDefinitionReader(beans).loadBeanDefinitions(new ClassPathResource("applicationContext.xml"));
        return beans.getBean("updatesCheckerPage", MenuPageInfo.class);
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
