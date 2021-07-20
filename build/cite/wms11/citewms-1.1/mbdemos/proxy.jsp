<%
/**
 * This is a JSP replacement for ProxyRedirect servlet from mapbuilder,
 * so it is no needed to include the servlet into the geoserver code base
 */

	if("GET".equals(request.getMethod())){
		//execute the GET
		String serverUrl = request.getParameter("url");
		java.net.URL url = new java.net.URL(serverUrl);
		if (!"http".equals(url.getProtocol())) {
			throw new javax.servlet.ServletException(
					"only use HTTP Url's, please don't hack this server!");
		}
		java.io.InputStream in = url.openStream();

		response.setContentType("text/xml");
		byte[] buff = new byte[1024];
		int count;
		java.io.OutputStream o = response.getOutputStream();
		while ((count = in.read(buff)) > -1) {
			o.write(buff, 0, count);
		}
		o.flush();
		o.close();
	}else{
		//execute the POST
    try {
      // Transfer bytes from in to out
      java.io.PrintWriter o = response.getWriter();
      javax.servlet.ServletInputStream in = request.getInputStream();
      
      org.apache.commons.httpclient.HttpClient client = new org.apache.commons.httpclient.HttpClient();

      String serverUrl = request.getHeader("serverUrl");
      org.apache.commons.httpclient.methods.PostMethod httppost = new org.apache.commons.httpclient.methods.PostMethod(serverUrl);

      httppost.setRequestBody(in);
      //httppost.setRequestContentLength(PostMethod.CONTENT_LENGTH_CHUNKED);

      client.executeMethod(httppost);

      if (httppost.getStatusCode() == org.apache.commons.httpclient.HttpStatus.SC_OK) {
        response.setContentType("text/xml");
        String responseBody = httppost.getResponseBodyAsString();
        response.setContentLength(responseBody.length());
        System.out.println("responseBody:" + responseBody);
        o.print( responseBody );
      } else {
        throw new javax.servlet.ServletException("Unexpected failure: " + httppost.getStatusLine().toString());
      }
      httppost.releaseConnection();
      o.flush();
      o.close();
    } catch (java.io.IOException e) {
      throw new javax.servlet.ServletException(e);
    }    
	}
	%>
