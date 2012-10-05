import org.restlet.data.MediaType

def run(request,response) {
  response.setEntity("Hello World!", MediaType.TEXT_PLAIN)
}