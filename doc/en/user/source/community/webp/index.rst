.. _webp_wms_output_format:

WMS WebP output format
============================

This module adds the `WebP <https://developers.google.com/speed/webp>`_ WMS output format.

WMS GetMap Request: *FORMAT=image/webp*

Advantages of the WebP image format compared to other formats:

* | WebP lossy images are about 30% smaller than comparable JPEG images.
* | WebP supports transparency, typically providing 3x smaller file sizes compared to PNG.
* | WebP supports :ref:`paletted images <tutorials_palettedimages>`, typically providing 20% smaller file sizes compared to PNG.

**Attention! Unfortunately, all the advantages of the WebP format regarding file size are negated by
a more complex, time and energy-consuming processing.**

**However, the WebP format could serve as an input format for the GWC and then play out the advantages again.
Work is in progress.**

**Read more about it here:** :ref:`WebP processing <webp_processing>`

Only in exceptional cases where a slow internet connection is given (e.g. G3) this format makes sense.

WebP is supported by all modern browsers (`caniuse <https://caniuse.com/webp>`_).
However, backwards compatibility can be built in on the client side.
Use `Google's recommended function <https://developers.google.com/speed/webp/faq#how_can_i_detect_browser_support_for_webp>`_ to detect with Javascript if the browser supports WebP.
Be aware that the used image-loading is non-blocking and asynchronous. Any code that depends should preferably be put in the callback function.

Example for `OpenLayers v7 <https://openlayers.org/>`_ WebP browser support check via javascript:

.. code-block:: javascript

  ...
  import ImageLayer from 'ol/layer/Image';
  import ImageWMS from 'ol/source/ImageWMS';
  ...
  function check_webp_feature(feature, callback) {
  ... // code from google link above
  }
  check_webp_feature('lossless', function (feature, isSupported) {
    let wmsoutputformat = 'image/webp'
    if (!isSupported) {
	   wmsoutputformat = 'image/png'
    }
    var wmsLayerSource = new ImageWMS({
      params: {'LAYERS': 'yourLayerName','FORMAT': wmsoutputformat},
      ...
    });
    ... // your OL code
  });



Because native libraries are used, not all platforms are supported. For those supported, no additional native library needs to be installed though.
The plugin is based on `Java ImageIO WebP support <https://github.com/gotson/webp-imageio>`_, here you can find further information.
If your platform is not supported, there are `instructions for compiling the native library <https://github.com/gotson/webp-imageio#compiling>`_.
In this case, do not forget to contribute.

.. toctree::
   :maxdepth: 1

   installing
   webp_processing
