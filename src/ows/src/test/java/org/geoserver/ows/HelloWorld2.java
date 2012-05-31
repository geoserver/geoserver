package org.geoserver.ows;

public class HelloWorld2 extends HelloWorld {

    @Override
    public Message hello(Message message) {
        return new Message(message.message + ":V2"); 
    }
}
