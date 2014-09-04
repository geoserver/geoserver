/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;


class TestGetThread extends Thread {
    private static final boolean concurrent = false;
    URL url;
    int result = 0;
    Date t1;
    Date t2;
    Date t3;
    protected HttpURLConnection hc;

    TestGetThread(URL u) throws MalformedURLException {
        url = new URL(u.toString());
    }

    int getResult() {
        return result;
    }

    public void connect() throws IOException {
        //HttpURLConnection hc = null;
        hc = (HttpURLConnection) url.openConnection();
        hc.setRequestMethod("GET");
        t1 = new Date();
        yield(); //wait to let everyone else connect before getting result
        hc.connect();
        //return hc;
    }

    public void run() {
        try {
            //HttpURLConnection hc = connect();
            //Unsure if we want this, this is an experiment to try to connect
            //more than once, which is what a reasonable web browser would
            //do I believe.  Or maybe web browsers only try once?  It's a bit
            //imperfect, as the input stream stuff should probably be here too
            //since if this is running it just shifts more errors to BindExceptions
            //when hc.getInputStream is called, since it's connected but not yet
            //bound - it doesn't have the actual stream yet.  So perhaps it needs
            //to be in the loop too, though would that cause it to get a new 
            //connection (from the connect method) if it failed?  And/or is that
            //maybe the behavior that we want?  Or do we want it to try to bind
            //on the same connection - ch. (should also make these options
            //user configurable...hmmm - how did I get roped in to writing
            //a java wfs client?  This is essentially what the wfsTester that
            //rob wrote does, and it's about as naive, it just doesn't do
            //threads.  If anyone wants to expand on this the one nice thing
            //to have, that rob never got to, would be some validation
            //as well.  Shouldn't be too hard, just run xerces schema
            //checking on it.  But then again this class was written to test
            //a ton of requests at once, not to be a full tester.  One thing
            //that would be cool though is to take a list of requests and
            //fire them all off at different intervals.  Though I feel like
            //we might be able to find some better framework to do that,
            //rather than just writing our own.  
            int tries = 0;
            while (tries < 15) {
            try {
				connect();
				break;
            } catch (IOException ioe){
				System.out.println("IOException: " + ioe.getMessage() +", try: " + tries);
            	
            	sleep(5);
            	tries++;
            }
            }

            t2 = new Date();

            InputStream input = hc.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(input));

            if (concurrent) {
                yield();
            }

			//This reading bit seems to be damned if you do damned if you dont
			//The first way ensures that the client doesn't prematurely close
			//the socket, which happens on big GetFeature requests all the 
			//time, I'm not too sure why, perhaps it gets set to not ready
			//when the underlying stream from the server pauses or something.
			
			//The second way ensures that we don't get errors with the input
			//stream closing on us, but for some reason I can't seem to reproduce
			//them right now - the only reason I can think of is because I 
			//changed it to explicitly create the InputStream, so it doesn't 
			//get gced or something?  I'm not too sure though, but as the first
			//isn't messing up at all we'll stick with it, as the second (original)
			//way wasn't actually reading everything, so it was returning false 
			//response times.
			
			//Oh ok, it seems that the exception is only on resin, with ready
			//it works with getCaps, but with read it messes up about one in
			//40 or so with java.net.SocketException: socket closed - so it
			//looks like resin decides to close the socket prematurely under
			//heavy duress or something.  I'm not sure, but I definitely feel
			//like I'm testing servlet containers far more than GeoServer
			//itself with this solid day of testing. 
			
            while (br.read() != -1);

            //while(br.ready())
            //	br.readLine();
            result = hc.getResponseCode();
            t3 = new Date();

            yield(); //wait to let everyone else hit before disconnecting
            hc.disconnect();
            ThreadedBatchTester.threadDone();
        } catch (Exception e) {
            e.printStackTrace();
            try {
            result = hc.getResponseCode();
            } catch (IOException ioe) {
            	result = 0;
            }
            ThreadedBatchTester.threadDone();
        }
    }

    Date getTime1() {
        return t1;
    }

    Date getTime2() {
        return t2;
    }

    Date getTime3() {
        return t3;
    }
}


class TestPostThread extends TestGetThread {
    String request;

    TestPostThread(URL u, String request) throws MalformedURLException {
        super(u);
        this.request = request;
    }

    public void connect() throws IOException {
        //HttpURLConnection hc = null;
        hc = (HttpURLConnection) url.openConnection();
        hc.setRequestMethod("POST");
        hc.setDoOutput(true);
        t1 = new Date();
        yield(); //wait to let everyone else connect before getting result
        hc.connect();

        OutputStreamWriter osw = new OutputStreamWriter(hc.getOutputStream());
        osw.write(request);
        osw.flush();
    }

    /*public void run(){
       try{
               HttpURLConnection hc = null;
               hc = (HttpURLConnection)url.openConnection();
               hc.setRequestMethod("POST");
               hc.setDoOutput(true);
               yield(); //wait to let everyone else connect before getting result
    
               t1 = new Date();
               hc.connect();
               OutputStreamWriter osw = new OutputStreamWriter(hc.getOutputStream());
               osw.write(request);
               osw.flush();
    
               t2 = new Date();
           BufferedReader br = new BufferedReader(new InputStreamReader(hc.getInputStream()));
           while(br.ready()){
                   br.readLine();
           }
               result = hc.getResponseCode();
               t3 = new Date();
               yield(); //wait to let everyone else hit before disconnecting
               hc.disconnect();
       }catch(Exception e){
               e.printStackTrace();
               result = 0;
       }
       }*/
}
