/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.cog.panel;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.cog.CogSettings;

/** Basic Panel to configure CogSettings. */
@SuppressWarnings("unchecked")
public class CogSettingsPanel<T extends CogSettings> extends FormComponentPanel<T> {

    /** Note that caching is temporarily disabled from the UI */
    protected final CheckBox useCachingStream;

    protected DropDownChoice<CogSettings.RangeReaderType> rangeReaderSettings;

    protected final WebMarkupContainer container;

    public CogSettingsPanel(String id, IModel<T> model) {
        super(id, model);

        container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);
        useCachingStream = new CheckBox("useCachingStream", new PropertyModel<>(model, "useCachingStream"));
        useCachingStream.setVisible(false);
        container.add(useCachingStream);

        List<CogSettings.RangeReaderType> rangeReaderTypes =
                new ArrayList<>(Arrays.asList(CogSettings.RangeReaderType.values()));

        // create the editor, eventually set a default value
        rangeReaderSettings = new DropDownChoice<>(
                "rangeReaderSettings", new PropertyModel<>(model, "rangeReaderSettings"), rangeReaderTypes);

        rangeReaderSettings.setOutputMarkupId(true);
        container.add(rangeReaderSettings);

        useCachingStream.add(new OnChangeAjaxBehavior() {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                boolean useCache = useCachingStream.getModelObject();
                CogSettings object = getSettings(model);
                object.setUseCachingStream(useCache);
                model.setObject((T) object);
            }
        });

        rangeReaderSettings.add(new OnChangeAjaxBehavior() {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                CogSettings.RangeReaderType rangeReader = rangeReaderSettings.getModelObject();
                CogSettings object = getSettings(model);
                object.setRangeReaderSettings(rangeReader);
                model.setObject((T) object);
            }
        });
    }

    private CogSettings getSettings(IModel<T> model) {
        CogSettings settings = model.getObject();
        if (settings == null) {
            model.setObject((T) new CogSettings());
        }
        return settings;
    }

    @Override
    public void convertInput() {
        CogSettings convertedInput = new CogSettings();
        convertedInput.setUseCachingStream(useCachingStream.getModelObject());
        convertedInput.setRangeReaderSettings(rangeReaderSettings.getModelObject());
        setConvertedInput((T) convertedInput);
    }
}
