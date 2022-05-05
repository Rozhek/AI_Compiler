package org.l2junity.decompiler.dataholder;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public final class ComparisonMapper extends AbstractDataHolder
{
	private final Map<String, String> _data = new HashMap<>();
	
	public ComparisonMapper()
	{
		loadXml("data/comparisonMapper.xml");
	}
	
	@Override
	protected void load(Node n)
	{
		for (Node e = n.getFirstChild(); e != null; e = e.getNextSibling())
		{
			if (e.getNodeName().equals("comparison"))
			{
				final NamedNodeMap attrs = e.getAttributes();
				final String name = attrs.getNamedItem("param").getNodeValue();
				final String values = attrs.getNamedItem("values").getNodeValue();
				_data.put(name, values);
			}
		}
	}
	
	public String get(String key, String val, Set<String> libs)
	{
		return (_data.containsKey(key)) ? MapperDataHolder.getInstance().get(_data.get(key), val, val, libs) : val;
	}
	
	public String get(String left, String right, String sign, Set<String> libs)
	{
		if (_data.containsKey(left))
		{
			right = MapperDataHolder.getInstance().get(_data.get(left), right, right, libs);
		}
		else if (_data.containsKey(right))
		{
			left = MapperDataHolder.getInstance().get(_data.get(right), left, left, libs);
		}
		return left + " " + sign + " " + right;
	}
	
	@Override
	public int size()
	{
		return _data.size();
	}
	
	public static ComparisonMapper getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		public static final ComparisonMapper INSTANCE = new ComparisonMapper();
	}
	
}
