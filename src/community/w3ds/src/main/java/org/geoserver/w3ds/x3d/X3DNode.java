/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Jorge Gustavo Rocha / Universidade do Minho
 * @author Nuno Carvalho Oliveira / Universidade do Minho 
 */

package org.geoserver.w3ds.x3d;

import java.util.ArrayList;

public class X3DNode {
	private String tag;
	private ArrayList<X3DAttribute> attributes;
	private ArrayList<X3DNode> nodes;
	private String text;
	private boolean textX3DNode;
	private boolean expand;
	private boolean valid;

	public X3DNode(String tag) {
		this.tag = tag;
		this.attributes = new ArrayList<X3DAttribute>();
		this.nodes = new ArrayList<X3DNode>();
		this.text = "";
		this.textX3DNode = false;
		this.expand = false;
		this.valid = true;
	}

	public X3DNode() {
		this.tag = "";
		this.attributes = new ArrayList<X3DAttribute>();
		this.nodes = new ArrayList<X3DNode>();
		this.text = "";
		this.textX3DNode = true;
		this.expand = false;
		this.valid = true;
	}

	public X3DNode(X3DNode x3dNode) {
		this.tag = x3dNode.getTag();
		this.attributes = new ArrayList<X3DAttribute>();
		for (X3DAttribute a : x3dNode.getAttributes()) {
			this.attributes.add(a.clone());
		}
		this.nodes = new ArrayList<X3DNode>();
		for (X3DNode n : x3dNode.getNodes()) {
			this.nodes.add(n.clone());
		}
		this.text = x3dNode.getText();
		this.textX3DNode = x3dNode.isTextX3DNode();
		this.expand = x3dNode.isExpand();
		this.valid = x3dNode.isValid();
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public ArrayList<X3DAttribute> getAttributes() {
		return attributes;
	}

	public void setAttributes(ArrayList<X3DAttribute> attributes) {
		this.attributes = attributes;
	}

	public ArrayList<X3DNode> getNodes() {
		return nodes;
	}

	public void setNodes(ArrayList<X3DNode> nodes) {
		this.nodes = nodes;
	}

	public boolean isTextX3DNode() {
		return textX3DNode;
	}

	public void setTextX3DNode(boolean textX3DNode) {
		this.textX3DNode = textX3DNode;
	}

	public String getText() {
		return text;
	}

	public boolean isExpand() {
		return expand;
	}

	public boolean isValid() {
		return this.valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public void addX3DAttribute(X3DAttribute attribute) {
		attributes.add(attribute);
	}

	public void addX3DNode(X3DNode node) {
		nodes.add(node);
	}

	public void setText(String text) {
		this.text = text;
	}

	public void setTextX3DNode(Boolean b) {
		this.textX3DNode = b;
	}

	public void setExpand(boolean b) {
		this.expand = b;
	}

	@Override
	public String toString() {
		if (!this.valid) {
			return "";
		}
		if (this.textX3DNode) {
			return this.text;
		}
		StringBuilder strb = new StringBuilder();
		strb.append("<" + tag);
		for (X3DAttribute a : attributes) {
			strb.append(" " + a.toString());
		}
		if (nodes.isEmpty() && text.isEmpty() && !expand) {
			strb.append("/>");
		} else {
			strb.append(">");
			if (!text.isEmpty()) {
				strb.append(text);
			}
			for (X3DNode n : nodes) {
				strb.append(n.toString());
			}
			strb.append("</" + tag + ">");
		}
		return strb.toString();
	}

	public String toStringSpaces(String tabs) {
		if (!this.valid) {
			return "";
		}
		if (this.textX3DNode) {
			return this.text;
		}
		StringBuilder strb = new StringBuilder();
		strb.append(tabs + "<" + tag);
		for (X3DAttribute a : attributes) {
			strb.append(" " + a.toString());
		}
		if (nodes.isEmpty() && text.isEmpty() && !expand) {
			strb.append("/>\n");
		} else {
			strb.append(">\n");
			if (!text.isEmpty()) {
				strb.append(text);
			}
			for (X3DNode n : nodes) {
				strb.append(n.toStringSpaces(tabs + "\t"));
			}
			strb.append(tabs + "</" + tag + ">\n");
		}
		return strb.toString();
	}

	public void addX3DAttribute(String name, String value) {
		this.addX3DAttribute(new X3DAttribute(name, value));
	}

	public X3DNode clone() {
		return new X3DNode(this);
	}
	
	public boolean haveChilds() {
		return !nodes.isEmpty();
	}

	public boolean haveChild(String tag) {
		for (X3DNode n : this.nodes) {
			if (n.getTag().equalsIgnoreCase(tag)) {
				return true;
			}
			if (n.haveChild(tag)) {
				return true;
			}
		}
		return false;
	}
}
