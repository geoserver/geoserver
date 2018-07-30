/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.model.label;

import java.util.List;

/**
 *
 * @author Juan Marin, OpenGeo
 *
 */
public class LabelInfo {

    private List<Label> type;

    public List<Label> getType() {
        return type;
    }

    public void setType(List<Label> labels) {
        this.type = labels;
    }

    public LabelInfo(List<Label> labels) {
        super();
        this.type = labels;
    }
}
