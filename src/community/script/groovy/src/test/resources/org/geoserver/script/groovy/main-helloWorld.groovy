def run(request,response) {
  response.getWriter().write("Hello World!")
  response.setContentType("text/plain")
//  response.setEntity("Hello World!", MediaType.TEXT_PLAIN)
}