/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.LocalizedChoiceRenderer;
import org.geowebcache.locks.LockProvider;
import org.springframework.context.ApplicationContext;

/**
 * General options for caching.
 * 
 * @author Gabriel Roldan, Boundless
 * @author Kevin Smith, Boundless
 *
 */
public class CachingOptionsPanel extends Panel {
    private static final long serialVersionUID = 1L;

    public CachingOptionsPanel(final String id, final IModel<GWCConfig> gwcConfigModel) {

        super(id, gwcConfigModel);

        final WebMarkupContainer container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);

        final WebMarkupContainer configs = new WebMarkupContainer("configs");
        configs.setOutputMarkupId(true);
        container.add(configs);

        IModel<String> lockProviderModel = new PropertyModel<String>(gwcConfigModel, "lockProviderName");
        ApplicationContext applicationContext = GeoServerApplication.get().getApplicationContext();
        String[] lockProviders = applicationContext.getBeanNamesForType(LockProvider.class);
        List<String> lockProviderChoices = new ArrayList<String>(Arrays.asList(lockProviders));
        Collections.sort(lockProviderChoices); // make sure we get a stable listing order
        DropDownChoice<String> lockProviderChoice = new DropDownChoice<String>("lockProvider", lockProviderModel,
                lockProviderChoices, new LocalizedChoiceRenderer(this));
        configs.add(lockProviderChoice);
    }
}
