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
    private boolean ws;
    private Boolean selectAll;

    public AccessDataRuleListView(String id, IModel<List<DataAccessRuleInfo>> model, boolean ws) {
        super(id, model);
        this.ws = ws;
        if (checkBoxes == null)
            checkBoxes = new ArrayList<CheckBox>(model.getObject().size() * (ws ? 1 : 2));
    }

    @Override
    protected void populateItem(ListItem<DataAccessRuleInfo> item) {
        DataAccessRuleInfo ruleModel = item.getModelObject();
        item.add(new Label("roleName", new PropertyModel<String>(ruleModel, "roleName")));
        if (!ws) {
            CheckBox readCheckBox = new CheckBox("read", new PropertyModel<>(ruleModel, "read"));
            readCheckBox.setOutputMarkupId(true);
            item.add(readCheckBox);
            CheckBox writeCheckBox = new CheckBox("write", new PropertyModel<>(ruleModel, "write"));
            item.add(writeCheckBox);
            checkBoxes.add(readCheckBox);
            checkBoxes.add(writeCheckBox);
        } else {
            CheckBox adminCheckBox = new CheckBox("admin", new PropertyModel<>(ruleModel, "admin"));
            adminCheckBox.setOutputMarkupId(true);
            item.add(adminCheckBox);
            checkBoxes.add(adminCheckBox);
        }
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

    public boolean isWs() {
        return ws;
    }

    public void setWs(boolean ws) {
        this.ws = ws;
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
}
