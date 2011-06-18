package org.vfny.geoserver.wms.responses.map.htmlimagemap.utils;

import java.util.LinkedList;

/**
 * Cyclical implementation of LinkedList: when a not
 * available index is used, then index is wrapped until
 * it falls in tre available index range.
 * 
 * @author m.bartolomeoli
 *
 */
public class IndexableCyclicalLinkedList extends LinkedList {
	
	private static final long serialVersionUID = 6239225551852896282L;

	
	public Object get(int index)
	{		
			//perform the index wrapping
			while (index < 0)
				index = size() + index;
			if (index >=  size())
				index %=  size();
			return super.get(index);		
	}
}
