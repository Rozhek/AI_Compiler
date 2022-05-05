package org.l2junity.decompiler.dataholder;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

public abstract class AbstractDataHolder
{
	protected final void loadXml(String path)
	{
		loadXml(new File(path));
	}
	
	protected final void loadXml(File file)
	{
		try
		{
			if (file.exists())
			{
				final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				final DocumentBuilder db = dbf.newDocumentBuilder();
				final Document doc = db.parse(file);
				
				for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
				{
					if (n.getNodeName().equals("list"))
					{
						load(n);
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	protected final void readDirectory(String dir)
	{
		readDirectory(new File(dir));
	}
	
	protected final void readDirectory(File dir)
	{
		try
		{
			if (!dir.exists())
			{
				return;
			}
			
			final File[] files = dir.listFiles();
			for (File f : files)
			{
				if (f.isDirectory())
				{
					readDirectory(f);
				}
				else if (f.getName().endsWith(".xml"))
				{
					loadXml(f);
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	protected abstract void load(Node n);
	
	public abstract int size();
}
