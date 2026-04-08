/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.web.components;

import java.io.Serializable;
import lombok.Getter;
import lombok.NonNull;
import org.apache.wicket.model.CompoundPropertyModel;

@SuppressWarnings("serial")
public abstract class AbstractRuleEditModel<R extends Serializable> extends AbstractRulesModel {

    private @Getter CompoundPropertyModel<R> model;

    public AbstractRuleEditModel(@NonNull R rule) {
        model = new CompoundPropertyModel<>(rule);
    }

    public abstract void save();

    public @Override String getSelectedRoleName() {
        return getRoleName(getModel().getObject());
    }

    /** @see #getUserChoices(String) */
    protected abstract String getRoleName(R rule);
}
