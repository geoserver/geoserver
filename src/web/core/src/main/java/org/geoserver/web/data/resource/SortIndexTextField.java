package org.geoserver.web.data.resource;

import org.apache.wicket.markup.html.form.AbstractTextComponent.ITextFormatProvider;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.string.Strings;

public class SortIndexTextField extends TextField<Integer> implements ITextFormatProvider {

	private static final long serialVersionUID = -8830614532379530837L;

	public SortIndexTextField(String fieldName) {
		super(fieldName);
	}

	@Override
	protected void onBeforeRender() {
		hideIntegerMax();
		super.onBeforeRender();
	}

	public void hideIntegerMax() {

		IModel<Integer> intModel = (IModel<Integer>) getDefaultModel();

		if(Integer.MAX_VALUE == intModel.getObject()) {
			intModel.setObject(null);
		}	
	}

	@Override
	public void convertInput()
	{
		String[] value = getInputAsArray();
		String tmp = value != null && value.length > 0 ? value[0] : null;
		if (Strings.isEmpty(tmp))
		{
			// No value means sort as last
			setConvertedInput(Integer.MAX_VALUE);
		} else {
			super.convertInput();
		}
	}

	@Override
	public String getTextFormat() {
		return null;		  
	}	
}

