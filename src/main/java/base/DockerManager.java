package base;

import java.io.*;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import reporting.TestLogManager;

public class DockerManager {

    // ---------- Config ----------
    private static DockerManager dockerInstance;

    // Where your docker-compose.yml lives (keep your existing PropertyManager)
    public static final String DOCKER_COMPOSE_PATH = PropertyManager.getPropFileRoot();

    // Commands supplied via -D system properties (keep your existing wiring)
    public static final String DOCKER_COMPOSE_UP   = System.getProperty("Docker_Compose_Up");     // e.g. "docker compose -f docker-compose.yml up -d"
    public static final String DOCKER_COMPOSE_DOWN = System.getProperty("Docker_Compose_Down");   // e.g. "docker compose -f docker-compose.yml down"
    public static final String DOCKER_GET_CONTAINERS = System.getProperty("Docker_Get_Containers"); // e.g. "docker ps --format \"table {{.Names}}\t{{.Status}}\""
    public static final String DOCKER_GET_LOGS     = System.getProperty("Docker_Get_Logs");        // e.g. "docker compose logs --no-color"

    // Timeout (minutes) can be overridden: -Ddocker.exec.timeout.min=15
    private static final long EXEC_TIMEOUT_MIN =
            Long.getLong("docker.exec.timeout.min", 1L);

    private DockerManager() {}

    // Make this static (singleton accessor)
    public static DockerManager getDockerInstance() {
        if (dockerInstance == null) {
            dockerInstance = new DockerManager();
        }
        return dockerInstance;
    }

    public static void dockerContainterUp() {
        if (DOCKER_COMPOSE_UP != null && !DOCKER_COMPOSE_UP.isBlank()) {
            printAlignedBoxedText("DOCKER CONTAINER STARTED SUCCESSFULLY", 100);
            executeCommand(DOCKER_COMPOSE_UP, DOCKER_COMPOSE_PATH, true); // raw streaming to show progress
            getDockerContainers();
        } else {
            TestLogManager.error("DOCKER_COMPOSE_UP is not set. Provide -DDocker_Compose_Up=...");
        }
    }

    public static void dockerContainterDown() {
        if (DOCKER_COMPOSE_DOWN != null && !DOCKER_COMPOSE_DOWN.isBlank()) {
            printAlignedBoxedText("DOCKER CONTAINER ENDED SUCCESSFULLY", 100);
            executeCommand(DOCKER_COMPOSE_DOWN, DOCKER_COMPOSE_PATH, true);
            getDockerContainers();
        } else {
            TestLogManager.error("DOCKER_COMPOSE_DOWN is not set. Provide -DDocker_Compose_Down=...");
        }
    }

    private static void getDockerContainers() {
        printAlignedBoxedText("DOCKER RUNNING CONTAINERS", 180);
        if (DOCKER_GET_CONTAINERS != null && !DOCKER_GET_CONTAINERS.isBlank()) {
            executeCommand(DOCKER_GET_CONTAINERS, DOCKER_COMPOSE_PATH, false);
        } else {
            TestLogManager.error("DOCKER_GET_CONTAINERS is not set. Provide -DDocker_Get_Containers=...");
        }
    }

    private static void getDockerLogs() {
        if (DOCKER_GET_LOGS != null && !DOCKER_GET_LOGS.isBlank()) {
            executeCommand(DOCKER_GET_LOGS, DOCKER_COMPOSE_PATH, true);
        } else {
            TestLogManager.error("DOCKER_GET_LOGS is not set. Provide -DDocker_Get_Logs=...");
        }
    }

    /**
     * Backwards-compatible signature: defaults to raw streaming (progress shown).
     */
    private static void executeCommand(String command, String workingDirectoryPath) {
        executeCommand(command, workingDirectoryPath, true);
    }

    /**
     * Executes a shell command with live streaming output.
     * @param command shell command string (quotes, pipes, && supported)
     * @param workingDirectoryPath working directory
     * @param rawStreaming if true, writes characters directly to console (preserves \r progress)
     */
    private static void executeCommand(String command, String workingDirectoryPath, boolean rawStreaming) {
        try {
            boolean isWindows = System.getProperty("os.name")
                    .toLowerCase(Locale.ROOT).contains("win");

            // Check if this is a docker-compose up command without -d flag
            boolean isComposeUp = command.matches(".*\\bdocker(\\s+compose|\\-compose)?\\s+up\\b.*");
            boolean hasDetachedFlag = command.contains(" -d") || command.contains(" --detach");
            
            String actualCommand = command;
            if (isComposeUp && !hasDetachedFlag) {
                // Add --wait flag which waits for services to be ready then exits
                actualCommand = command + " --wait";
                TestLogManager.info("Auto-adding --wait flag to docker-compose up command to prevent hanging");
            }

            ProcessBuilder pb = isWindows
                    ? new ProcessBuilder("cmd.exe", "/c", actualCommand)
                    : new ProcessBuilder("bash", "-lc", actualCommand);

            File workDir = new File(workingDirectoryPath);
            pb.directory(workDir);
            pb.redirectErrorStream(true);

            // Use shorter timeout for compose commands
            long timeoutMinutes = isComposeUp ? 5 : EXEC_TIMEOUT_MIN;

            TestLogManager.info("Executing: " + String.join(" ", pb.command()));
            TestLogManager.info("Working dir: " + workDir.getAbsolutePath());
            TestLogManager.info("Timeout (min): " + timeoutMinutes);

            Process process = pb.start();

            // Close stdin to prevent the child from waiting for input
            try { process.getOutputStream().close(); } catch (IOException ignore) {}

            Thread outThread = new Thread(new StreamGobbler(process.getInputStream(), rawStreaming));
            outThread.setName("Docker-StreamGobbler");
            outThread.setDaemon(true);
            outThread.start();

            boolean finished = process.waitFor(timeoutMinutes, TimeUnit.MINUTES);
//            if (!finished) {
//                process.destroyForcibly();
//                outThread.join(2000);
//                
//                // For compose up, check if containers are actually running despite timeout
//                if (isComposeUp) {
//                    if (areDockerComposeServicesRunning(workDir.getAbsolutePath())) {
//                        TestLogManager.info("Docker Compose services are running (ignoring process timeout)");
//                        return; // Success - services are running even though process timed out
//                    }
//                }
//                
//                throw new RuntimeException("Docker command timed out: " + actualCommand);
//            }

            int exitCode = process.exitValue();
            outThread.join();

            if (exitCode != 0) {
                throw new RuntimeException("FAILED to execute Docker command '" + actualCommand +
                        "' with ExitCode: " + exitCode);
            }

            TestLogManager.info("Docker command execution completed");
        } catch (Exception e) {
            TestLogManager.error("Docker command execution failed", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks if Docker Compose services are actually running
     */
    private static boolean areDockerComposeServicesRunning(String workingDir) {
        try {
            ProcessBuilder pb = new ProcessBuilder("docker", "compose", "ps", "--format", "json");
            pb.directory(new File(workingDir));
            Process process = pb.start();
            
            String output = readProcessOutput(process);
            boolean hasRunningServices = output.contains("\"State\":\"running\"");
            boolean hasSeleniumServices = output.contains("selenium") || output.contains("chrome") || output.contains("firefox");
            
            return hasRunningServices && hasSeleniumServices;
        } catch (Exception e) {
            TestLogManager.warning("Could not check Docker Compose services status: " + e.getMessage());
            return false;
        }
    }

    /**
     * Reads process output as string
     */
    private static String readProcessOutput(Process process) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
            return output.toString();
        }
    }

    public static void printAlignedBoxedText(String text, int width) {
        String border = "+".concat("-".repeat(Math.max(0, width - 2))).concat("+");
        TestLogManager.info(border);
        int innerWidth = Math.max(0, width - 2);
        int padding = Math.max(0, (innerWidth - text.length()) / 2);
        String line = "|" + " ".repeat(padding) + text + " ".repeat(Math.max(0, innerWidth - padding - text.length())) + "|";
        TestLogManager.info(line);
        TestLogManager.info(border);
    }

    /**
     * CR-aware stdout gobbler:
     * - When rawStreaming=true, writes characters directly to System.out (preserves '\r' updates).
     * - Also mirrors completed lines to TestLogManager for auditability.
     */
    private static class StreamGobbler implements Runnable {
        private final InputStream inputStream;
        private final boolean rawStreaming;

        StreamGobbler(InputStream inputStream, boolean rawStreaming) {
            this.inputStream = inputStream;
            this.rawStreaming = rawStreaming;
        }

        @Override
        public void run() {
            try (BufferedInputStream bis = new BufferedInputStream(inputStream)) {
                StringBuilder lineBuf = new StringBuilder();
                int b;
                while ((b = bis.read()) != -1) {
                    char ch = (char) b;

                    // Raw pass-through for progress rendering
                    if (rawStreaming) {
                        System.out.print(ch); // preserves \r
                    }

                    // Build lines for logger (on \n)
                    if (ch == '\r') {
                        // carriage return -> treat as a line update; don't log to TestLogManager
                        // (logging here would spam; the raw console already shows progress)
                        lineBuf.setLength(0);
                    } else if (ch == '\n') {
                        String s = lineBuf.toString();
                        if (!s.isEmpty()) {
                            TestLogManager.info(s);
                        }
                        lineBuf.setLength(0);
                    } else {
                        lineBuf.append(ch);
                    }
                }

                // Flush any remaining line
                if (lineBuf.length() > 0) {
                    String s = lineBuf.toString();
                    if (rawStreaming) {
                        System.out.println();
                    }
                    TestLogManager.info(s);
                }

                if (rawStreaming) {
                    System.out.flush();
                }
                TestLogManager.info("Docker stream completed");
            } catch (IOException e) {
                TestLogManager.error("Docker stream error", e);
            }
        }
    }
}