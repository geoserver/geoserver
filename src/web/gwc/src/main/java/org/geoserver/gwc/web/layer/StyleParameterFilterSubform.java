package org.geoserver.gwc.web.layer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.CheckBoxMultipleChoice;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.gwc.layer.StyleParameterFilter;

/**
 * Subform that displays basic information about a ParameterFilter
 * @author Kevin Smith, OpenGeo
 *
 */
public class StyleParameterFilterSubform extends AbstractParameterFilterSubform<StyleParameterFilter> {

    /**
     * Model Set<String> as a List<String> and optionally add a dummy element at the beginning.
     */
    static class SetAsListModel implements IModel<List<String>> {

        /** serialVersionUID */
        private static final long serialVersionUID = 1L;
        
        final private IModel<Set<String>> realModel;
        
        final private List<String> fakeObject;
        
        final protected String extra;
        
        public SetAsListModel(IModel<Set<String>> realModel, String extra) {
            super();
            this.realModel = realModel;
            this.extra = extra;
            
            Set<String> realObj =  realModel.getObject();
            
            int size;
            if(realObj==null) {
                size = 0;
            } else {
                size = realObj.size();
            }
            if(extra!=null){
                size++;
            }
            fakeObject = new ArrayList<String>(size);
        }

        @Override
        public void detach() {
            realModel.detach();
        }

        @Override
        public List<String> getObject() {
            Set<String> realObj = realModel.getObject();
            
            fakeObject.clear();
            
            if(extra!=null) fakeObject.add(extra);
            if(realObj != null) fakeObject.addAll(realObj);
            
            return fakeObject;
        }

        @Override
        public void setObject(List<String> object) {
            if(object == null){
                realModel.setObject(null);
            } else {
                Set<String> newObj = new HashSet<String>(object);
                newObj.remove(extra);
                realModel.setObject(new HashSet<String>(object));
            }
        }
    }
    
    /**
     * Model Set<String> as a List<String> and add an option to represent the set being 
     * {@literal null}
     */
    static class NullableSetAsListModel implements IModel<List<String>> {

        /** serialVersionUID */
        private static final long serialVersionUID = 1L;
        
        final private IModel<Set<String>> realModel;
        
        final private List<String> fakeObject;
        
        final protected String nullify;
        
        public NullableSetAsListModel(IModel<Set<String>> realModel, String nullify) {
            super();
            this.realModel = realModel;
            this.nullify = nullify;
            
            Set<String> realObj =  realModel.getObject();
            
            int size;
            if(realObj==null) {
                size = 1;
            } else {
                size = realObj.size();
            }
            fakeObject = new ArrayList<String>(size);
        }

        @Override
        public void detach() {
            realModel.detach();
        }

        @Override
        public List<String> getObject() {
            Set<String> realObj = realModel.getObject();
            
            fakeObject.clear();
            
            if(realObj!=null) {
                fakeObject.addAll(realObj);
            } else {
                fakeObject.add(nullify);
            }
            
            return fakeObject;
        }

        @Override
        public void setObject(List<String> object) {
            if(object == null || object.contains(nullify)){
                realModel.setObject(null);
            } else {
                Set<String> newObj = new HashSet<String>(object);
                newObj.remove(nullify);
                realModel.setObject(new HashSet<String>(object));
            }
        }
    }
    


    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    public StyleParameterFilterSubform(String id,
            IModel<StyleParameterFilter> model) {
        super(id, model);
        
        final Component defaultValue;
        
        final String allStyles = getLocalizer().getString("allStyles", this);
        
        final IModel<List<String>> availableStylesModelDefault = 
                new SetAsListModel(new PropertyModel<Set<String>>(model, "layerStyles"), null);
        final IModel<List<String>> availableStylesModelAllowed = 
                new SetAsListModel(new PropertyModel<Set<String>>(model, "layerStyles"), allStyles);
        final IModel<List<String>> selectedStylesModel = 
                new NullableSetAsListModel(new PropertyModel<Set<String>>(model, "styles"), allStyles);
        
        defaultValue = new DropDownChoice<String>("defaultValue", new PropertyModel<String>(model, "defaultValue"), availableStylesModelDefault);
        add(defaultValue);
        
        final CheckBoxMultipleChoice<String> styles = new CheckBoxMultipleChoice<String>("styles", selectedStylesModel, availableStylesModelAllowed);
        styles.setPrefix("<li>");styles.setSuffix("</li>");
        add(styles);
    }

}
