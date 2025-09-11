package base;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import reporting.TestLogManager;

public class DockerManager
{
	// DOCKER Compose Property
	private static DockerManager dockerInstance;
	public static final String DOCKER_COMPOSE_PATH = PropertyManager.getPropFileRoot();
	public static final String DOCKER_COMPOSE_UP = System.getProperty("Docker_Compose_Up");
	public static final String DOCKER_COMPOSE_DOWN = System.getProperty("Docker_Compose_Down");
	public static final String DOCKER_GET_CONTAINERS = System.getProperty("Docker_Get_Containers");
	public static final String DOCKER_GET_LOGS = System.getProperty("Docker_Get_Logs");

	public static final String GREEN_COLOR = "\u001B[32;1m";
	public static final String BLUE_COLOR = "\033[1;34m";
	public static final String ANSI_BOLD = "\u001B[1m";
	public static final String RESET_COLOR = "\u001B[0m";

	private DockerManager() {

	}

	public DockerManager getDockerInstance()
	{
		if (dockerInstance == null)
		{
			dockerInstance = new DockerManager();
		}
		return dockerInstance;
	}

	public static void dockerContainterUp()
	{
		if (DOCKER_COMPOSE_UP != null)
		{
			printAlignedBoxedText("DOCKER CONTAINER STARTED SUCCESSFULLY", 100);
			executeCommand(DOCKER_COMPOSE_UP, DOCKER_COMPOSE_PATH);
			getDockerContainers();
		}
	}

	public static void dockerContainterDown()
	{
		if (DOCKER_COMPOSE_DOWN != null)
		{
			printAlignedBoxedText("DOCKER CONTAINER ENDED SUCCESSFULLY", 100);
			executeCommand(DOCKER_COMPOSE_DOWN, DOCKER_COMPOSE_PATH);
			getDockerContainers();
		}
	}

	private static void getDockerContainers()
	{
		printAlignedBoxedText("DOCKER RUNNING CONTAINERS", 180);
		executeCommand(DOCKER_GET_CONTAINERS, DOCKER_COMPOSE_PATH);
	}

	private static void getDockerLogs()
	{
		executeCommand(DOCKER_GET_LOGS, DOCKER_COMPOSE_PATH);
	}

	private static void executeCommand(String command, String workingDirectoryPath)
	{
		CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
			try
			{
				Process process = new ProcessBuilder(command.split("\\s+")).directory(new File(workingDirectoryPath)).redirectErrorStream(true).start();
				Thread outputThread = new Thread(new StreamGobbler(process.getInputStream()));
				outputThread.start();

				// Terminate the process if it takes too long
				if (!process.waitFor(40, TimeUnit.SECONDS))
				{
					process.destroy();
				} else
				{
					int exitCode = process.waitFor();
					if (exitCode != 0)
					{
						throw new RuntimeException("FAILED..! to execute Docker command '" + command + "' with Exitcode : " + exitCode);
					}
				}

			} catch (IOException | InterruptedException e)
			{
				TestLogManager.error("Docker command execution failed", e);
			}
		});

		future.join();
		TestLogManager.info("Docker command execution completed");
	}

	public static void printAlignedBoxedText(String text, int width)
	{
		String border = "+".concat("-".repeat(width - 2)).concat("+");
		TestLogManager.info("Docker container border: " + border);

		int padding = (width - text.length() - 2) / 2;

		TestLogManager.info("Docker container text: " + text);

		TestLogManager.info("Docker container border end: " + border);
	}

	private static class StreamGobbler implements Runnable
	{
		private final BufferedReader reader;

		StreamGobbler(InputStream inputStream) {
			this.reader = new BufferedReader(new InputStreamReader(inputStream));
		}

		@Override
		public void run()
		{
			try
			{
				String line;
				while ((line = reader.readLine()) != null)
				{
					TestLogManager.info("Docker stream: " + line);
				}
				TestLogManager.info("Docker stream completed");
			} catch (IOException e)
			{
				TestLogManager.error("Docker stream error", e);
			}
		}
	}

}