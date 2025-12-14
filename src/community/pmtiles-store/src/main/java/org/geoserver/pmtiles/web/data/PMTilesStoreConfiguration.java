/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.pmtiles.web.data;

import java.util.List;
import org.apache.wicket.markup.html.WebPage;
import org.geoserver.web.HeaderContribution;
import org.geoserver.web.data.resource.DataStorePanelInfo;
import org.geoserver.web.data.store.DataAccessEditPage;
import org.geoserver.web.data.store.DataAccessNewPage;
import org.geoserver.web.data.store.StoreEditPanel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class PMTilesStoreConfiguration {

    @Bean
    @SuppressWarnings("unchecked")
    DataStorePanelInfo pmtilesDataStorePanel() {
        DataStorePanelInfo panelInfo = new DataStorePanelInfo();
        panelInfo.setId("pmtilesDataStorePanel");
        panelInfo.setFactoryClass(org.geotools.pmtiles.store.PMTilesDataStoreFactory.class);
        Class<?> componentClass = org.geoserver.pmtiles.web.data.PMTilesDataStoreEditPanel.class;
        panelInfo.setComponentClass((Class<StoreEditPanel>) componentClass);
        panelInfo.setIconBase(org.geoserver.pmtiles.web.data.PMTilesDataStoreEditPanel.class);
        panelInfo.setIcon("img/protomaps_icon.svg");
        return panelInfo;
    }

    /** Contributes {@code RadioGroupParamPanel.css} to {@link PMTilesDataStoreEditPanel} */
    @Bean
    HeaderContribution aclSwitchFieldCssContribution() {
        return new CssContribution("RadioGroupParamPanel.css", DataAccessEditPage.class, DataAccessNewPage.class);
    }

    static class CssContribution extends HeaderContribution {

        private List<Class<? extends WebPage>> appliesTo;

        @SafeVarargs
        CssContribution(String cssFile, Class<? extends WebPage>... pages) {
            this.appliesTo = List.of(pages);
            setCSSFilename(cssFile);
            setScope(PMTilesDataStoreEditPanel.class);
        }

        public @Override boolean appliesTo(WebPage page) {
            return appliesTo.stream().anyMatch(c -> c.isInstance(page));
        }
    }
}
