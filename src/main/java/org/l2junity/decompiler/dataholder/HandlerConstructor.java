package org.l2junity.decompiler.dataholder;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.HashMap;
import java.util.Map;

public final class HandlerConstructor extends AbstractDataHolder
{
	private final Map<String, String> _data = new HashMap<>();

	public HandlerConstructor()
	{
		loadXml("data/handlerConstructor.xml");
	}
	
	@Override
	protected void load(Node n)
	{
		for (Node e = n.getFirstChild(); e != null; e = e.getNextSibling())
		{
			if (e.getNodeName().equals("item"))
			{
				final NamedNodeMap attrs = e.getAttributes();
				final String name = attrs.getNamedItem("name").getNodeValue();
				final String values = attrs.getNamedItem("value").getNodeValue();
				_data.put(name, values);
			}
		}
	}
	
	public String get(String key)
	{
		return (_data.containsKey(key)) ? _data.get(key) : key;
	}
	
	@Override
	public int size()
	{
		return _data.size();
	}
	
	public static HandlerConstructor getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		public static final HandlerConstructor INSTANCE = new HandlerConstructor();
	}
	
}
