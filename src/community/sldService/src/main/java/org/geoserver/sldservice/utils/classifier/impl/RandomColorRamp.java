package org.geoserver.sldservice.utils.classifier.impl;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.geoserver.sldservice.utils.classifier.ColorRamp;

public class RandomColorRamp implements ColorRamp {

	private int classNum = 0;
	private List<Color> colors = new ArrayList();

	public int getNumClasses() {
		return classNum;
	}

	public List<Color> getRamp() throws Exception {
		if (colors == null)
			throw new Exception("Class num not setted, color ramp null");
		return colors;
	}

	public void revert() {
		// TODO Auto-generated method stub

	}

	public void setNumClasses(int numClass) {
		classNum = numClass;
		try {
			createRamp();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void createRamp() throws Exception {
		for (int i = 0; i < classNum; i++)
			colors.add(new Color((int) (Math.random() * 255), (int) (Math
					.random() * 255), (int) (Math.random() * 255)));
	}

}
