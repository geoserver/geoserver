/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Jorge Gustavo Rocha / Universidade do Minho
 * @author Nuno Carvalho Oliveira / Universidade do Minho 
 */

package org.geoserver.w3ds.x3d;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;

public class X3DXmlWriter {

	private OutputStream output;
	private String version;
	private BufferedWriter writer;
	private Node origin;
	private Attribute geoSystem;

	private String DEF_POINTS = "POINTS";

	public X3DXmlWriter(OutputStream output, double[] origin) {
		this.output = output;
		geoSystem = new Attribute("geoSystem",
				"&quot;GD&quot; &quot;WE&quot; &quot;longitude_first&quot;");
		this.origin = new Node("GeoOrigin");
		this.origin.addAttribute(this.geoSystem);
		this.origin.addAttribute(new Attribute("DEF", "ORIGIN"));
		this.origin.addAttribute(new Attribute("geoCoords", String
				.valueOf(origin[0])
				+ " "
				+ String.valueOf(origin[1])
				+ " "
				+ String.valueOf(origin[2])));
	}

	public void init() throws IOException {
		writer = new BufferedWriter(new OutputStreamWriter(output));
	}

	public void close() throws IOException {
		writer.flush();
		writer.close();
		writer = null;
	}

	public void write(Node node) throws IOException {
		writer.write(node.toStringSpaces(""));
	}

	public void writeHTMLX3D(Node html) throws IOException {
		writer.write("<!DOCTYPE html>" + html.toString());
	}

	public void writeComent(String text) throws IOException {
		writer.write("<!-- " + text + " -->");
	}

	public void notSupported(String text) throws IOException {
		this.writeComent("NOT SUPPORTED: " + text);
	}

	public Node createX3DHeader() {
		Node x3d = new Node("x3d");
		x3d.addAttribute(new Attribute("id", "element"));
		x3d.addAttribute(new Attribute("width", "1200px"));
		x3d.addAttribute(new Attribute("height", "900px"));
		return x3d;
	}

	public Node finalHTMLX3D(Node x3d) {
		Node html = new Node("html");
		Node doc = new Node();
		doc.setText("<!DOCTYPE html>");
		html.addNode(doc);
		Node head = new Node("head");
		Node meta = new Node("meta");
		meta.addAttribute(new Attribute("http-equiv", "Content-Type"));
		meta.addAttribute(new Attribute("content", "text/html; charset=utf-8"));
		Node title = new Node("title");
		title.setText("Modelo X3D");
		Node link = new Node("link");
		link.addAttribute(new Attribute("rel", "stylesheet"));
		link.addAttribute(new Attribute("href",
				"http://www.x3dom.org/x3dom/release/x3dom.css"));
		Node script = new Node("script");
		script.setExpand(true);
		script.addAttribute(new Attribute("type", "text/javascript"));
		script.addAttribute(new Attribute("src",
				"http://www.x3dom.org/x3dom/release/x3dom.js"));
		head.addNode(meta);
		head.addNode(title);
		head.addNode(link);
		head.addNode(script);
		html.addNode(head);
		Node body = new Node("body");
		body.addAttribute(new Attribute("onload", "init()"));
		Node p = new Node("p");
		Node l1 = new Node("a");
		l1.addAttribute(new Attribute("href", "#"));
		l1.addAttribute(new Attribute("onClick",
				"$element.runtime.showAll();return false;"));
		l1.setText("Mostrar Tudo");
		Node l2 = new Node("a");
		l2.addAttribute(new Attribute("href", "#"));
		l2.addAttribute(new Attribute("onClick",
				"$element.runtime.examine();return false;"));
		l2.setText("Examinar");
		Node l3 = new Node("a");
		l3.addAttribute(new Attribute("href", "#"));
		l3.addAttribute(new Attribute("onClick",
				"$element.runtime.lookAt();return false;"));
		l3.setText("LookAt");
		p.addNode(l1);
		p.addNode(l2);
		p.addNode(l3);
		body.addNode(p);
		body.addNode(x3d);
		Node init = new Node();
		init.setText("<script type=\"text/javascript\">function init() {$element = document.getElementById('element');}</script>");
		body.addNode(init);
		html.addNode(body);
		return html;
	}

	public Node createScene() {
		Node scene = new Node("Scene");
		return scene;
	}

	public Node createIndexedFaceSet(Coordinate[] coordinates) {

		Node shape = new Node("Shape");

		Node appearance = new Node("Appearance");
		Node material = new Node("Material");
		material.addAttribute(new Attribute("emissiveColor", "0 0 0"));
		appearance.addNode(material);
		shape.addNode(appearance);

		Node indexedFaceSet = new Node("IndexedFaceSet");
		Node coordinate = new Node("GeoCoordinate");

		String coords = "";
		String index = "";
		int i = 0;
		int size = coordinates.length;
		for (i = 0; i < size; i++) {
			index += String.valueOf(i) + " ";
		}
		index += "-1";
		for (i = 0; i < size - 1; i++) {
			coords += String.valueOf(coordinates[i].x) + " ";
			coords += String.valueOf(coordinates[i].y) + " ";
			if (coordinates[i].z == Double.NaN) {
				coords += String.valueOf(0) + " ";
			} else {
				coords += String.valueOf(coordinates[i].z) + " ";
			}
		}
		coords += String.valueOf(coordinates[i].x) + " ";
		coords += String.valueOf(coordinates[i].y) + " ";
		if (coordinates[i].z == Double.NaN) {
			coords += String.valueOf(0) + " ";
		} else {
			coords += String.valueOf(coordinates[i].z);
		}

		coordinate.addAttribute(new Attribute("point", coords));
		coordinate.addAttribute(this.geoSystem);
		coordinate.addNode(this.origin);
		indexedFaceSet.addNode(coordinate);
		indexedFaceSet.addAttribute(new Attribute("coordIndex", index));
		indexedFaceSet.addAttribute(new Attribute("solid", "false"));
		indexedFaceSet.addAttribute(new Attribute("convex", "false"));
		shape.addNode(indexedFaceSet);

		return shape;
	}

	public Node createPolygon(List<Integer> indexis,
			List<Coordinate> coordinates) {

		Node shape = new Node("Shape");

		Node appearance = new Node("Appearance");
		Node material = new Node("Material");
		material.addAttribute(new Attribute("emissiveColor", "0 0 0"));
		appearance.addNode(material);
		shape.addNode(appearance);

		Node indexedFaceSet = new Node("IndexedFaceSet");
		Node geoCoordinates = new Node("GeoCoordinate");

		StringBuilder coordinates_txt = new StringBuilder();
		StringBuilder indexis_txt = new StringBuilder();

		for (Integer index : indexis) {
			indexis_txt.append(String.valueOf(index) + " ");
		}

		for (Coordinate coordinate : coordinates) {
			coordinates_txt.append(String.valueOf(coordinate.x) + " ");
			coordinates_txt.append(String.valueOf(coordinate.y) + " ");
			coordinates_txt.append(String.valueOf(coordinate.z) + " ");
		}

		geoCoordinates.addAttribute(new Attribute("point", coordinates_txt
				.toString()));
		geoCoordinates.addAttribute(this.geoSystem);
		geoCoordinates.addNode(this.origin);
		indexedFaceSet.addNode(geoCoordinates);
		indexedFaceSet.addAttribute(new Attribute("coordIndex", indexis_txt
				.toString()));
		indexedFaceSet.addAttribute(new Attribute("solid", "false"));
		indexedFaceSet.addAttribute(new Attribute("convex", "false"));
		shape.addNode(indexedFaceSet);

		return shape;
	}

	public Node createIndexedLineSet(CoordinateSequence coordinates) {

		Node shape = new Node("Shape");

		Node appearance = new Node("Appearance");
		Node material = new Node("Material");
		material.addAttribute(new Attribute("emissiveColor", "0 0 0"));
		appearance.addNode(material);
		shape.addNode(appearance);

		Node indexedLineSet = new Node("IndexedLineSet");
		Node coordinate = new Node("GeoCoordinate");

		String coords = "";
		String index = "";
		int i = 0;
		int size = coordinates.size();
		int d = coordinates.getDimension();
		for (i = 0; i < size; i++) {
			index += String.valueOf(i) + " ";
		}
		index += "-1";
		for (i = 0; i < size - 1; i++) {
			coords += String.valueOf(coordinates.getOrdinate(i, 0)) + " ";
			coords += String.valueOf(coordinates.getOrdinate(i, 1)) + " ";
			if (d < 3) {
				coords += String.valueOf(0) + " ";
			} else {
				coords += String.valueOf(coordinates.getOrdinate(i, 2)) + " ";
			}
		}
		coords += String.valueOf(coordinates.getOrdinate(i, 0)) + " ";
		coords += String.valueOf(coordinates.getOrdinate(i, 1)) + " ";
		if (d < 3) {
			coords += String.valueOf(0);
		} else {
			coords += String.valueOf(coordinates.getOrdinate(i, 2));
		}

		coordinate.addAttribute(new Attribute("point", coords));
		coordinate.addAttribute(this.geoSystem);
		coordinate.addNode(this.origin);
		indexedLineSet.addNode(coordinate);
		indexedLineSet.addAttribute(new Attribute("coordIndex", index));
		shape.addNode(indexedLineSet);

		return shape;
	}

	public Node createIndexedLineSetIndexis(String indexis) {

		Node shape = new Node("Shape");

		Node appearance = new Node("Appearance");
		Node material = new Node("Material");
		material.addAttribute(new Attribute("emissiveColor", "0 0 0"));
		appearance.addNode(material);
		shape.addNode(appearance);

		Node indexedLineSet = new Node("IndexedLineSet");
		Node coordinate = new Node("GeoCoordinate");

		coordinate.addAttribute(new Attribute("USE", this.DEF_POINTS));
		indexedLineSet.addNode(coordinate);
		indexedLineSet.addAttribute(new Attribute("coordIndex", indexis));
		shape.addNode(indexedLineSet);

		return shape;
	}

	public Node createIndexedLineSetIndexisPoints(String indexis, String points) {

		Node shape = new Node("Shape");

		Node appearance = new Node("Appearance");
		Node material = new Node("Material");
		material.addAttribute(new Attribute("emissiveColor", "0 0 0"));
		appearance.addNode(material);
		shape.addNode(appearance);

		Node indexedLineSet = new Node("IndexedLineSet");
		Node coordinate = new Node("GeoCoordinate");

		coordinate.addAttribute(new Attribute("point", points));
		coordinate.addAttribute(this.geoSystem);
		coordinate.addNode(this.origin);
		indexedLineSet.addNode(coordinate);
		indexedLineSet.addAttribute(new Attribute("coordIndex", indexis));
		shape.addNode(indexedLineSet);

		return shape;
	}

	public Node createIndexedLineSetIndexisPointsStyle(String indexis,
			String points, String style) {

		Node shape = new Node("Shape");

		Node appearance = new Node("Appearance");
		appearance.addAttribute(new Attribute("USE", style));
		appearance.setExpand(true);
		shape.addNode(appearance);

		Node indexedLineSet = new Node("IndexedLineSet");
		Node coordinate = new Node("GeoCoordinate");

		coordinate.addAttribute(new Attribute("point", points));
		coordinate.addAttribute(this.geoSystem);
		coordinate.addNode(this.origin);
		indexedLineSet.addNode(coordinate);
		indexedLineSet.addAttribute(new Attribute("coordIndex", indexis));
		shape.addNode(indexedLineSet);

		return shape;
	}

	public Node orcreateIndexedFaceSetIndexisPointsStyle(String indexis,
			String points, String style) {

		Node shape = new Node("Shape");

		Node appearance = new Node("Appearance");
		appearance.addAttribute(new Attribute("USE", style));
		appearance.setExpand(true);
		shape.addNode(appearance);

		Node indexedFaceSet = new Node("IndexedFaceSet");
		Node coordinate = new Node("GeoCoordinate");

		coordinate.addAttribute(new Attribute("point", points));
		coordinate.addAttribute(this.geoSystem);
		coordinate.addNode(this.origin);
		indexedFaceSet.addNode(coordinate);
		indexedFaceSet.addAttribute(new Attribute("coordIndex", indexis));
		indexedFaceSet.addAttribute(new Attribute("solid", "false"));
		indexedFaceSet.addAttribute(new Attribute("convex", "false"));
		shape.addNode(indexedFaceSet);

		return shape;
	}
	
	public Node fixe_createIndexedFaceSetIndexisPointsStyle(String indexis,
			String points, String style) {

		Node shape = new Node("Shape");

		Node appearance = new Node("Appearance");
		appearance.addAttribute(new Attribute("USE", style));
		appearance.setExpand(true);
		shape.addNode(appearance);

		Node indexedFaceSet = new Node("IndexedFaceSet");
		Node coordinate = new Node("Coordinate");

		coordinate.addAttribute(new Attribute("point", points));
		indexedFaceSet.addNode(coordinate);
		indexedFaceSet.addAttribute(new Attribute("coordIndex", indexis));
		indexedFaceSet.addAttribute(new Attribute("solid", "false"));
		indexedFaceSet.addAttribute(new Attribute("convex", "false"));
		shape.addNode(indexedFaceSet);

		return shape;
	}

	public Node createIndexedFaceSetIndexisPointsStyle(String indexis,
			String points, String style) {

		Node shape = new Node("Shape");

		Node appearance = new Node("Appearance");
		appearance.addAttribute(new Attribute("USE", style));
		appearance.setExpand(true);
		shape.addNode(appearance);

		Node indexedFaceSet = new Node("IndexedFaceSet");
		Node coordinate = new Node("Coordinate");

		coordinate.addAttribute(new Attribute("point", points));
		indexedFaceSet.addNode(coordinate);
		indexedFaceSet.addAttribute(new Attribute("coordIndex", indexis));
		indexedFaceSet.addAttribute(new Attribute("solid", "false"));
		indexedFaceSet.addAttribute(new Attribute("convex", "true"));
		shape.addNode(indexedFaceSet);

		return shape;
	}

	public Node createMaterialEmissiveColorDEF(String color, String def) {
		Node appearance = new Node("Appearance");
		appearance.addAttribute(new Attribute("DEF", def));
		Node material = new Node("Material");
		material.addAttribute(new Attribute("emissiveColor", color));
		appearance.addNode(material);
		return appearance;
	}

	public Node createMaterialDefault() {
		Node appearance = new Node("Appearance");
		appearance.addAttribute(new Attribute("DEF", "default"));
		Node material = new Node("Material");
		material.addAttribute(new Attribute("emissiveColor", "0 0 0"));
		appearance.addNode(material);
		return appearance;
	}

	public Node createDEFPOINT(String points) {
		Node shape = new Node("Shape");

		Node indexedLineSet = new Node("IndexedLineSet");
		Node coordinate = new Node("GeoCoordinate");

		coordinate.addAttribute(new Attribute("DEF", this.DEF_POINTS));
		coordinate.addAttribute(new Attribute("point", points));
		coordinate.addAttribute(this.geoSystem);
		coordinate.addNode(this.origin);
		indexedLineSet.addNode(coordinate);
		indexedLineSet.addAttribute(new Attribute("coordIndex", "-1"));
		shape.addNode(indexedLineSet);

		return shape;
	}
	
	public Attribute getJscrEvent(String event, String action) {
		return new Attribute(event, action);
	}
	
	public Attribute getOnclickAlert(String text) {
		return getJscrEvent("onhover", "alert(" + text + ");");
	}

	public class Attribute {

		private String attribute;
		private String value;

		public String getAttribute() {
			return attribute;
		}

		public Attribute(String attribute, String value) {
			this.attribute = attribute;
			this.value = value;
		}

		public void setAttribute(String attribute) {
			this.attribute = attribute;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return attribute + "=\"" + value + "\"";
		}
	}

	public class Node {

		private String tag;
		private ArrayList<Attribute> attributes;
		private ArrayList<Node> nodes;
		private String text;
		private boolean textNode;
		private boolean expand;

		public Node(String tag) {
			this.tag = tag;
			this.attributes = new ArrayList<Attribute>();
			this.nodes = new ArrayList<Node>();
			this.text = "";
			this.textNode = false;
			this.expand = false;
		}

		public Node() {
			this.tag = "";
			this.attributes = new ArrayList<Attribute>();
			this.nodes = new ArrayList<Node>();
			this.text = "";
			this.textNode = true;
		}

		public void addAttribute(Attribute attribute) {
			attributes.add(attribute);
		}

		public void addNode(Node node) {
			nodes.add(node);
		}

		public void setText(String text) {
			this.text = text;
		}

		public void setTextNode(Boolean b) {
			this.textNode = b;
		}

		public void setExpand(boolean b) {
			this.expand = b;
		}

		@Override
		public String toString() {
			if (this.textNode) {
				return this.text;
			}
			StringBuilder strb = new StringBuilder();
			strb.append("<" + tag);
			for (Attribute a : attributes) {
				strb.append(" " + a.toString());
			}
			if (nodes.isEmpty() && text.isEmpty() && !expand) {
				strb.append("/>");
			} else {
				strb.append(">");
				if (!text.isEmpty()) {
					strb.append(text);
				}
				for (Node n : nodes) {
					strb.append(n.toString());
				}
				strb.append("</" + tag + ">");
			}
			return strb.toString();
		}

		public String toStringSpaces(String tabs) {
			if (this.textNode) {
				return this.text;
			}
			StringBuilder strb = new StringBuilder();
			strb.append(tabs + "<" + tag);
			for (Attribute a : attributes) {
				strb.append(" " + a.toString());
			}
			if (nodes.isEmpty() && text.isEmpty() && !expand) {
				strb.append("/>\n");
			} else {
				strb.append(">\n");
				if (!text.isEmpty()) {
					strb.append(text);
				}
				for (Node n : nodes) {
					strb.append(n.toStringSpaces(tabs + "\t"));
				}
				strb.append(tabs + "</" + tag + ">\n");
			}
			return strb.toString();
		}

	}

}
