package org.geoserver.web.security;

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;

public class CheckBoxesPanel extends Panel {

    List<CheckBox> checkBoxes = new ArrayList<>();

    boolean selectAllValue;

    public CheckBoxesPanel(String id, WebMarkupContainer listContainer) {
        super(id);
        CheckBox sa = new CheckBox("selectAll", new PropertyModel<Boolean>(this, "selectAllValue"));
        sa.setOutputMarkupId(true);
        sa.add(
                new AjaxFormComponentUpdatingBehavior("click") {

                    private static final long serialVersionUID = 1154921156065269691L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        // select all the checkboxes
                        setSelection(selectAllValue);

                        // update table and the checkbox itself
                        target.add(getComponent());
                        target.add(listContainer);

                        // allow subclasses to play on this change as well
                        // onSelectionUpdate(target);
                    }
                });
    }

    public CheckBoxesPanel(String id) {
        super(id);
    }

    void setSelection(boolean selected) {
        checkBoxes.forEach(c -> c.getModel().setObject(selected));
        selectAllValue = selected;
    }

    boolean checkAllSelected() {
        for (CheckBox c : checkBoxes) {
            if (c.getModelObject().booleanValue() == true) continue;
            else return false;
        }
        return true;
    }
}
