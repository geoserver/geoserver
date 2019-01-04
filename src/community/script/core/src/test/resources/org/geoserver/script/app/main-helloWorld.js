run = function(request, response) {
  //response.setEntity("Hello World!", Packages.org.restlet.data.MediaType.TEXT_PLAIN);
  response.setContentType("text/plain");
  response.getWriter().write("Hello World!");
}
