/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.io.Serial;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.validator.RangeValidator;
import org.geoserver.libdeflate.LibdeflateSettings;
import org.geoserver.libdeflate.LibdeflateSettingsInitializer;

/** Basic Panel to configure LibdeflateSettings. */
@SuppressWarnings("unchecked")
public class LibdeflateSettingsPanel<T extends LibdeflateSettings> extends FormComponentPanel<T> {

    private static final boolean isCssEmpty = org.geoserver.web.util.WebUtils.IsWicketCssFileEmpty(
            java.lang.invoke.MethodHandles.lookup().lookupClass());

    @Override
    public void renderHead(org.apache.wicket.markup.head.IHeaderResponse response) {
        super.renderHead(response);
        // if the panel-specific CSS file contains actual css then have the browser load the css
        if (!isCssEmpty) {
            response.render(org.apache.wicket.markup.head.CssHeaderItem.forReference(
                    new org.apache.wicket.request.resource.PackageResourceReference(
                            getClass(), getClass().getSimpleName() + ".css")));
        }
    }

    protected final WebMarkupContainer container;
    private final TextField<Integer> compressionPriority;
    private final TextField<Integer> decompressionPriority;
    private final TextField<Integer> minLevel;
    private final TextField<Integer> maxLevel;

    public LibdeflateSettingsPanel(String id, IModel<T> model) {
        super(id, model);

        container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);

        compressionPriority = new TextField<>("compressionPriority", new PropertyModel<>(model, "compressionPriority"));
        compressionPriority.add(new RangeValidator<>(0, 100));
        container.add(compressionPriority);

        decompressionPriority =
                new TextField<>("decompressionPriority", new PropertyModel<>(model, "decompressionPriority"));
        decompressionPriority.add(new RangeValidator<>(0, 100));
        container.add(decompressionPriority);

        minLevel = new TextField<>("minLevel", new PropertyModel<>(model, "minLevel"));
        minLevel.add(new RangeValidator<>(-10, 12));
        container.add(minLevel);

        maxLevel = new TextField<>("maxLevel", new PropertyModel<>(model, "maxLevel"));
        maxLevel.add(new RangeValidator<>(0, 12));
        container.add(maxLevel);

        compressionPriority.add(new OnChangeAjaxBehavior() {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                int priority = compressionPriority.getModelObject();
                LibdeflateSettings object = getSettings(model);
                object.setCompressionPriority(priority);
                model.setObject((T) object);
            }
        });

        decompressionPriority.add(new OnChangeAjaxBehavior() {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                int priority = decompressionPriority.getModelObject();
                LibdeflateSettings object = getSettings(model);
                object.setDecompressionPriority(priority);
                model.setObject((T) object);
            }
        });
        minLevel.add(new OnChangeAjaxBehavior() {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                int min = minLevel.getModelObject();
                LibdeflateSettings object = getSettings(model);
                object.setMinLevel(min);
                model.setObject((T) object);
            }
        });
        maxLevel.add(new OnChangeAjaxBehavior() {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                int max = maxLevel.getModelObject();
                LibdeflateSettings object = getSettings(model);
                object.setMaxLevel(max);
                model.setObject((T) object);
            }
        });
    }

    private LibdeflateSettings getSettings(IModel<T> model) {
        LibdeflateSettings settings = model.getObject();
        if (settings == null) {
            model.setObject((T) new LibdeflateSettings());
        }
        return settings;
    }

    @Override
    public void convertInput() {
        LibdeflateSettings convertedInput = new LibdeflateSettings();
        convertedInput.setCompressionPriority(compressionPriority.getModelObject());
        convertedInput.setDecompressionPriority(decompressionPriority.getModelObject());
        convertedInput.setMaxLevel(maxLevel.getModelObject());
        convertedInput.setMinLevel(minLevel.getModelObject());

        setConvertedInput((T) convertedInput);
        LibdeflateSettingsInitializer.initSettings(convertedInput);
    }
}
