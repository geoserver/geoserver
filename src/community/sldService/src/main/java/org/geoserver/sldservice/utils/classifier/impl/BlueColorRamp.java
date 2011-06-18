package org.geoserver.sldservice.utils.classifier.impl;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.geoserver.sldservice.utils.classifier.ColorRamp;

public class BlueColorRamp implements ColorRamp {

	private int classNum = 0;
	private List<Color> colors = new ArrayList();

	public int getNumClasses() {
		return classNum;
	}

	public void revert() {

	}

	public void setNumClasses(int numClass) {
		classNum = numClass;
		createRamp();

	}

	public List<Color> getRamp() throws Exception {
		if (colors == null)
			throw new Exception("Class num not setted, color ramp null");
		return colors;
	}

	private void createRamp() {

		double step = (225.0 / (double) classNum);
		for (int i = 0; i < classNum; i++)
			colors.add(new Color(0, 0, (int) (step * i + 30)));
	}

}
