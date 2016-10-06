FAQ
***

**All I get in my PDF is: "ERROR: infinite table loop". What's wrong?**
  Something in your page is too big. For example, the width or the height of your !map block.

**I tried to print (pylons mode) and I get a "Java error". What's next?**
  Look in the apache error log, you'll find more information.

**What are the limitations of the type 2 layers?**
  It depends mostly on the map server you use. For the moment, GeoServer has not been extensively tested. With MapServer:

  * The PDF output must be enabled when you compile and doesn't work in WMS mode, only in native MapServer mode. There are some limitations. on the styling. And you must use truetype fonts.
  * The SVG output is limited regarding the stylings you can use. For example only plain polygon fillings are supported by MapServer. If a complex styling is used, your features may appear plain black.

**I tried to change the layout and half the Map is printed off the page on the right. Or I have an empty page added. Is it a bug?**
  It's mostly a feature ;-) . This kind of behavior can be seen in iText, when adding a block that is too big for the page size. Try to reduce the size of your map block.

**When I look at my generated PDF in Acrobat Reader, it looks good. But, when I print it, it misses some tiles/layers, some bitmaps are just weird or there are no page printed. What's wrong?**
  There are three possible explanations:

  * Your printer has not enough memory: in Acrobat's print dialog, select "Save printer memory"
  * Your printer firmware is buggy: upgrade it
  * Your printer driver is buggy: upgrade it

**The module needs to go through a proxy to acces the map services.**
  It's so 90s... you should hire some fresh guys for your IT team. ;-)

  You need to set some system properties (http.proxy*) when you start your java programs.

**On the browser, the scale is displayed with spaces to separate thousands and it's against my religion. How do I put my sacred separator?**
  By default, the browser's configured locale is used. You can force another locale in the print widget configuration:

  .. code-block:: yaml

    {
      ...
      configUrl: 'print/info.json',
      serviceParams: { locale: 'fr_CH' },
      ...
    }

**I copied the examples and the print widgets are not working.**
  First edit the client/examples/examples.js file and make sure the URLs are correct.

  1. If you don't want to install the server side, make sure you installed a proxy (see Configure Proxy). For example, test (must return a JSON content, not the proxy.cgi script's content) it with an URL like that (adapt the hostname, port and path): `http://localhost/cgi-bin/proxy.cgi?url=http://demo.mapfish.org/mapfishsample/trunk/print/info.json`
  2. If you installed the server side, make sure it works by calling the URL specified in the mapfish.SERVER_BASE_URL variable (must be the hostname/port your page is accessed through) added with `/print/info.json`. For example, if you have `mapfish.SERVER_BASE_URL="http://localhost/mapfish": http://localhost/mapfish/print/info.json`

  If it still doesn't work, use firefox, install firebug and check in the console panel that the AJAX request made by the print widget works fine.
  
Warranty disclaimer and license
-------------------------------

The authors provide these documents "AS-IS", without warranty of any kind
either expressed or implied.

Document under `Creative Common License Attribution-Share Alike 2.5 Generic
<http://creativecommons.org/licenses/by-sa/2.5/>`_.

Authors: MapFish developers.