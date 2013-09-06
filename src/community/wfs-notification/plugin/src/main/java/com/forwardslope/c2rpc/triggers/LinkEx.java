package com.forwardslope.c2rpc.triggers;

public class LinkEx extends Link {
    @Override
    public String getForeign() {
        return foreign == null ? key : foreign;
    }
}
