package org.l2junity.decompiler.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.l2junity.decompiler.dataholder.FunctionMapper;
import org.l2junity.decompiler.dataholder.MapperDataHolder;
import org.l2junity.decompiler.reader.AiReader;

public final class FunctionController
{
	private final String _name;
	private final List<String> _parameters = new ArrayList<>();
	private String _addValue;
	private String _caller = null;
	private Set<String> _libs;
	
	public FunctionController(String name, Set<String> libs)
	{
		_name = name;
		_libs = libs;
		_addValue = "";
	}
	
	public void setCaller(String caller)
	{
		_caller = caller;
	}
	
	public void addParameter(String param)
	{
		_parameters.add(param);
	}

	public void addValue(String val)
	{
		_addValue = val;
	}
	
	@Override
	public String toString()
	{
		// Reverse parameters
		Collections.reverse(_parameters);
		
		// Get mapper for function
		final Map<Integer, String> mapper = FunctionMapper.getInstance().getMapper(_name);
		
		// Start building function
		String func = _name + "(";
		if (_caller != null)
		{
			func = _caller + "." + func;
		}
		
		// Append parameters
		for (int i = 0; i < _parameters.size(); i++)
		{
			final int paramIndex = i + 1;
			String param = _parameters.get(i);
			if (mapper.containsKey(paramIndex))
			{
				String type = mapper.get(paramIndex);
				if(type.equals("skillData"))
					type = "skillDataComment";
				param = MapperDataHolder.getInstance().get(type, param, param, _libs);
			}
			
			if (i != 0)
			{
				func += ", ";
			}
			func += param;
		}
		func += ")";
		func += _addValue;
		return func;
	}

	public String getFunc()
	{
		// Reverse parameters
		//Collections.reverse(_parameters);

		// Get mapper for function
		final Map<Integer, String> mapper = FunctionMapper.getInstance().getMapper(_name);

		// Start building function
		String func = _name + "(";
		if (_caller != null)
		{
			func = _caller + "." + func;
		}
		func += ")";
		return func;
	}
}
