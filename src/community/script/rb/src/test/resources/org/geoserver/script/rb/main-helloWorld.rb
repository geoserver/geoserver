def run(request, response)
  response.setEntity("Hello World!", org.restlet.data.MediaType::TEXT_PLAIN)
end