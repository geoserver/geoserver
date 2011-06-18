package org.geoserver.web.security;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.security.impl.DataAccessRule;

public class ConfirmRemovalDataAccessRulePanel extends Panel {

    List<DataAccessRule> roots;
    
    public ConfirmRemovalDataAccessRulePanel(String id, DataAccessRule... roots) {
        this(id, Arrays.asList(roots));
    }

    public ConfirmRemovalDataAccessRulePanel(String id, List<DataAccessRule> roots) {
        super(id);
        this.roots = roots;

        // add roots
        WebMarkupContainer root = new WebMarkupContainer("rootObjects");
        root.add(new Label("rootObjectNames", names(roots)));
        root.setVisible(!roots.isEmpty());
        add(root);

        // removed objects root (we show it if any removed object is on the list)
        WebMarkupContainer removed = new WebMarkupContainer("removedObjects");

        add(removed);

        // removed workspaces
        WebMarkupContainer rulesRemoved = new WebMarkupContainer("rulesRemoved");
        removed.add(rulesRemoved);
        List<DataAccessRule> rules = roots;
        if (rules.size() == 0)
            rulesRemoved.setVisible(false);
        rulesRemoved.add(new Label("rules", names(rules)));
    }

    String names(List objects) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < objects.size(); i++) {
            sb.append(name(objects.get(i)));
            if (i < (objects.size() - 1))
                sb.append(", ");
        }
        return sb.toString();
    }

    String name(Object object) {
        try {
            return (String) BeanUtils.getProperty(object, "workspace") + "."
                    + (String) BeanUtils.getProperty(object, "layer") + "."
                    + (String) BeanUtils.getProperty(object, "accessMode") + "="
                    + (String) BeanUtils.getProperty(object, "roles");
        } catch (Exception e) {
            throw new RuntimeException("A data object that does not have "
                    + "a 'name' property has been used, this is unexpected", e);
        }
    }

    protected StringResourceModel canRemove(DataAccessRule data) {
        return null;
    }

}
