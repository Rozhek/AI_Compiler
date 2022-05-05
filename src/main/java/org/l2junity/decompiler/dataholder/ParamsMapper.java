package org.l2junity.decompiler.dataholder;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class ParamsMapper extends AbstractDataHolder
{
	private final Map<String, Set<String>> _data = new HashMap<>();

	public ParamsMapper()
	{
		loadXml("data/paramMapper.xml");
	}
	
	@Override
	protected void load(Node n)
	{
		for (Node e = n.getFirstChild(); e != null; e = e.getNextSibling())
		{
			if (e.getNodeName().equals("param"))
			{
				final String name = e.getAttributes().getNamedItem("name").getNodeValue();
				final Set<String> set = new HashSet<>();
				for (Node f = e.getFirstChild(); f != null; f = f.getNextSibling())
				{
					if (f.getNodeName().equals("item"))
					{
						final NamedNodeMap attrs = f.getAttributes();
						final String value = attrs.getNamedItem("value").getNodeValue();
						set.add(value);
					}
				}
				_data.put(name, set);
			}
		}
	}
	
	public Set<String> getSet(String param)
	{
		return _data.getOrDefault(param, Collections.emptySet());
	}
	public boolean hasHolderAndItsNot(String param, String ai_class)
	{
		return _data.get(param) != null && !_data.get(param).contains(ai_class);
	}
	
	@Override
	public int size()
	{
		return _data.size();
	}
	
	public static ParamsMapper getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		public static final ParamsMapper INSTANCE = new ParamsMapper();
	}
}
