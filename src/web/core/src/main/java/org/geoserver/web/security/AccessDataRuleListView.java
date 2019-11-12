/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security;

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

public class AccessDataRuleListView extends ListView<DataAccessRuleInfo> {

    private List<CheckBox> checkBoxes;
    private Boolean selectAll;
    private boolean isWs;

    public AccessDataRuleListView(String id, IModel<List<DataAccessRuleInfo>> model, boolean isWs) {
        super(id, model);
        this.isWs = isWs;
        if (checkBoxes == null) {
            int checkBoxesTypes = isWs ? 3 : 2;
            checkBoxes = new ArrayList<CheckBox>(model.getObject().size() * checkBoxesTypes);
        }
    }

    @Override
    protected void populateItem(ListItem<DataAccessRuleInfo> item) {
        DataAccessRuleInfo ruleModel = item.getModelObject();
        item.add(new Label("roleName", new PropertyModel<String>(ruleModel, "roleName")));
        CheckBox readCheckBox = new CheckBox("read", new PropertyModel<>(ruleModel, "read"));
        readCheckBox.setOutputMarkupId(true);
        item.add(readCheckBox);
        CheckBox writeCheckBox = new CheckBox("write", new PropertyModel<>(ruleModel, "write"));
        item.add(writeCheckBox);
        checkBoxes.add(readCheckBox);
        checkBoxes.add(writeCheckBox);
        CheckBox adminCheckBox = new CheckBox("admin", new PropertyModel<>(ruleModel, "admin"));
        adminCheckBox.setOutputMarkupId(true);
        if (!isWs) {
            adminCheckBox.setVisible(false);
        }
        item.add(adminCheckBox);

        checkBoxes.add(adminCheckBox);
        addNewCheckboxesUpdateBeahaviour();
        this.selectAll = checkAllSelected();
    }

    public List<CheckBox> getCheckBoxes() {
        return checkBoxes;
    }

    public void setCheckBoxes(List<CheckBox> checkBoxes) {
        this.checkBoxes = checkBoxes;
    }

    boolean checkAllSelected() {
        for (CheckBox c : this.checkBoxes) {
            if (c.getModelObject().booleanValue() == true) continue;
            else return false;
        }
        return true;
    }

    public Boolean getSelectAll() {
        return selectAll;
    }

    public void setSelectAll(Boolean selectAll) {
        this.selectAll = selectAll;
    }

    public void setSelection() {
        getCheckBoxes().forEach(c -> c.getModel().setObject(this.selectAll));
        setSelectAll(this.selectAll);
    }

    private void addNewCheckboxesUpdateBeahaviour() {
        for (CheckBox c : checkBoxes) {
            c.add(
                    new AjaxFormComponentUpdatingBehavior("click") {

                        private static final long serialVersionUID = 1154921156065269691L;

                        @Override
                        protected void onUpdate(AjaxRequestTarget target) {
                            selectAll = checkAllSelected();
                            target.add(c.getForm());
                        }
                    });
        }
    }

    public boolean isWs() {
        return isWs;
    }

    public void setWs(boolean ws) {
        isWs = ws;
    }
}
