/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.web.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.wicket.model.IModel;
import org.geoserver.taskmanager.data.Attribute;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.data.Parameter;
import org.geoserver.taskmanager.data.Task;
import org.geoserver.taskmanager.util.InitConfigUtil;
import org.geoserver.taskmanager.util.TaskManagerBeans;
import org.geoserver.web.wicket.GeoServerDataProvider;

public class AttributesModel extends GeoServerDataProvider<Attribute> {

    private static final long serialVersionUID = -8846370782957169591L;

    public static final Property<Attribute> NAME = new BeanProperty<Attribute>("name", "name");

    public static final Property<Attribute> VALUE = new BeanProperty<Attribute>("value", "value");

    public static final Property<Attribute> ACTIONS =
            new AbstractProperty<Attribute>("actions") {

                private static final long serialVersionUID = -978472501994535469L;

                @Override
                public Object getPropertyValue(Attribute item) {
                    return null;
                }
            };

    private IModel<Configuration> configurationModel;

    private Map<String, Attribute> attributes = new HashMap<String, Attribute>();

    public AttributesModel(IModel<Configuration> configurationModel) {
        this.configurationModel = configurationModel;
    }

    @Override
    protected List<Property<Attribute>> getProperties() {
        return Arrays.asList(NAME, VALUE, ACTIONS);
    }

    @Override
    public List<Attribute> getItems() {
        attributes.putAll(configurationModel.getObject().getAttributes());

        Set<String> taskAttNames = new LinkedHashSet<String>();
        for (Task task : configurationModel.getObject().getTasks().values()) {
            for (Parameter pam : task.getParameters().values()) {
                String attName =
                        TaskManagerBeans.get().getDataUtil().getAssociatedAttributeName(pam);
                if (attName != null) {
                    taskAttNames.add(attName);
                }
            }
        }

        Set<String> configAttNames = new LinkedHashSet<String>(attributes.keySet());

        List<Attribute> attList = new ArrayList<Attribute>();
        for (String attName : taskAttNames) {
            Attribute att = attributes.get(attName);
            if (att == null) {
                att = TaskManagerBeans.get().getFac().createAttribute();
                att.setConfiguration(InitConfigUtil.unwrap(configurationModel.getObject()));
                att.setName(attName);
                attributes.put(attName, att);
            }
            attList.add(att);
            configAttNames.remove(attName);
        }
        for (String attName : configAttNames) {
            Attribute att = attributes.get(attName);
            if (att.getValue() != null && !"".equals(att.getValue())) {
                attList.add(att);
            }
        }
        return attList;
    }

    public void save(boolean removeEmpty) {
        getItems();
        for (Attribute att : attributes.values()) {
            if (!removeEmpty || att.getValue() != null && !"".equals(att.getValue())) {
                configurationModel.getObject().getAttributes().put(att.getName(), att);
            } else {
                configurationModel.getObject().getAttributes().remove(att.getName());
            }
        }
    }
}
