/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.inspire.web;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.apache.wicket.model.IModel;
import org.geoserver.inspire.InspireSchema;

/**
 * Model for the list of INSPIRE supported languages.
 *
 * <p>The three-letter ISO language codes are loaded from the {@code
 * org/geoserver/inspire/wms/available_languages.properties} properties file.
 */
public class AllLanguagesModel implements IModel<List<String>> {
    private static final String LANGUAGES_FILE =
            "/org/geoserver/inspire/available_languages.properties";

    private static final long serialVersionUID = -6324842325783657135L;

    private List<String> langs;

    /** @see org.apache.wicket.model.IModel#setObject(java.lang.Object) */
    @Override
    public void setObject(List<String> object) {
        this.langs = object;
    }

    /** @see org.apache.wicket.model.IModel#getObject() */
    @Override
    public List<String> getObject() {
        if (langs == null) {
            try {
                langs = getAvailableLanguages();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return langs;
    }

    /** @see org.apache.wicket.model.IDetachable#detach() */
    @Override
    public void detach() {
        langs = null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    List<String> getAvailableLanguages() throws IOException {
        List<String> langs = new ArrayList<String>();
        URL resource = InspireSchema.class.getResource(LANGUAGES_FILE);
        InputStream inStream = resource.openStream();
        try {
            Properties list = new Properties();
            list.load(inStream);
            Set codes = list.keySet();
            langs.addAll(codes);
        } finally {
            inStream.close();
        }
        Collections.sort(langs);
        return langs;
    }
}
