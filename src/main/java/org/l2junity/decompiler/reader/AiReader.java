package org.l2junity.decompiler.reader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;

import org.l2junity.decompiler.ThreadPoolManager;
import org.l2junity.decompiler.controllers.MainController;
import org.l2junity.decompiler.dataholder.ComparisonMapper;
import org.l2junity.decompiler.dataholder.FunctionMapper;
import org.l2junity.decompiler.dataholder.HandlerMapper;
import org.l2junity.decompiler.dataholder.MapperDataHolder;

import javafx.application.Platform;

/**
 * AI file reader.
 * @author malyelfik
 */
public final class AiReader implements Runnable
{
	private final boolean _withoutTree;
	private final MainController _main;
	private final StringBuilder logBuffer = new StringBuilder();
	private static Set<String> _params = new HashSet<>();
	public static Map<String, String> _classTree = new HashMap<>();
	public static Map<String, Set<String>> _paramTree = new HashMap<>();
	
	public AiReader(MainController main, boolean withoutTree)
	{
		_main = main;
		_withoutTree = withoutTree;
	}
	
	@Override
	public void run()
	{
		// Disable buttons
		_main.disableButtons(true);
		
		// Make text area log buffer to send messages in batch to JavaFX TextArea
		final ScheduledFuture<?> logTask = ThreadPoolManager.getInstance().sheduleAtFixedRate(() ->
		{
			synchronized (logBuffer)
			{
				if (logBuffer.length() > 0)
				{
					final String buff = logBuffer.toString();
					logBuffer.setLength(0);
					
					Platform.runLater(() ->
					{
						_main.appendToLog(buff, false);
					});
				}
			}
		} , 200L, 200L);
		
		// Run decompilation task
		try
		{
			// Load required files for decompilation
			appendToLog("Loading files required for decompilation.");
			appendToLog("Loaded " + MapperDataHolder.getInstance().size() + " mappers data.");
			appendToLog("Loaded " + FunctionMapper.getInstance().size() + " function mappers.");
			appendToLog("Loaded " + ComparisonMapper.getInstance().size() + " comparison mappers.");
			appendToLog("Loaded " + HandlerMapper.getInstance().size() + " handler mappers.");
			// Start decompilation
			appendToLog("Decompilation started.");
			read();
			appendToLog("Decompilation finished.");
		}
		catch (Exception e)
		{
			appendToLog("Unable to decompile AI file.");
			e.printStackTrace();
		}
		
		// Stop log update task and append buffer content
		logTask.cancel(false);
		if (logBuffer.length() > 0)
		{
			Platform.runLater(() ->
			{
				_main.appendToLog(logBuffer.toString(), false);
			});
		}
		
		// Enable buttons
		_main.disableButtons(false);
	}
	
	public void appendToLog(String message)
	{
		synchronized (logBuffer)
		{
			logBuffer.append(message + "\n");
		}
	}
	
	private void read() throws Exception
	{
		BufferedWriter bw = null;
		String className = "null";
		String extend = "(null)";
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(_main.getAIPath()), "UTF-16LE")))
		{
			// Prepare
			if(_withoutTree) {
				final Path dir = Paths.get("decompiled");
				if (!Files.exists(dir) || !Files.isDirectory(dir)) {
					Files.createDirectory(dir);
					appendToLog("Creating decompiled directory.");
				}
			}
			else {
				final Path dir = Paths.get("decompiled_tree");
				if (!Files.exists(dir) || !Files.isDirectory(dir)) {
					Files.createDirectory(dir);
					appendToLog("Creating decompiled directory.");
				}
			}

			final Path dir = Paths.get("debug");
			if (!Files.exists(dir) || !Files.isDirectory(dir))
			{
				Files.createDirectory(dir);
			}
			Files.write(Paths.get("debug/debug.txt"), new byte[]{0});

			// Read
			String line;
			while ((line = reader.readLine()) != null)
			{
				if (line.startsWith("class "))
				{
					className = line.split("\\s")[2];
					extend = line.split("\\s")[4];
					if(!extend.equals("(null)"))
						_classTree.put(className, extend);
					final ClassReader cr = new ClassReader(this, readClass(reader, line));
					final StringBuilder sb = cr.read();
					_paramTree.put(className, cr.getParams());
					if (_withoutTree)
					{
						String fullPath = "decompiled/" + className + ".java";
						Files.write(Paths.get(fullPath), sb.toString().getBytes());
					}
					else
					{
						String fullPath = "decompiled_tree/" + getPath(className, 5) + className + ".java";
						Files.write(Paths.get(fullPath), sb.toString().getBytes());
					}
					/*String params = checkParam(className);
					if(!params.isEmpty())
						Files.write(Paths.get("debug/debug.txt"), params.getBytes(), StandardOpenOption.APPEND);*/
					sb.setLength(0);
					appendToLog("Class " + className + " has been parsed successfully.");
				}
			}
		}
		catch (Exception e){
			appendToLog("Error while decompile class:" + className);
			throw new Exception(e);
		}
		finally
		{
			if (bw != null)
			{
				bw.close();
			}
		}
	}
	
	private List<String> readClass(BufferedReader reader, String line) throws Exception
	{
		final List<String> lines = new ArrayList<>();
		lines.add(line);
		
		while ((line = reader.readLine()) != null)
		{
			lines.add(line.trim());
			if (line.startsWith("class_end"))
			{
				break;
			}
		}
		return lines;
	}

	private String getPath(String className, int depth) throws Exception{
		String path = "";
		String extend = className;
		while(true) {
			extend = _classTree.get(extend);
			if(extend == null)
				break;
			path = extend + "/" + path;
		}
		if(path.equals(""))
			return path;
		String[] spath = path.split("/");
		int size = Math.min(depth, spath.length);
		String[] subpath = new String[size];
		System.arraycopy(spath, 0, subpath, 0, size);
		String joined = String.join("/", subpath);

		final Path dir = Paths.get("decompiled_tree/" + joined);
		if (!Files.exists(dir) || !Files.isDirectory(dir))
		{
			Files.createDirectory(dir);
		}
		return joined + "/";
	}



	/*private String checkParam(String className) throws Exception{
		StringBuilder sb = new StringBuilder();
		Map<String, Set<String>> override = new HashMap<>();
		Set<String> params = _paramTree.get(className);
		String extend = className;
		boolean trigger = false;
		while(true) {
			Set<String> superParams = new HashSet<>();
			extend = _classTree.get(extend);
			if(extend == null)
				break;
			if(extend.startsWith("monster_parameter"))
				trigger = true;
			_paramTree.get(extend).stream().filter(params::contains).forEach(superParams::add);
			override.put(extend, superParams);
		}
		if(trigger) {
			for(String key : override.keySet()) {
				override.get(key).forEach(param -> sb.append(key).append(":").append(param).append("\n"));
			}
		}
		return sb.toString();
	}*/
}