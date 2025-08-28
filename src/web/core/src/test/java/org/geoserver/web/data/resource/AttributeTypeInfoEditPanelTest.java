/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.resource;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.impl.AttributeTypeInfoImpl;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.data.resource.AttributeTypeInfoEditPanel.RESTRICTION_TYPE;
import org.geotools.util.NumberRange;
import org.junit.Test;

public class AttributeTypeInfoEditPanelTest extends GeoServerWicketTestSupport {
    @Test
    public void testRestrictionType() {

        AttributeTypeInfoImpl model = new AttributeTypeInfoImpl();
        AttributeTypeInfoEditPanel dialog = new AttributeTypeInfoEditPanel("dialog", Model.of(model));

        tester.startComponentInPage(dialog);

        String attributeForm = "dialog:attributeForm";
        FormTester form = tester.newFormTester(attributeForm);

        // assert restrictions not visible
        tester.assertInvisible(attributeForm + ":restrictionsContainer");

        // set attribute type to Integer
        form.setValue("type", "java.lang.Integer");
        tester.executeAjaxEvent(attributeForm + ":type", "change");

        // assert restrictions visible
        tester.assertVisible(attributeForm + ":restrictionsContainer");

        DropDownChoice<RESTRICTION_TYPE> restrictionTypeDropDown = (DropDownChoice)
                tester.getComponentFromLastRenderedPage(attributeForm + ":restrictionsContainer:restrictionType");

        // assert restrictions empty status
        tester.assertModelValue(restrictionTypeDropDown.getPageRelativePath(), RESTRICTION_TYPE.NONE);
        tester.assertInvisible(attributeForm + ":restrictionsContainer:optionsContainer");
        tester.assertInvisible(attributeForm + ":restrictionsContainer:rangeContainer");

        // assert restriction types for attribute numeric binding
        assertEquals(
                List.of(RESTRICTION_TYPE.NONE, RESTRICTION_TYPE.OPTIONS, RESTRICTION_TYPE.RANGE),
                restrictionTypeDropDown.getChoices());

        // set attribute type to String
        form.setValue("type", "java.lang.String");
        tester.executeAjaxEvent(attributeForm + ":type", "change");

        // assert restriction types for attribute string binding
        assertEquals(List.of(RESTRICTION_TYPE.NONE, RESTRICTION_TYPE.OPTIONS), restrictionTypeDropDown.getChoices());
    }

    @Test
    public void testRestrictionTypeOptions() {

        AttributeTypeInfoImpl model = new AttributeTypeInfoImpl();
        AttributeTypeInfoEditPanel dialog = new AttributeTypeInfoEditPanel("dialog", Model.of(model));

        tester.startComponentInPage(dialog);

        String attributeForm = "dialog:attributeForm";
        FormTester form = tester.newFormTester(attributeForm);

        // assert restrictions not visible
        tester.assertInvisible(attributeForm + ":restrictionsContainer");

        // set attribute type to String
        form.setValue("type", "java.lang.String");
        tester.executeAjaxEvent(attributeForm + ":type", "change");

        // assert restrictions visible
        tester.assertVisible(attributeForm + ":restrictionsContainer");

        DropDownChoice<RESTRICTION_TYPE> restrictionTypeDropDown = (DropDownChoice)
                tester.getComponentFromLastRenderedPage(attributeForm + ":restrictionsContainer:restrictionType");

        // assert restrictions empty status
        tester.assertModelValue(restrictionTypeDropDown.getPageRelativePath(), RESTRICTION_TYPE.NONE);
        tester.assertInvisible(attributeForm + ":restrictionsContainer:optionsContainer");
        tester.assertInvisible(attributeForm + ":restrictionsContainer:rangeContainer");

        // set options restriction type
        form.setValue("restrictionsContainer:restrictionType", RESTRICTION_TYPE.OPTIONS.name());
        tester.executeAjaxEvent(attributeForm + ":restrictionsContainer:restrictionType", "change");

        String optionsContainer = attributeForm + ":restrictionsContainer:optionsContainer";

        // assert options restriction components visibility
        tester.assertVisible(optionsContainer);
        tester.assertVisible(optionsContainer + ":options");
        tester.assertVisible(optionsContainer + ":removeOption");
        tester.assertVisible(optionsContainer + ":newOption");
        tester.assertVisible(optionsContainer + ":addOption");
        tester.assertInvisible(attributeForm + ":restrictionsContainer:rangeContainer");
    }

    @Test
    public void testRestrictionTypeOptionsComponents() {

        AttributeTypeInfoImpl model = new AttributeTypeInfoImpl();

        // init model to a string with options restriction
        model.setName("optioned");
        model.setBinding(String.class);
        model.setOptions(List.of("A", "b"));

        AttributeTypeInfoEditPanel dialog = new AttributeTypeInfoEditPanel("dialog", Model.of(model));

        tester.startComponentInPage(dialog);

        String attributeForm = "dialog:attributeForm";
        FormTester form = tester.newFormTester(attributeForm);

        // assert restrictions visible
        tester.assertVisible(attributeForm + ":restrictionsContainer");

        DropDownChoice<RESTRICTION_TYPE> restrictionTypeDropDown = (DropDownChoice)
                tester.getComponentFromLastRenderedPage(attributeForm + ":restrictionsContainer:restrictionType");

        String optionsContainer = attributeForm + ":restrictionsContainer:optionsContainer";

        // assert options restrictions status
        tester.assertModelValue(restrictionTypeDropDown.getPageRelativePath(), RESTRICTION_TYPE.OPTIONS);
        tester.assertVisible(optionsContainer);
        tester.assertInvisible(attributeForm + ":restrictionsContainer:rangeContainer");

        ListMultipleChoice<Object> optionsListMultipleChoice =
                (ListMultipleChoice<Object>) tester.getComponentFromLastRenderedPage(optionsContainer + ":options");

        // assert options values
        assertEquals(model.getOptions(), optionsListMultipleChoice.getChoices());

        // add option
        form.setValue("restrictionsContainer:optionsContainer:newOption", "C");
        tester.executeAjaxEvent(attributeForm + ":restrictionsContainer:optionsContainer:addOption", "click");

        // assert options values after adding option
        assertEquals(List.of("A", "b", "C"), optionsListMultipleChoice.getChoices());

        // remove option
        optionsListMultipleChoice.setModelObject(List.of("C"));
        tester.executeAjaxEvent(attributeForm + ":restrictionsContainer:optionsContainer:removeOption", "click");

        // assert options values after removing option
        assertEquals(List.of("A", "b"), optionsListMultipleChoice.getChoices());

        tester.assertNoErrorMessage();
    }

    @Test
    public void testRestrictionTypeRange() {

        AttributeTypeInfoImpl model = new AttributeTypeInfoImpl();
        AttributeTypeInfoEditPanel dialog = new AttributeTypeInfoEditPanel("dialog", Model.of(model));

        tester.startComponentInPage(dialog);

        String attributeForm = "dialog:attributeForm";
        FormTester form = tester.newFormTester(attributeForm);

        // assert restrictions not visible
        tester.assertInvisible(attributeForm + ":restrictionsContainer");

        // set attribute type to String
        form.setValue("type", "java.lang.Float");
        tester.executeAjaxEvent(attributeForm + ":type", "change");

        // assert restrictions visible
        tester.assertVisible(attributeForm + ":restrictionsContainer");

        DropDownChoice<RESTRICTION_TYPE> restrictionTypeDropDown = (DropDownChoice)
                tester.getComponentFromLastRenderedPage(attributeForm + ":restrictionsContainer:restrictionType");

        // assert restrictions empty status
        tester.assertModelValue(restrictionTypeDropDown.getPageRelativePath(), RESTRICTION_TYPE.NONE);
        tester.assertInvisible(attributeForm + ":restrictionsContainer:optionsContainer");
        tester.assertInvisible(attributeForm + ":restrictionsContainer:rangeContainer");

        // set range restriction type
        form.setValue("restrictionsContainer:restrictionType", RESTRICTION_TYPE.RANGE.name());
        tester.executeAjaxEvent(attributeForm + ":restrictionsContainer:restrictionType", "change");

        String rangeContainer = attributeForm + ":restrictionsContainer:rangeContainer";

        // assert range restriction components visibility
        tester.assertVisible(rangeContainer);
        tester.assertVisible(rangeContainer + ":rangeMin");
        tester.assertVisible(rangeContainer + ":rangeMax");
        tester.assertInvisible(attributeForm + ":restrictionsContainer:optionsContainer");
    }

    @Test
    public void testRestrictionTypeRangeComponents() {

        AttributeTypeInfoImpl model = new AttributeTypeInfoImpl();

        // init model to a double with range restriction
        model.setName("ranged");
        model.setBinding(Double.class);
        model.setRange(NumberRange.create(Math.E, Math.PI));

        AttributeTypeInfoEditPanel dialog = new AttributeTypeInfoEditPanel("dialog", Model.of(model));

        tester.startComponentInPage(dialog);

        String attributeForm = "dialog:attributeForm";
        FormTester form = tester.newFormTester(attributeForm);

        // assert restrictions visible
        tester.assertVisible(attributeForm + ":restrictionsContainer");

        DropDownChoice<RESTRICTION_TYPE> restrictionTypeDropDown = (DropDownChoice)
                tester.getComponentFromLastRenderedPage(attributeForm + ":restrictionsContainer:restrictionType");

        String rangeContainer = attributeForm + ":restrictionsContainer:rangeContainer";

        // assert options restrictions status
        tester.assertModelValue(restrictionTypeDropDown.getPageRelativePath(), RESTRICTION_TYPE.RANGE);
        tester.assertVisible(rangeContainer);
        tester.assertInvisible(attributeForm + ":restrictionsContainer:optionsContainer");

        NumberTextField<? extends Number> rangeMinTextField = (NumberTextField<? extends Number>)
                tester.getComponentFromLastRenderedPage(rangeContainer + ":rangeMin");
        NumberTextField<? extends Number> rangeMaxTextField = (NumberTextField<? extends Number>)
                tester.getComponentFromLastRenderedPage(rangeContainer + ":rangeMax");

        // assert range value
        tester.assertModelValue(rangeMinTextField.getPageRelativePath(), Math.E);
        tester.assertModelValue(rangeMaxTextField.getPageRelativePath(), Math.PI);

        tester.assertNoErrorMessage();
    }
}
