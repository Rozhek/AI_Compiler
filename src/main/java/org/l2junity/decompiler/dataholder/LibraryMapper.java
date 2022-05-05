package org.l2junity.decompiler.dataholder;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.HashMap;
import java.util.Map;

public final class LibraryMapper extends AbstractDataHolder
{
	private final Map<String, String> _data = new HashMap<>();

	public LibraryMapper()
	{
		loadXml("data/libraryMapper.xml");
	}
	
	@Override
	protected void load(Node n)
	{
		for (Node e = n.getFirstChild(); e != null; e = e.getNextSibling())
		{
			if (e.getNodeName().equals("libpath"))
			{
				final NamedNodeMap attrs = e.getAttributes();
				final String name = attrs.getNamedItem("name").getNodeValue();
				final String value = attrs.getNamedItem("path").getNodeValue();
				_data.put(name, value);
			}
		}
	}
	
	public String get(String key)
	{
		return _data.get(key);
	}
	
	@Override
	public int size()
	{
		return _data.size();
	}
	
	public static LibraryMapper getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		public static final LibraryMapper INSTANCE = new LibraryMapper();
	}
	
}
