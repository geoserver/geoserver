package com.forwardslope.c2rpc.triggers;

public class ObjectFactoryEx extends ObjectFactory {
    @Override
    public Link createLink() {
        return new LinkEx();
    }
}
