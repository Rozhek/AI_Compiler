package org.l2junity.decompiler.dataholder;

import org.w3c.dom.Node;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class HandlerMapper extends AbstractDataHolder
{
	private final Map<String, String> _names = new HashMap<>();
	private final Map<String, Set<String>> _params = new HashMap<>();

	public HandlerMapper()
	{
		loadXml("data/handlerConvertor.xml");
	}
	
	@Override
	protected void load(Node n)
	{
		for (Node e = n.getFirstChild(); e != null; e = e.getNextSibling())
		{
			if (e.getNodeName().equals("handler"))
			{
				final String name = e.getAttributes().getNamedItem("name").getNodeValue();
				final String value = e.getAttributes().getNamedItem("value").getNodeValue();
				final Set<String> paramset = new LinkedHashSet<>();
				for (Node f = e.getFirstChild(); f != null; f = f.getNextSibling())
				{
					if (f.getNodeName().equals("param"))
					{
						final String param = f.getAttributes().getNamedItem("name").getNodeValue();
						paramset.add(param);
					}
				}
				_names.put(name, value);
				_params.put(name, paramset);
			}
		}
	}
	
	public String get(String key)
	{
		return (_names.containsKey(key)) ? _names.get(key) : key;
	}

	public Set<String> getParams(String key)
	{
		return _params.containsKey(key) ? _params.get(key) : Collections.emptySet();
	}
	
	@Override
	public int size()
	{
		return _names.size();
	}
	
	public static HandlerMapper getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		public static final HandlerMapper INSTANCE = new HandlerMapper();
	}
	
}
