/* (c) 2015-2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.layer;

import java.io.Serial;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geowebcache.filter.parameters.CaseNormalizer;
import org.geowebcache.filter.parameters.CaseNormalizer.Case;

/**
 * Component for configuring a CaseNormalizer for a ParameterFilter
 *
 * @author Kevin Smith, Boundless
 */
public class CaseNormalizerSubform extends FormComponentPanel<CaseNormalizer> {
    /** serialVersionUID */
    @Serial
    private static final long serialVersionUID = -197485768903404047L;

    private DropDownChoice<Locale> localeEntry;
    private DropDownChoice<Case> caseEntry;

    private List<Locale> getLocales() {
        Locale[] locales = Locale.getAvailableLocales();
        Arrays.sort(locales, (o1, o2) -> o1.toString().compareTo(o2.toString()));
        return Arrays.asList(locales);
    }

    public CaseNormalizerSubform(final String id, final IModel<CaseNormalizer> model) {
        super(id, model);

        final IModel<Case> caseModel = new PropertyModel<>(model, "case");
        final IModel<Locale> localeModel = new PropertyModel<>(model, "configuredLocale");

        localeEntry = new DropDownChoice<>("locale", localeModel, getLocales(), new ChoiceRenderer<>() {

            @Serial
            private static final long serialVersionUID = -2122570049478633429L;

            @Override
            public Object getDisplayValue(Locale object) {
                return object.getDisplayName(CaseNormalizerSubform.this.getLocale());
            }

            @Override
            public String getIdValue(Locale object, int index) {
                return object.toString();
            }
        });
        localeEntry.setNullValid(true);

        caseEntry = new DropDownChoice<>("case", caseModel, Arrays.asList(Case.values()), new ChoiceRenderer<>() {

            @Serial
            private static final long serialVersionUID = -129788130907421097L;

            @Override
            public Object getDisplayValue(Case object) {
                return getLocalizer().getString("case." + object.name(), CaseNormalizerSubform.this);
            }

            @Override
            public String getIdValue(Case object, int index) {
                return object.name();
            }
        });

        add(caseEntry);
        add(localeEntry);
    }

    @Override
    public void convertInput() {
        visitChildren((component, visit) -> {
            if (component instanceof FormComponent<?> formComponent) {
                formComponent.processInput();
            }
        });
        CaseNormalizer filter = getModelObject();
        setConvertedInput(filter);
    }
}
