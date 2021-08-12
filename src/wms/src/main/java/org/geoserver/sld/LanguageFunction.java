/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sld;

import static org.geotools.filter.capability.FunctionNameImpl.parameter;

import java.util.Locale;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.ows.util.RequestUtils;
import org.geoserver.util.GeoServerDefaultLocale;
import org.geotools.filter.FunctionExpressionImpl;
import org.geotools.filter.capability.FunctionNameImpl;
import org.opengis.filter.capability.FunctionName;

/**
 * This function return the language found in the request query param Language attribute or the
 * {@link GeoServerDefaultLocale} otherwise.
 */
public class LanguageFunction extends FunctionExpressionImpl {

    private static final String LANGUAGE = "LANGUAGE";

    public static FunctionName NAME =
            new FunctionNameImpl("language", parameter("result", String.class));

    public LanguageFunction() {
        super(NAME);
    }

    @Override
    public Object evaluate(Object object) {
        Request request = Dispatcher.REQUEST.get();
        String[] langAr = RequestUtils.getLanguageValue(request, LANGUAGE);
        String result = langAr != null ? langAr[0] : null;
        if (result == null || result.equals("*")) {
            Locale locale = GeoServerDefaultLocale.get();
            if (locale != null) result = locale.getLanguage();
            else result = "";
        }
        return result;
    }
}
