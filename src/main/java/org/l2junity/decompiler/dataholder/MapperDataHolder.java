package org.l2junity.decompiler.dataholder;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public final class MapperDataHolder extends AbstractDataHolder
{
	private final Map<String, Map<String, String>> _data = new HashMap<>();
	
	public MapperDataHolder()
	{
		readDirectory("data/holders");
	}
	
	@Override
	protected void load(Node n)
	{
		for (Node e = n.getFirstChild(); e != null; e = e.getNextSibling())
		{
			if (e.getNodeName().equals("map"))
			{
				final String name = e.getAttributes().getNamedItem("name").getNodeValue();
				final Map<String, String> map = new HashMap<>();
				for (Node f = e.getFirstChild(); f != null; f = f.getNextSibling())
				{
					if (f.getNodeName().equals("item"))
					{
						final NamedNodeMap attrs = f.getAttributes();
						final String key = attrs.getNamedItem("key").getNodeValue();
						final String value = attrs.getNamedItem("value").getNodeValue();
						map.put(key, value);
					}
				}
				_data.put(name, map);
			}
		}
	}
	
	@Override
	public int size()
	{
		return _data.size();
	}
	
	public String get(String type, String key, Set<String> libs)
	{
		return get(type, key, null, libs);
	}
	
	public String get(String type, String key, String defaul, Set<String> libs)
	{
		String result = defaul;
		switch (type)
		{
			case "npcId":
			{
				try
				{
					result = String.valueOf(Integer.parseInt(key) - 1000000);
				}
				catch (Exception e)
				{
				}
				break;
			}
			case "skillData":
			{
				try
				{
					final int value = Integer.parseInt(key);
					final int skillId = value >> 16;
					final int skillLevel = value & 0xFFFF;
					if (value > 0 && skillLevel > 0 && skillLevel < 0xFFFF)
					{
						result = "@SkillInfo(id = " + skillId+", level = " + skillLevel + ")";
					}
				}
				catch (Exception e)
				{
				}
				break;
			}
			case "skillDataComment":
			{
				try
				{
					final int value = Integer.parseInt(key);
					final int skillId = value >> 16;
					final int skillLevel = value & 0xFFFF;
					if (value > 0 && skillLevel > 0 && skillLevel < 0xFFFF)
					{
						result = value + "/*@skill_" + skillId + "_" + skillLevel + "*/";
					}
				}
				catch (Exception e)
				{
				}
				break;
			}
			default:
			{
				if ((_data.containsKey(type) && _data.get(type).containsKey(key)))
				{
					result = _data.get(type).get(key);
				}
			}
		}
		if(type.equals("scriptEvent"))
			libs.add("ScriptEvent");
		else if(type.equals("moveSuperPoint"))
			libs.add("SuperPointRail");
		return result;
	}
	
	public static MapperDataHolder getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		public static final MapperDataHolder INSTANCE = new MapperDataHolder();
	}
}
