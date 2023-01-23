.. _webp_wms_output_format:

WMS WebP output format
============================

This module adds the `WebP <https://developers.google.com/speed/webp>`_ WMS output format.

WMS GetMap Request: *FORMAT=image/webp*

Advantages of the WebP image format compared to other formats:

* | WebP lossy images are about 30% smaller than comparable JPEG images.
* | WebP supports transparency, typically providing 3x smaller file sizes compared to PNG.
* | WebP supports :ref:`paletted images <tutorials_palettedimages>`, typically providing 20% smaller file sizes compared to PNG.

WebP is supported by all modern browsers (`caniuse <https://caniuse.com/webp>`_).
However, backwards compatibility can be built in on the client side.

Because native libraries are used, not all platforms are supported.
The plugin is based on `Java ImageIO WebP support <https://github.com/gotson/webp-imageio>`_, here you can find further information.

.. toctree::
   :maxdepth: 1

   installing
