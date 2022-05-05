package org.l2junity.decompiler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class ThreadPoolManager
{
	private final ExecutorService _generalTasks;
	private final ScheduledExecutorService _scheduledTasks;
	
	public ThreadPoolManager()
	{
		_generalTasks = Executors.newSingleThreadExecutor();
		_scheduledTasks = Executors.newSingleThreadScheduledExecutor();
	}
	
	public ScheduledFuture<?> sheduleAtFixedRate(Runnable command, long initialDelay, long period)
	{
		return _scheduledTasks.scheduleAtFixedRate(command, initialDelay, period, TimeUnit.MILLISECONDS);
	}
	
	public Future<?> execute(Runnable command)
	{
		return _generalTasks.submit(command);
	}
	
	public void shutdown()
	{
		_generalTasks.shutdownNow();
		_scheduledTasks.shutdownNow();
	}
	
	public static ThreadPoolManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected static class SingletonHolder
	{
		protected static final ThreadPoolManager _instance = new ThreadPoolManager();
	}
}
