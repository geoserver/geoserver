/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.publish.dggs;

import static org.geotools.dggs.gstore.DGGSResolutionCalculator.CONFIGURED_MAXRES_KEY;
import static org.geotools.dggs.gstore.DGGSResolutionCalculator.CONFIGURED_MINRES_KEY;
import static org.geotools.dggs.gstore.DGGSResolutionCalculator.CONFIGURED_OFFSET_KEY;

import java.io.IOException;
import java.util.Arrays;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.ValidationError;
import org.apache.wicket.validation.validator.RangeValidator;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.web.publish.PublishedConfigurationPanel;
import org.geoserver.web.util.MapModel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.api.data.FeatureSource;
import org.geotools.dggs.DGGSInstance;
import org.geotools.dggs.gstore.DGGSFeatureSource;
import org.geotools.util.decorate.Wrapper;

/** Configures a layer DGGS related attributes */
public class DGGSConfigPanel extends PublishedConfigurationPanel<LayerInfo> {

    private static final long serialVersionUID = 6469105227923320272L;
    private final TextField<Integer> minResolution;
    private final TextField<Integer> maxResolution;

    public DGGSConfigPanel(String id, IModel<LayerInfo> model) throws IOException {
        super(id, model);

        // adding the resolution offset editor
        PropertyModel<MetadataMap> metadata = new PropertyModel<>(model, "resource.metadata");
        add(metadataIntegerEditor("resolutionOffset", metadata, CONFIGURED_OFFSET_KEY));

        // Adding the min and max resolution editors
        @SuppressWarnings("PMD.CloseResource") // owned by the store
        DGGSInstance dggs = getDGGS(model);
        int[] resolutions = dggs.getResolutions();
        int minDggsRes = Arrays.stream(resolutions).min().orElse(0);
        int maxDggsRes = Arrays.stream(resolutions).max().orElse(Integer.MAX_VALUE);

        minResolution = metadataIntegerEditor("minResolution", metadata, CONFIGURED_MINRES_KEY);
        add(minResolution);
        minResolution.add(RangeValidator.minimum(minDggsRes));
        maxResolution = metadataIntegerEditor("maxResolution", metadata, CONFIGURED_MAXRES_KEY);
        add(maxResolution);
        maxResolution.add(RangeValidator.maximum(maxDggsRes));
    }

    /**
     * Returns a {@link TextField} for editing an integer value stored in the metadata map
     *
     * @param fieldName
     * @param metadata
     * @param metadataMapKey
     */
    private static TextField<Integer> metadataIntegerEditor(
            String fieldName, PropertyModel<MetadataMap> metadata, String metadataMapKey) {
        return new TextField<>(fieldName, new MapModel<>(metadata, metadataMapKey), Integer.class);
    }

    @Override
    /** Check the min/max values are consistent, need the panel to included in the hierarchy to find the /form */
    protected void onInitialize() {
        super.onInitialize();

        minResolution.getForm().add(new MinMaxValidator(minResolution, maxResolution));
    }

    private static DGGSInstance getDGGS(IModel<LayerInfo> model) throws IOException {
        FeatureTypeInfo fti = (FeatureTypeInfo) model.getObject().getResource();
        FeatureSource fs = fti.getFeatureSource(null, null);
        if (fs instanceof Wrapper) {
            fs = ((Wrapper) fs).unwrap(DGGSFeatureSource.class);
        }
        return ((DGGSFeatureSource) fs).getDGGS();
    }

    public class MinMaxValidator implements IFormValidator {

        private final TextField<Integer> minField;
        private final TextField<Integer> maxField;

        public MinMaxValidator(TextField<Integer> minField, TextField<Integer> maxField) {
            this.minField = minField;
            this.maxField = maxField;
        }

        @Override
        public FormComponent<?>[] getDependentFormComponents() {
            return new FormComponent<?>[] {minField, maxField};
        }

        @Override
        public void validate(Form<?> form) {
            Integer minValue = minField.getConvertedInput();
            Integer maxValue = maxField.getConvertedInput();

            if (minValue != null && maxValue != null && minValue > maxValue) {
                ValidationError error = new ValidationError();
                error.setMessage(new ParamResourceModel("minMaxError", DGGSConfigPanel.this).getObject());
                minField.error(error);
            }
        }
    }
}
