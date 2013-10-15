package com.forwardslope.c2rpc.triggers;

import javax.xml.namespace.QName;

public class LinkEx extends Link {
    @Override
    public QName getForeign() {
        return foreign == null ? key : foreign;
    }
}
