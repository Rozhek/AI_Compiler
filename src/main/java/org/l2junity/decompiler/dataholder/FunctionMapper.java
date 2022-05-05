package org.l2junity.decompiler.dataholder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public final class FunctionMapper extends AbstractDataHolder
{
	private final Map<String, Map<Integer, String>> _data = new HashMap<>();
	
	public FunctionMapper()
	{
		loadXml("data/functionMapper.xml");
	}
	
	@Override
	protected void load(Node n)
	{
		for (Node e = n.getFirstChild(); e != null; e = e.getNextSibling())
		{
			if (e.getNodeName().equals("function"))
			{
				final String name = e.getAttributes().getNamedItem("name").getNodeValue();
				final Map<Integer, String> map = new HashMap<>();
				for (Node f = e.getFirstChild(); f != null; f = f.getNextSibling())
				{
					if (f.getNodeName().equals("param"))
					{
						final NamedNodeMap attrs = f.getAttributes();
						final int id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
						final String values = attrs.getNamedItem("values").getNodeValue();
						map.put(id, values);
					}
				}
				_data.put(name, map);
			}
		}
	}
	
	public Map<Integer, String> getMapper(String funcName)
	{
		return _data.getOrDefault(funcName, Collections.emptyMap());
	}
	
	@Override
	public int size()
	{
		return _data.size();
	}
	
	public static FunctionMapper getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		public static final FunctionMapper INSTANCE = new FunctionMapper();
	}
}
