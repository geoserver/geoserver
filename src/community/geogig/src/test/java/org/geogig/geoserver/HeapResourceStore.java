package org.geogig.geoserver;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceListener;
import org.geoserver.platform.resource.ResourceNotificationDispatcher;
import org.geoserver.platform.resource.ResourceStore;

import com.google.common.collect.Lists;

import org.geoserver.platform.resource.Resource.Type;

public class HeapResourceStore implements ResourceStore {
	
	protected Map<String, HeapResource> resources;
	
	HeapResource root;
	
	public HeapResourceStore() {
		this.resources = new HashMap<String, HeapResource>();
		root = new HeapResource(this);
	}

	@Override
	public Resource get(String path) {
		return root.get(path);
	}

	@Override
	public boolean remove(String path) {
		HeapResource resource = (HeapResource) get(path);
		if (!resource.getType().equals(Type.UNDEFINED)) {
			return resource.delete();
		}
		
		return false;
	}

	@Override
	public boolean move(String path, String target) {
		HeapResource resource = (HeapResource) get(path);
		if (!resource.getType().equals(Type.UNDEFINED)) {
			return resource.renameTo(get(target));
		}
		return false;
	}

	@Override
	public ResourceNotificationDispatcher getResourceNotificationDispatcher() {
		throw new UnsupportedOperationException();
	}
	
	class HeapResource implements Resource {
		
		private String name;
		
		private String path;
		
		private HeapResource parent;
		
		private List<HeapResource> children;
		
		private Type type;
		
		private ByteArrayOutputStream bytes = null;
		
		final private HeapResourceStore store;
		
		public HeapResource(HeapResourceStore store) {
			this(store, null, null);
		}
		
		public HeapResource(HeapResourceStore store, String name, HeapResource parent) {
			this.store = store;
			this.name = name;
			if (name == null) {
				this.type = Type.DIRECTORY;
				this.parent = null;
				this.path = null;
			} else {
				this.type = Type.UNDEFINED;
				this.parent = parent;
				this.path = buildPath();
			}
			this.children = new LinkedList<HeapResource>();
		}
		
		private String buildPath() {
			List<String> pathNames = new LinkedList<String>();
			pathNames.add(name);
			HeapResource currParent = parent;
			while(currParent.name != null) {
				pathNames.add(currParent.name);
				currParent = currParent.parent;
			}
			String[] paths = Lists.reverse(pathNames).toArray(new String[0]);
			return Paths.path(paths);
		}

		@Override
		public String path() {
			return path;
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public Lock lock() {
			return null;
		}

		@Override
		public void addListener(ResourceListener listener) {
			
		}

		@Override
		public void removeListener(ResourceListener listener) {
			
		}

		@Override
		public InputStream in() {
			if (getType().equals(Type.UNDEFINED) || getType().equals(Type.DIRECTORY)) {
				throw new UnsupportedOperationException();
			}
			return new ByteArrayInputStream(bytes.toByteArray());
		}

		@Override
		public OutputStream out() {
			if (bytes == null && getType().equals(Type.UNDEFINED)) {
				this.type = Type.RESOURCE;
			}
			if (getType().equals(Type.RESOURCE)) {
				bytes = new ByteArrayOutputStream();
			}
			return bytes;
		}

		@Override
		public File file() {
			throw new UnsupportedOperationException();
		}

		@Override
		public File dir() {
			throw new UnsupportedOperationException();
		}

		@Override
		public long lastmodified() {
			return 0;
		}

		@Override
		public Resource parent() {
			return parent;
		}

		@Override
		public Resource get(String resourcePath) {
			String[] pathNames = resourcePath.split("/");
			int pathIndex = 0;
			HeapResource resource = this;
			while(pathIndex < pathNames.length) {
				if (resource.getType().equals(Type.UNDEFINED)) {
					resource.type = Type.DIRECTORY;
				}
				boolean found = false;
				String pathName = pathNames[pathIndex];
				for (HeapResource child : resource.children) {
					if (child.name.equals(pathName)) {
						resource = child;
						found = true;
						break;
					}
				}
				if (!found) {
					HeapResource newResource = new HeapResource(store, pathName, resource);
					resource.children.add(newResource);
					resource = newResource;
				}
				pathIndex++;
			}
			return resource;
			
		}

		@Override
		public List<Resource> list() {
			List<Resource> resources = Lists.newArrayList(children);
			return resources;
		}

		@Override
		public Type getType() {
			return type;
		}

		@Override
		public boolean delete() {
			boolean deleted = false;
			if (!getType().equals(Type.UNDEFINED)) {
				type = Type.UNDEFINED;
				children.clear();
				bytes = null;
				deleted = true;
			}
			return deleted;
		}

		@Override
		public boolean renameTo(Resource dest) {
			if (dest == this) {
				return false;
			}
			this.path = dest.path();
			this.name = dest.name();
			this.parent = (HeapResource) dest.parent();
			this.parent.children.remove(dest);
			this.parent.children.add(this);
			
			return true;
		}
		
	}

}
