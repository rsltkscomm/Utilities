package patterns.command;

import reporting.TestLogManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Command invoker that executes commands and manages command history.
 * This implements the Command pattern for UI actions.
 */
public class CommandInvoker
{
	
	private final List<Command> commandHistory;
	private final boolean retryEnabled;
	private final int maxRetries;
	private final long retryDelayMs;
	private volatile String lastErrorMessage;
	private volatile Throwable lastException;
	
	public CommandInvoker()
	{
		this(false, 0, 0L);
	}
	
	public CommandInvoker(boolean retryEnabled, int maxRetries)
	{
		this(retryEnabled, maxRetries, 0L);
	}
	
	public CommandInvoker(boolean retryEnabled, int maxRetries, long retryDelayMs)
	{
		this.commandHistory = new ArrayList<>();
		this.retryEnabled = retryEnabled;
		this.maxRetries = maxRetries;
		this.retryDelayMs = Math.max(0L, retryDelayMs);
		this.lastErrorMessage = null;
		this.lastException = null;
		
		TestLogManager.info("CommandInvoker initialized with retry: " + retryEnabled + ", maxRetries: " + maxRetries + ", retryDelayMs: " + this.retryDelayMs);
	}
	
	/**
	 * Executes a command.
	 * @param command The command to execute
	 * @return true if successful, false otherwise
	 */
	public boolean executeCommand(Command command)
	{
		if (command == null)
		{
			TestLogManager.error("Cannot execute null command");
			return false;
		}
		
		TestLogManager.info("Executing command: " + command.getClass().getSimpleName());
		
		int attempts = 0;
		int maxAttempts = retryEnabled ? maxRetries + 1 : 1;
		
		while (attempts < maxAttempts)
		{
			try
			{
				boolean success = command.execute();
				
				if (success)
				{
					commandHistory.add(command);
					lastErrorMessage = null;
					lastException = null;
					TestLogManager.success("Command executed successfully: " + command.getClass().getSimpleName());
					return true;
				}
				else
				{
					attempts++;
					lastErrorMessage = "Command returned unsuccessful status on attempt " + attempts + "/" + maxAttempts;
					lastException = null;
					if (attempts < maxAttempts)
					{
						TestLogManager.warning("Command failed, retrying... (attempt " + attempts + "/" + maxAttempts + ")");
						if (retryDelayMs > 0)
						{
							try
							{
								Thread.sleep(retryDelayMs);
							}
							catch (InterruptedException ie)
							{
								Thread.currentThread().interrupt();
								lastException = ie;
								TestLogManager.warning("Retry sleep interrupted");
							}
						}
					}
				}
				
			}
			catch (Exception e)
			{
				attempts++;
				TestLogManager.error("Command execution failed (attempt " + attempts + "/" + maxAttempts + ")", e);
				lastException = e;
				lastErrorMessage = e.getMessage();
				
				if (attempts >= maxAttempts)
				{
					TestLogManager.error("Command failed after " + maxAttempts + " attempts: " + command.getClass().getSimpleName());
					return false;
				}
				if (retryDelayMs > 0)
				{
					try
					{
						Thread.sleep(retryDelayMs);
					}
					catch (InterruptedException ie)
					{
						Thread.currentThread().interrupt();
						lastException = ie;
						TestLogManager.warning("Retry sleep interrupted");
					}
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Executes multiple commands in sequence.
	 * @param commands List of commands to execute
	 * @return true if all commands successful, false otherwise
	 */
	public boolean executeCommands(List<Command> commands)
	{
		if (commands == null || commands.isEmpty())
		{
			TestLogManager.warning("No commands to execute");
			return true;
		}
		
		TestLogManager.info("Executing " + commands.size() + " commands");
		
		for (Command command : commands)
		{
			if (!executeCommand(command))
			{
				TestLogManager.error("Command execution failed, stopping batch execution");
				return false;
			}
		}
		
		TestLogManager.success("All commands executed successfully");
		return true;
	}
	
	/**
	 * Undoes the last command.
	 * @return true if successful, false otherwise
	 */
	public boolean undoLastCommand()
	{
		if (commandHistory.isEmpty())
		{
			TestLogManager.warning("No commands to undo");
			return false;
		}
		
		Command lastCommand = commandHistory.remove(commandHistory.size() - 1);
		
		try
		{
			boolean success = lastCommand.undo();
			if (success)
			{
				TestLogManager.info("Command undone successfully: " + lastCommand.getClass().getSimpleName());
			}
			else
			{
				TestLogManager.warning("Failed to undo command: " + lastCommand.getClass().getSimpleName());
			}
			return success;
			
		}
		catch (Exception e)
		{
			TestLogManager.error("Error undoing command: " + lastCommand.getClass().getSimpleName(), e);
			return false;
		}
	}
	
	/**
	 * Undoes multiple commands.
	 * @param count Number of commands to undo
	 * @return true if all undone successfully, false otherwise
	 */
	public boolean undoCommands(int count)
	{
		if (count <= 0)
		{
			TestLogManager.warning("Invalid undo count: " + count);
			return false;
		}
		
		if (commandHistory.size() < count)
		{
			TestLogManager.warning("Not enough commands to undo. Available: " + commandHistory.size() + ", Requested: " + count);
			count = commandHistory.size();
		}
		
		TestLogManager.info("Undoing " + count + " commands");
		
		boolean allSuccessful = true;
		for (int i = 0; i < count; i++)
		{
			if (!undoLastCommand())
			{
				allSuccessful = false;
			}
		}
		
		return allSuccessful;
	}
	
	/**
	 * Gets the command history.
	 * @return List of executed commands
	 */
	public List<Command> getCommandHistory()
	{
		return new ArrayList<>(commandHistory);
	}
	
	/**
	 * Clears the command history.
	 */
	public void clearHistory()
	{
		commandHistory.clear();
		TestLogManager.info("Command history cleared");
	}
	
	/**
	 * Gets the number of commands in history.
	 * @return Number of commands
	 */
	public int getHistorySize()
	{
		return commandHistory.size();
	}
	
	/**
	 * Gets the last error message from a failed execution, if any.
	 * @return Last error message or null
	 */
	public String getLastErrorMessage()
	{
		return lastErrorMessage;
	}
	
	/**
	 * Gets the last exception thrown during execution, if any.
	 * @return Last exception or null
	 */
	public Throwable getLastException()
	{
		return lastException;
	}
}