/* This notice must be untouched at all times.

wz_jsgraphics.js    v. 2.3
The latest version is available at
http://www.walterzorn.com
or http://www.devira.com
or http://www.walterzorn.de

Copyright (c) 2002-2004 Walter Zorn. All rights reserved.
Created 3. 11. 2002 by Walter Zorn (Web: http://www.walterzorn.com )
Last modified: 15. 3. 2004

Performance optimizations for Internet Explorer
by Thomas Frank and John Holdsworth.
fillPolygon method implemented by Matthieu Haller.

High Performance JavaScript Graphics Library.
Provides methods
- to draw lines, rectangles, ellipses, polygons
  with specifiable line thickness,
- to fill rectangles and ellipses
- to draw text.
NOTE: Operations, functions and branching have rather been optimized
to efficiency and speed than to shortness of source code.

This program is free software;
you can redistribute it and/or modify it under the terms of the
GNU General Public License as published by the Free Software Foundation;
either version 2 of the License, or (at your option) any later version.
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the GNU General Public License
at http://www.gnu.org/copyleft/gpl.html for more details.
*/


var jg_ihtm, jg_ie, jg_fast, jg_dom, jg_moz,
jg_n4 = (document.layers && typeof document.classes != "undefined");


function chkDHTM(x, i)
{
	x = document.body || null;
	jg_ie = x && typeof x.insertAdjacentHTML != "undefined";
	jg_dom = (x && !jg_ie &&
		typeof x.appendChild != "undefined" &&
		typeof document.createRange != "undefined" &&
		typeof (i = document.createRange()).setStartBefore != "undefined" &&
		typeof i.createContextualFragment != "undefined");
	jg_ihtm = !jg_ie && !jg_dom && x && typeof x.innerHTML != "undefined";
	jg_fast = jg_ie && document.all && !window.opera;
	jg_moz = jg_dom && typeof x.style.MozOpacity != "undefined";
}


function pntDoc()
{
	this.wnd.document.write(jg_fast? this.htmRpc() : this.htm);
	this.htm = '';
}


function pntCnvDom()
{
	var x = document.createRange();
	x.setStartBefore(this.cnv);
	x = x.createContextualFragment(jg_fast? this.htmRpc() : this.htm);
	this.cnv.appendChild(x);
	this.htm = '';
}


function pntCnvIe()
{
	this.cnv.insertAdjacentHTML("beforeEnd", jg_fast? this.htmRpc() : this.htm);
	this.htm = '';
}


function pntCnvIhtm()
{
	this.cnv.innerHTML += this.htm;
	this.htm = '';
}


function pntCnv()
{
	this.htm = '';
}


function mkDiv(x, y, w, h)
{
	this.htm += '<div style="position:absolute;'+
		'left:' + x + 'px;'+
		'top:' + y + 'px;'+
		'width:' + w + 'px;'+
		'height:' + h + 'px;'+
		'clip:rect(0,'+w+'px,'+h+'px,0);'+
		'background-color:' + this.color +
		(!jg_moz? ';overflow:hidden' : '')+
		';"><\/div>';
}


function mkDivIe(x, y, w, h)
{
	this.htm += '%%'+this.color+';'+x+';'+y+';'+w+';'+h+';';
}


function mkDivPrt(x, y, w, h)
{
	this.htm += '<div style="position:absolute;'+
		'border-left:' + w + 'px solid ' + this.color + ';'+
		'left:' + x + 'px;'+
		'top:' + y + 'px;'+
		'width:0px;'+
		'height:' + h + 'px;'+
		'clip:rect(0,'+w+'px,'+h+'px,0);'+
		'background-color:' + this.color +
		(!jg_moz? ';overflow:hidden' : '')+
		';"><\/div>';
}


function mkLyr(x, y, w, h)
{
	this.htm += '<layer '+
		'left="' + x + '" '+
		'top="' + y + '" '+
		'width="' + w + '" '+
		'height="' + h + '" '+
		'bgcolor="' + this.color + '"><\/layer>\n';
}


var regex =  /%%([^;]+);([^;]+);([^;]+);([^;]+);([^;]+);/g;
function htmRpc()
{
	return this.htm.replace(
		regex,
		'<div style="overflow:hidden;position:absolute;background-color:'+
		'$1;left:$2;top:$3;width:$4;height:$5"></div>\n');
}


function htmPrtRpc()
{
	return this.htm.replace(
		regex,
		'<div style="overflow:hidden;position:absolute;background-color:'+
		'$1;left:$2;top:$3;width:$4;height:$5;border-left:$4px solid $1"></div>\n');
}


function mkLin(x1, y1, x2, y2)
{
	if (x1 > x2)
	{
		var _x2 = x2;
		var _y2 = y2;
		x2 = x1;
		y2 = y1;
		x1 = _x2;
		y1 = _y2;
	}
	var dx = x2-x1, dy = Math.abs(y2-y1),
	x = x1, y = y1,
	yIncr = (y1 > y2)? -1 : 1;

	if (dx >= dy)
	{
		var pr = dy<<1,
		pru = pr - (dx<<1),
		p = pr-dx,
		ox = x;
		while ((dx--) > 0)
		{
			++x;
			if (p > 0)
			{
				this.mkDiv(ox, y, x-ox, 1);
				y += yIncr;
				p += pru;
				ox = x;
			}
			else p += pr;
		}
		this.mkDiv(ox, y, x2-ox+1, 1);
	}

	else
	{
		var pr = dx<<1,
		pru = pr - (dy<<1),
		p = pr-dy,
		oy = y;
		if (y2 <= y1)
		{
			while ((dy--) > 0)
			{
				if (p > 0)
				{
					this.mkDiv(x++, y, 1, oy-y+1);
					y += yIncr;
					p += pru;
					oy = y;
				}
				else
				{
					y += yIncr;
					p += pr;
				}
			}
			this.mkDiv(x2, y2, 1, oy-y2+1);
		}
		else
		{
			while ((dy--) > 0)
			{
				y += yIncr;
				if (p > 0)
				{
					this.mkDiv(x++, oy, 1, y-oy);
					p += pru;
					oy = y;
				}
				else p += pr;
			}
			this.mkDiv(x2, oy, 1, y2-oy+1);
		}
	}
}


function mkLin2D(x1, y1, x2, y2)
{
	if (x1 > x2)
	{
		var _x2 = x2;
		var _y2 = y2;
		x2 = x1;
		y2 = y1;
		x1 = _x2;
		y1 = _y2;
	}
	var dx = x2-x1, dy = Math.abs(y2-y1),
	x = x1, y = y1,
	yIncr = (y1 > y2)? -1 : 1;

	var s = this.stroke;
	if (dx >= dy)
	{
		if (s-3 > 0)
		{
			var _s = (s*dx*Math.sqrt(1+dy*dy/(dx*dx))-dx-(s>>1)*dy) / dx;
			_s = (!(s-4)? Math.ceil(_s) : Math.round(_s)) + 1;
		}
		else var _s = s;
		var ad = Math.ceil(s/2);

		var pr = dy<<1,
		pru = pr - (dx<<1),
		p = pr-dx,
		ox = x;
		while ((dx--) > 0)
		{
			++x;
			if (p > 0)
			{
				this.mkDiv(ox, y, x-ox+ad, _s);
				y += yIncr;
				p += pru;
				ox = x;
			}
			else p += pr;
		}
		this.mkDiv(ox, y, x2-ox+ad+1, _s);
	}

	else
	{
		if (s-3 > 0)
		{
			var _s = (s*dy*Math.sqrt(1+dx*dx/(dy*dy))-(s>>1)*dx-dy) / dy;
			_s = (!(s-4)? Math.ceil(_s) : Math.round(_s)) + 1;
		}
		else var _s = s;
		var ad = Math.round(s/2);

		var pr = dx<<1,
		pru = pr - (dy<<1),
		p = pr-dy,
		oy = y;
		if (y2 <= y1)
		{
			++ad;
			while ((dy--) > 0)
			{
				if (p > 0)
				{
					this.mkDiv(x++, y, _s, oy-y+ad);
					y += yIncr;
					p += pru;
					oy = y;
				}
				else
				{
					y += yIncr;
					p += pr;
				}
			}
			this.mkDiv(x2, y2, _s, oy-y2+ad);
		}
		else
		{
			while ((dy--) > 0)
			{
				y += yIncr;
				if (p > 0)
				{
					this.mkDiv(x++, oy, _s, y-oy+ad);
					p += pru;
					oy = y;
				}
				else p += pr;
			}
			this.mkDiv(x2, oy, _s, y2-oy+ad+1);
		}
	}
}


function mkLinDott(x1, y1, x2, y2)
{
	if (x1 > x2)
	{
		var _x2 = x2;
		var _y2 = y2;
		x2 = x1;
		y2 = y1;
		x1 = _x2;
		y1 = _y2;
	}
	var dx = x2-x1, dy = Math.abs(y2-y1),
	x = x1, y = y1,
	yIncr = (y1 > y2)? -1 : 1,
	drw = true;
	if (dx >= dy)
	{
		var pr = dy<<1,
		pru = pr - (dx<<1),
		p = pr-dx;
		while ((dx--) > 0)
		{
			if (drw) this.mkDiv(x, y, 1, 1);
			drw = !drw;
			if (p > 0)
			{
				y += yIncr;
				p += pru;
			}
			else p += pr;
			++x;
		}
		if (drw) this.mkDiv(x, y, 1, 1);
	}

	else
	{
		var pr = dx<<1,
		pru = pr - (dy<<1),
		p = pr-dy;
		while ((dy--) > 0)
		{
			if (drw) this.mkDiv(x, y, 1, 1);
			drw = !drw;
			y += yIncr;
			if (p > 0)
			{
				++x;
				p += pru;
			}
			else p += pr;
		}
		if (drw) this.mkDiv(x, y, 1, 1);
	}
}


function mkOv(left, top, width, height)
{
	var a = width>>1, b = height>>1,
	wod = width&1, hod = (height&1)+1,
	cx = left+a, cy = top+b,
	x = 0, y = b,
	ox = 0, oy = b,
	aa = (a*a)<<1, bb = (b*b)<<1,
	st = (aa>>1)*(1-(b<<1)) + bb,
	tt = (bb>>1) - aa*((b<<1)-1),
	w, h;
	while (y > 0)
	{
		if (st < 0)
		{
			st += bb*((x<<1)+3);
			tt += (bb<<1)*(++x);
		}
		else if (tt < 0)
		{
			st += bb*((x<<1)+3) - (aa<<1)*(y-1);
			tt += (bb<<1)*(++x) - aa*(((y--)<<1)-3);
			w = x-ox;
			h = oy-y;
			if (w&2 && h&2)
			{
				this.mkOvQds(cx, cy, -x+2, ox+wod, -oy, oy-1+hod, 1, 1);
				this.mkOvQds(cx, cy, -x+1, x-1+wod, -y-1, y+hod, 1, 1);
			}
			else this.mkOvQds(cx, cy, -x+1, ox+wod, -oy, oy-h+hod, w, h);
			ox = x;
			oy = y;
		}
		else
		{
			tt -= aa*((y<<1)-3);
			st -= (aa<<1)*(--y);
		}
	}
	this.mkDiv(cx-a, cy-oy, a-ox+1, (oy<<1)+hod);
	this.mkDiv(cx+ox+wod, cy-oy, a-ox+1, (oy<<1)+hod);
}


function mkOv2D(left, top, width, height)
{
	var s = this.stroke;
	width += s-1;
	height += s-1;
	var a = width>>1, b = height>>1,
	wod = width&1, hod = (height&1)+1,
	cx = left+a, cy = top+b,
	x = 0, y = b,
	aa = (a*a)<<1, bb = (b*b)<<1,
	st = (aa>>1)*(1-(b<<1)) + bb,
	tt = (bb>>1) - aa*((b<<1)-1);

	if (s-4 < 0 && (!(s-2) || width-51 > 0 && height-51 > 0))
	{
		var ox = 0, oy = b,
		w, h,
		pxl, pxr, pxt, pxb, pxw;
		while (y > 0)
		{
			if (st < 0)
			{
				st += bb*((x<<1)+3);
				tt += (bb<<1)*(++x);
			}
			else if (tt < 0)
			{
				st += bb*((x<<1)+3) - (aa<<1)*(y-1);
				tt += (bb<<1)*(++x) - aa*(((y--)<<1)-3);
				w = x-ox;
				h = oy-y;

				if (w-1)
				{
					pxw = w+1+(s&1);
					h = s;
				}
				else if (h-1)
				{
					pxw = s;
					h += 1+(s&1);
				}
				else pxw = h = s;
				this.mkOvQds(cx, cy, -x+1, ox-pxw+w+wod, -oy, -h+oy+hod, pxw, h);
				ox = x;
				oy = y;
			}
			else
			{
				tt -= aa*((y<<1)-3);
				st -= (aa<<1)*(--y);
			}
		}
		this.mkDiv(cx-a, cy-oy, s, (oy<<1)+hod);
		this.mkDiv(cx+a+wod-s+1, cy-oy, s, (oy<<1)+hod);
	}

	else
	{
		var _a = (width-((s-1)<<1))>>1,
		_b = (height-((s-1)<<1))>>1,
		_x = 0, _y = _b,
		_aa = (_a*_a)<<1, _bb = (_b*_b)<<1,
		_st = (_aa>>1)*(1-(_b<<1)) + _bb,
		_tt = (_bb>>1) - _aa*((_b<<1)-1),

		pxl = new Array(),
		pxt = new Array(),
		_pxb = new Array();
		pxl[0] = 0;
		pxt[0] = b;
		_pxb[0] = _b-1;
		while (y > 0)
		{
			if (st < 0)
			{
				st += bb*((x<<1)+3);
				tt += (bb<<1)*(++x);
				pxl[pxl.length] = x;
				pxt[pxt.length] = y;
			}
			else if (tt < 0)
			{
				st += bb*((x<<1)+3) - (aa<<1)*(y-1);
				tt += (bb<<1)*(++x) - aa*(((y--)<<1)-3);
				pxl[pxl.length] = x;
				pxt[pxt.length] = y;
			}
			else
			{
				tt -= aa*((y<<1)-3);
				st -= (aa<<1)*(--y);
			}

			if (_y > 0)
			{
				if (_st < 0)
				{
					_st += _bb*((_x<<1)+3);
					_tt += (_bb<<1)*(++_x);
					_pxb[_pxb.length] = _y-1;
				}
				else if (_tt < 0)
				{
					_st += _bb*((_x<<1)+3) - (_aa<<1)*(_y-1);
					_tt += (_bb<<1)*(++_x) - _aa*(((_y--)<<1)-3);
					_pxb[_pxb.length] = _y-1;
				}
				else
				{
					_tt -= _aa*((_y<<1)-3);
					_st -= (_aa<<1)*(--_y);
					_pxb[_pxb.length-1]--;
				}
			}
		}

		var ox = 0, oy = b,
		_oy = _pxb[0],
		l = pxl.length,
		w, h;
		for (var i = 0; i < l; i++)
		{
			if (typeof _pxb[i] != "undefined")
			{
				if (_pxb[i] < _oy || pxt[i] < oy)
				{
					x = pxl[i];
					this.mkOvQds(cx, cy, -x+1, ox+wod, -oy, _oy+hod, x-ox, oy-_oy);
					ox = x;
					oy = pxt[i];
					_oy = _pxb[i];
				}
			}
			else
			{
				x = pxl[i];
				this.mkDiv(cx-x+1, cy-oy, 1, (oy<<1)+hod);
				this.mkDiv(cx+ox+wod, cy-oy, 1, (oy<<1)+hod);
				ox = x;
				oy = pxt[i];
			}
		}
		this.mkDiv(cx-a, cy-oy, 1, (oy<<1)+hod);
		this.mkDiv(cx+ox+wod, cy-oy, 1, (oy<<1)+hod);
	}
}


function mkOvDott(left, top, width, height)
{
	var a = width>>1, b = height>>1,
	wod = width&1, hod = height&1,
	cx = left+a, cy = top+b,
	x = 0, y = b,
	aa2 = (a*a)<<1, aa4 = aa2<<1, bb = (b*b)<<1,
	st = (aa2>>1)*(1-(b<<1)) + bb,
	tt = (bb>>1) - aa2*((b<<1)-1),
	drw = true;
	while (y > 0)
	{
		if (st < 0)
		{
			st += bb*((x<<1)+3);
			tt += (bb<<1)*(++x);
		}
		else if (tt < 0)
		{
			st += bb*((x<<1)+3) - aa4*(y-1);
			tt += (bb<<1)*(++x) - aa2*(((y--)<<1)-3);
		}
		else
		{
			tt -= aa2*((y<<1)-3);
			st -= aa4*(--y);
		}
		if (drw) this.mkOvQds(cx, cy, -x, x+wod, -y, y+hod, 1, 1);
		drw = !drw;
	}
}


function mkRect(x, y, w, h)
{
	var s = this.stroke;
	this.mkDiv(x, y, w, s);
	this.mkDiv(x+w, y, s, h);
	this.mkDiv(x, y+h, w+s, s);
	this.mkDiv(x, y+s, s, h-s);
}


function mkRectDott(x, y, w, h)
{
	this.drawLine(x, y, x+w, y);
	this.drawLine(x+w, y, x+w, y+h);
	this.drawLine(x, y+h, x+w, y+h);
	this.drawLine(x, y, x, y+h);
}


function jsgFont()
{
	this.PLAIN = 'font-weight:normal;';
	this.BOLD = 'font-weight:bold;';
	this.ITALIC = 'font-style:italic;';
	this.ITALIC_BOLD = this.ITALIC + this.BOLD;
	this.BOLD_ITALIC = this.ITALIC_BOLD;
}
var Font = new jsgFont();


function jsgStroke()
{
	this.DOTTED = -1;
}
var Stroke = new jsgStroke();


function jsGraphics(id, wnd)
{
	this.setColor = new Function('arg', 'this.color = arg.toLowerCase();');

	this.setStroke = function(x)
	{
		this.stroke = x;
		if (!(x+1))
		{
			this.drawLine = mkLinDott;
			this.mkOv = mkOvDott;
			this.drawRect = mkRectDott;
		}
		else if (x-1 > 0)
		{
			this.drawLine = mkLin2D;
			this.mkOv = mkOv2D;
			this.drawRect = mkRect;
		}
		else
		{
			this.drawLine = mkLin;
			this.mkOv = mkOv;
			this.drawRect = mkRect;
		}
	};


	this.setPrintable = function(arg)
	{
		this.printable = arg;
		if (jg_fast)
		{
			this.mkDiv = mkDivIe;
			this.htmRpc = arg? htmPrtRpc : htmRpc;
		}
		else this.mkDiv = jg_n4? mkLyr : arg? mkDivPrt : mkDiv;
	};


	this.setFont = function(fam, sz, sty)
	{
		this.ftFam = fam;
		this.ftSz = sz;
		this.ftSty = sty || Font.PLAIN;
	};


	this.drawPolyline = this.drawPolyLine = function(x, y, s)
	{
		for (var i=0 ; i<x.length-1 ; i++ )
			this.drawLine(x[i], y[i], x[i+1], y[i+1]);
	};


	this.fillRect = function(x, y, w, h)
	{
		this.mkDiv(x, y, w, h);
	};


	this.drawPolygon = function(x, y)
	{
		this.drawPolyline(x, y);
		this.drawLine(x[x.length-1], y[x.length-1], x[0], y[0]);
	};


	this.drawEllipse = this.drawOval = function(x, y, w, h)
	{
		this.mkOv(x, y, w, h);
	};


	this.fillEllipse = this.fillOval = function(left, top, w, h)
	{
		var a = (w -= 1)>>1, b = (h -= 1)>>1,
		wod = (w&1)+1, hod = (h&1)+1,
		cx = left+a, cy = top+b,
		x = 0, y = b,
		ox = 0, oy = b,
		aa2 = (a*a)<<1, aa4 = aa2<<1, bb = (b*b)<<1,
		st = (aa2>>1)*(1-(b<<1)) + bb,
		tt = (bb>>1) - aa2*((b<<1)-1),
		pxl, dw, dh;
		if (w+1) while (y > 0)
		{
			if (st < 0)
			{
				st += bb*((x<<1)+3);
				tt += (bb<<1)*(++x);
			}
			else if (tt < 0)
			{
				st += bb*((x<<1)+3) - aa4*(y-1);
				pxl = cx-x;
				dw = (x<<1)+wod;
				tt += (bb<<1)*(++x) - aa2*(((y--)<<1)-3);
				dh = oy-y;
				this.mkDiv(pxl, cy-oy, dw, dh);
				this.mkDiv(pxl, cy+oy-dh+hod, dw, dh);
				ox = x;
				oy = y;
			}
			else
			{
				tt -= aa2*((y<<1)-3);
				st -= aa4*(--y);
			}
		}
		this.mkDiv(cx-a, cy-oy, w+1, (oy<<1)+hod);
	};



/* fillPolygon method, implemented by Matthieu Haller.
This javascript function is an adaptation of the gdImageFilledPolygon for Walter Zorn lib.
C source of GD 1.8.4 found at http://www.boutell.com/gd/

THANKS to Kirsten Schulz for the polygon fixes!

The intersection finding technique of this code could be improved
by remembering the previous intertersection, and by using the slope.
That could help to adjust intersections to produce a nice
interior_extrema. */
	this.fillPolygon = function(array_x, array_y)
	{
		var i;
		var y;
		var miny, maxy;
		var x1, y1;
		var x2, y2;
		var ind1, ind2;
		var ints;

		var n = array_x.length;

		if (!n) return;


		miny = array_y[0];
		maxy = array_y[0];
		for (i = 1; i < n; i++)
		{
			if (array_y[i] < miny)
				miny = array_y[i];

			if (array_y[i] > maxy)
				maxy = array_y[i];
		}
		for (y = miny; y <= maxy; y++)
		{
			var polyInts = new Array();
			ints = 0;
			for (i = 0; i < n; i++)
			{
				if (!i)
				{
					ind1 = n-1;
					ind2 = 0;
				}
				else
				{
					ind1 = i-1;
					ind2 = i;
				}
				y1 = array_y[ind1];
				y2 = array_y[ind2];
				if (y1 < y2)
				{
					x1 = array_x[ind1];
					x2 = array_x[ind2];
				}
				else if (y1 > y2)
				{
					y2 = array_y[ind1];
					y1 = array_y[ind2];
					x2 = array_x[ind1];
					x1 = array_x[ind2];
				}
				else continue;

				 // modified 11. 2. 2004 Walter Zorn
				if ((y >= y1) && (y < y2))
					polyInts[ints++] = Math.round((y-y1) * (x2-x1) / (y2-y1) + x1);

				else if ((y == maxy) && (y > y1) && (y <= y2))
					polyInts[ints++] = Math.round((y-y1) * (x2-x1) / (y2-y1) + x1);
			}
			polyInts.sort(integer_compare);

			for (i = 0; i < ints; i+=2)
			{
				w = polyInts[i+1]-polyInts[i]
				this.mkDiv(polyInts[i], y, polyInts[i+1]-polyInts[i]+1, 1);
			}
		}
	};


	this.drawString = function(txt, x, y)
	{
		this.htm += '<div style="position:absolute;white-space:nowrap;'+
			'left:' + x + 'px;'+
			'top:' + y + 'px;'+
			'font-family:' +  this.ftFam + ';'+
			'font-size:' + this.ftSz + ';'+
			'color:' + this.color + ';' + this.ftSty + '">'+
			txt +
			'<\/div>';
	}


	this.drawImage = function(imgSrc, x, y, w, h)
	{
		this.htm += '<div style="position:absolute;'+
			'left:' + x + 'px;'+
			'top:' + y + 'px;'+
			'width:' +  w + ';'+
			'height:' + h + ';">'+
			'<img src="' + imgSrc + '" width="' + w + '" height="' + h + '">'+
			'<\/div>';
	}


	this.clear = function()
	{
		this.htm = "";
		if (this.cnv) this.cnv.innerHTML = this.defhtm;
	};


	this.mkOvQds = function(cx, cy, xl, xr, yt, yb, w, h)
	{
		this.mkDiv(xr+cx, yt+cy, w, h);
		this.mkDiv(xr+cx, yb+cy, w, h);
		this.mkDiv(xl+cx, yb+cy, w, h);
		this.mkDiv(xl+cx, yt+cy, w, h);
	};

	this.setStroke(1);
	this.setFont('verdana,geneva,helvetica,sans-serif', String.fromCharCode(0x31, 0x32, 0x70, 0x78), Font.PLAIN);
	this.color = '#000000';
	this.htm = '';
	this.wnd = wnd || window;

	if (!(jg_ie || jg_dom || jg_ihtm)) chkDHTM();
	if (typeof id != 'string' || !id) this.paint = pntDoc;
	else
	{
		this.cnv = document.all? (this.wnd.document.all[id] || null)
			: document.getElementById? (this.wnd.document.getElementById(id) || null)
			: null;
		this.defhtm = (this.cnv && this.cnv.innerHTML)? this.cnv.innerHTML : '';
		this.paint = jg_dom? pntCnvDom : jg_ie? pntCnvIe : jg_ihtm? pntCnvIhtm : pntCnv;
	}

	this.setPrintable(false);
}



function integer_compare(x,y)
{
	return (x < y) ? -1 : ((x > y)*1);
}

