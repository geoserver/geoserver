/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.wicket.model.ChainingModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geotools.util.GrowableInternationalString;

/**
 * A model for a {@link GrowableInternationalString} Internally it handles the mapping beetween the
 * entries of the growable internal Map and instances of a {@InternationalStringEntry} suitable to
 * be used as Model object.
 */
public class GrowableStringModel extends ChainingModel<GrowableInternationalString> {

    PropertyModel<Map<Locale, String>> growableMapModel;

    public GrowableStringModel(IModel<GrowableInternationalString> model) {
        super(model);
        this.growableMapModel = new PropertyModel<>(model, "localMap");
    }

    /**
     * Returns the list of entries currently available as a list of {InternationalStringEntry}
     * instances.
     *
     * @return the list of available entries.
     */
    public List<InternationalStringEntry> getEntries() {
        Map<Locale, String> map = growableMapModel.getObject();
        List<InternationalStringEntry> result = new ArrayList<>(map.size());
        for (Locale locale : map.keySet()) {
            String text = map.get(locale);
            result.add(new InternationalStringEntry(locale, text));
        }
        return result;
    }

    @Override
    public void setObject(GrowableInternationalString object) {
        this.growableMapModel = new PropertyModel<>(object, "localMap");
        super.setObject(object);
    }

    /**
     * This class represent an entry of a GrowableInternationalString. It is meant to be used as the
     * model object of a table/list entries
     */
    public static final class InternationalStringEntry implements Serializable {
        private Locale locale;

        private String text;

        public InternationalStringEntry() {}

        public InternationalStringEntry(Locale locale, String text) {
            this.locale = locale;
            this.text = text;
        }

        public Locale getLocale() {
            return locale;
        }

        public void setLocale(Locale locale) {

            this.locale = locale;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
}
