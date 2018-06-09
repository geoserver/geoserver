/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.passwd;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.IConverter;
import org.geoserver.security.password.URLMasterPasswordProviderConfig;
import org.geoserver.web.wicket.HelpLink;

@SuppressWarnings("serial")
public class URLMasterPasswordProviderPanel
        extends MasterPasswordProviderPanel<URLMasterPasswordProviderConfig> {

    public URLMasterPasswordProviderPanel(
            String id, IModel<URLMasterPasswordProviderConfig> model) {
        super(id, model);

        add(new HelpLink("urlHelp", this).setDialog(dialog));
        add(
                new TextField<URL>("uRL", URL.class) {
                    @Override
                    public <C> IConverter<C> getConverter(Class<C> type) {
                        return new IConverter() {
                            @Override
                            public String convertToString(Object value, Locale locale) {
                                return ((URL) value).toExternalForm();
                            }

                            @Override
                            public Object convertToObject(String value, Locale locale) {
                                try {
                                    return new URL(value);
                                } catch (MalformedURLException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        };
                    }
                });
        add(new CheckBox("encrypting"));
    }
}
