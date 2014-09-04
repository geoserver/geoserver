/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
/**
 * 
 */
package org.geoserver.web.data.store;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.geotools.data.DataAccessFactory.Param;
import org.geotools.data.Repository;

/**
 * A serializable view of a {@link Param}
 * 
 * @author Gabriel Roldan
 */
public class ParamInfo implements Serializable {

    private static final long serialVersionUID = 886996604911751174L;

    private final String name;

    private final String title;

    private boolean password;
    
    private boolean largeText;

    private Class<?> binding;

    private boolean required;

    private Serializable value;
    
    private List<Serializable> options;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ParamInfo(Param param) {
        this.name = param.key;
        this.title = param.title == null ? null : param.title.toString();
        this.password = param.isPassword();
        this.largeText = param.metadata != null && Boolean.TRUE.equals(param.metadata.get(Param.IS_LARGE_TEXT));
        if (Serializable.class.isAssignableFrom(param.type)) {
            this.binding = param.type;
            this.value = (Serializable) param.sample;
        } else if (Repository.class.equals(param.type)) {
                this.binding = param.type;
                this.value = null;
        } else {
            // handle the parameter as a string and let the DataStoreFactory
            // convert it to the appropriate type
            this.binding = String.class;
            this.value = param.sample == null ? null : String.valueOf(param.sample);
        }
        this.required = param.required;
        if (param.metadata != null) {
            List<Serializable> options = (List<Serializable>) param.metadata.get(Param.OPTIONS);
            if (options != null && options.size() > 0) {
                this.options = new ArrayList<Serializable>(options);
                if (Comparable.class.isAssignableFrom(this.binding)) {
                    Collections.sort((List) options);
                }
                if (this.value == null) {
                    this.value = options.get(0);
                }
            }
        }
    }
    
    public List<Serializable> getOptions(){
        return options;
    }
    
    public Serializable getValue() {
        return value;
    }

    public void setValue(Serializable value) {
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public boolean isPassword() {
        return password;
    }
    
    public boolean isLargeText() {
        return largeText;
    }

    public Class<?> getBinding() {
        return binding;
    }

    public boolean isRequired() {
        return required;
    }
}
