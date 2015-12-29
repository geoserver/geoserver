/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.layer;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geowebcache.filter.parameters.CaseNormalizer;
import org.geowebcache.filter.parameters.CaseNormalizer.Case;

/**
 * Component for configuring a CaseNormalizer for a ParameterFilter
 * 
 * @author Kevin Smith, Boundless
 *
 */
public class CaseNormalizerSubform extends FormComponentPanel<CaseNormalizer> {
    /** serialVersionUID */
    private static final long serialVersionUID = -197485768903404047L;
    private DropDownChoice<Locale> localeEntry;
    private DropDownChoice<Case> caseEntry;
    
    private List<Locale> getLocales() {
        Locale[] locales = Locale.getAvailableLocales();
        Arrays.sort(locales, new Comparator<Locale>(){
            
            @Override
            public int compare(Locale o1, Locale o2) {
                return o1.toString().compareTo(o2.toString());
            }
            
        });
        return Arrays.asList(locales);
    }
    
    public CaseNormalizerSubform(final String id, final IModel<CaseNormalizer> model) {
        super(id, model);
        
        final IModel<Case> caseModel = new PropertyModel<Case>(model, "case");
        final IModel<Locale> localeModel = new PropertyModel<Locale>(model, "configuredLocale");
        
        localeEntry = new DropDownChoice<>("locale", localeModel, getLocales(), new IChoiceRenderer<Locale>(){
            
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
        
        caseEntry = new DropDownChoice<Case>("case", caseModel, Arrays.asList(Case.values()), new IChoiceRenderer<Case>(){
            
            /** serialVersionUID */
            private static final long serialVersionUID = 1L;
            
            @Override
            public Object getDisplayValue(Case object) {
                return getLocalizer().getString("case."+object.name(), CaseNormalizerSubform.this);
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
        visitChildren(new Component.IVisitor<Component>() {
            
            @Override
            public Object component(Component component) {
                if (component instanceof FormComponent) {
                    FormComponent<?> formComponent = (FormComponent<?>) component;
                    formComponent.processInput();
                }
                return Component.IVisitor.CONTINUE_TRAVERSAL;
            }
        });
        CaseNormalizer filter = getModelObject();
        setConvertedInput(filter);
    }
}
