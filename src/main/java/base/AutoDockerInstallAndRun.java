package base;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AutoDockerInstallAndRun {

    public static void main(String[] args) {
        dockerInstallAndRun();
    }

    public static void dockerInstallAndRun() {
        try {
            String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
            String arch = System.getProperty("os.arch").toLowerCase(Locale.ROOT);
            System.out.println("Detected OS: " + os + " | Arch: " + arch);

            // 1) Install if missing
            if (!dockerCliPresent()) {
                section("Installing Docker");
                if (os.contains("linux")) {
                    installDockerOnLinux();
                } else if (os.contains("win")) {
                    installDockerOnWindows();
                } else if (os.contains("mac") || os.contains("darwin")) {
                    installDockerOnMac(arch);
                } else {
                    throw new UnsupportedOperationException("Unsupported OS: " + os);
                }
            } else {
                System.out.println("✔ Docker CLI already present.");
            }

            // --- Enhanced Windows Onboarding/First-Run Skip ---
            if (os.contains("win")) {
                section("Configuring Docker Desktop (Force Skip Onboarding + WSL)");

                // 1. Stop if running
                stopDockerDesktopIfRunning();
                sleep(3000);

                // 2. Nuclear Option: Complete Reset (Uncommented for robustness)
                completelyResetDockerDesktop(); 
                
                // 3. Create pre-configured settings BEFORE starting Docker
                createPreConfiguredSettings();
                disableFirstRunViaRegistry();
                
                updateWSL();
            }
            // -------------------------------------------------

            // 2) Start engine/Desktop
            section("Starting Docker engine");
            if (os.contains("linux")) {
                startDockerOnLinux();
            } else if (os.contains("win")) {
                startDockerOnWindows();
            } else {
                startDockerOnMac();
            }

            // 3) Wait until engine is ready (with one auto-restart fallback on Windows)
            section("Waiting for Docker to become ready");
            boolean up = waitUntilDockerReadyWithFallback(900); // up to 15 minutes
            if (!up) {
                System.err.println("✖ Docker engine did not become ready.");
                System.exit(1);
            }

            // 4) Sanity test
            section("Running hello-world");
            int code = dockerRun("docker", "run", "--rm", "hello-world");
            if (code == 0) System.out.println("✔ Success: hello-world ran.");
            else System.out.println("hello-world exit code: " + code);
        } catch (Exception e) {
            System.out.println("✖ Docker setup failed: " + e.getMessage());
            e.printStackTrace(System.out);
        }
    }

    // ---------------- Installers ----------------

    private static void installDockerOnLinux() throws Exception {
        Path tmp = Files.createTempDirectory("docker-install-");
        Path script = tmp.resolve("install-docker.sh");
        downloadSingleLine("https://get.docker.com", script, "Docker install script");

        runWithSpinner(new String[]{"chmod", "+x", script.toString()}, "Marking installer executable");
        if (isRoot()) runWithSpinner(new String[]{"sh", script.toString()}, "Running Docker installer");
        else runWithSpinner(new String[]{"sudo", "sh", script.toString()}, "Running Docker installer (sudo)");

        runIgnoreErrorsWithSpinner(new String[]{"sudo", "systemctl", "enable", "docker"}, "Enabling docker service");
        runIgnoreErrorsWithSpinner(new String[]{"sudo", "usermod", "-aG", "docker", System.getProperty("user.name")},
                "Adding user to docker group");
    }

    private static void installDockerOnWindows() throws Exception {
        Path tmp = Files.createTempDirectory("docker-install-");
        Path exe = tmp.resolve("DockerDesktopInstaller.exe");
        downloadSingleLine("https://desktop.docker.com/win/main/amd64/Docker%20Desktop%20Installer.exe", exe,
                "Docker Desktop (Windows)");
        runWithSpinner(new String[]{exe.toString(), "install", "--quiet", "--accept-license"},
                "Installing Docker Desktop (silent)");
    }

    private static void installDockerEngineOnWindows() throws Exception {
        section("Installing Docker Engine on Windows (alternative approach)");
        
        // Install Docker using Chocolatey
        String chocoInstall = "powershell -Command \"Set-ExecutionPolicy Bypass -Scope Process -Force; " +
                             "[System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072; " +
                             "iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))\"";
        
        runWithSpinner(new String[]{"cmd", "/c", chocoInstall}, "Installing Chocolatey");
        runWithSpinner(new String[]{"choco", "install", "docker-engine", "-y"}, "Installing Docker Engine");
        
        System.out.println("✔ Docker Engine installed (no Desktop GUI)");
    }

    private static void installDockerOnMac(String arch) throws Exception {
        boolean arm = arch.contains("aarch64") || arch.contains("arm64");
        String url = arm ? "https://desktop.docker.com/mac/main/arm64/Docker.dmg"
                         : "https://desktop.docker.com/mac/main/amd64/Docker.dmg";

        Path tmp = Files.createTempDirectory("docker-install-");
        Path dmg = tmp.resolve("Docker.dmg");
        downloadSingleLine(url, dmg, "Docker Desktop (macOS)");

        runIgnoreErrorsWithSpinner(new String[]{"hdiutil", "detach", "/Volumes/Docker"}, "Cleaning previous mounts");
        runWithSpinner(new String[]{"hdiutil", "attach", dmg.toString(), "-mountpoint", "/Volumes/Docker", "-nobrowse"},
                "Mounting Docker.dmg");
        runWithSpinner(new String[]{"sudo", "cp", "-R", "/Volumes/Docker/Docker.app", "/Applications/Docker.app"},
                "Copying Docker.app to /Applications");
        runIgnoreErrorsWithSpinner(new String[]{"hdiutil", "detach", "/Volumes/Docker"}, "Unmounting image");
    }

    // --------------- Start/Stop Desktop -------------

    private static void startDockerOnLinux() throws Exception {
        runIgnoreErrorsWithSpinner(new String[]{"sudo", "systemctl", "daemon-reload"}, "Reloading systemd");
        runIgnoreErrorsWithSpinner(new String[]{"sudo", "systemctl", "enable", "docker"}, "Enabling docker service");
        runIgnoreErrorsWithSpinner(new String[]{"sudo", "systemctl", "start", "docker"}, "Starting docker service");
    }

    private static void startDockerOnWindows() throws Exception {
        stopDockerDesktopIfRunning();
        sleep(2000); // Wait for processes to fully stop
        
        // Try silent start first
        startDockerDesktopSilently();
        
        // Fallback after a delay if needed
        sleep(5000);
        if (!isDockerEngineReady()) {
            System.out.println("Silent start failed, trying normal start...");
            String psCmd =
                "$p1=$Env:ProgramFiles+'\\Docker\\Docker\\Docker Desktop.exe';" +
                "if (Test-Path $p1) { Start-Process -FilePath $p1 } else { Start-Process 'Docker Desktop' }";
            runWithSpinner(new String[]{"powershell", "-NoProfile", "-Command", psCmd}, 
                          "Launching Docker Desktop (fallback)");
        }
    }

    private static void startDockerDesktopSilently() throws Exception {
        String programFiles = System.getenv("ProgramFiles");
        String dockerCli = programFiles + "\\Docker\\Docker\\Docker Desktop.exe";
        
        if (Files.exists(Paths.get(dockerCli))) {
            // Updated parameters, adding --skip-onboarding to the list
            String[] silentParams = {
                "--silent-start --window-style=hidden --skip-onboarding",
                "--no-skip-onboarding --window-style=hidden", // Retain old attempts
                "--quit-on-startup --window-style=hidden",
                "--unattended --window-style=hidden"
            };
            
            boolean started = false;
            for (String params : silentParams) {
                if (!started) {
                    System.out.println("Trying parameters: " + params);
                    String psCmd = String.format(
                        "Start-Process -FilePath '%s' -ArgumentList '%s' -WindowStyle Hidden",
                        dockerCli, params
                    );
                    
                    runWithSpinner(new String[]{"powershell", "-NoProfile", "-Command", psCmd}, 
                                  "Starting Docker Desktop silently");
                    
                    // Wait and check if it started
                    sleep(10000);
                    if (isDockerEngineReady()) {
                        started = true;
                        System.out.println("✔ Docker started successfully with: " + params);
                        break;
                    } else {
                        // Crucial: Stop and try the next parameter set if it failed to start
                        stopDockerDesktopIfRunning();
                        sleep(3000);
                    }
                }
            }
            
            // Final attempt with no parameters but hidden window
            if (!started) {
                String psCmd = String.format(
                    "Start-Process -FilePath '%s' -WindowStyle Hidden",
                    dockerCli
                );
                runWithSpinner(new String[]{"powershell", "-NoProfile", "-Command", psCmd}, 
                              "Final attempt: Starting Docker Desktop");
            }
            
        } else {
            // Fallback
            String psCmd = "Start-Process 'Docker Desktop' -WindowStyle Hidden";
            runWithSpinner(new String[]{"powershell", "-NoProfile", "-Command", psCmd}, 
                          "Launching Docker Desktop (fallback)");
        }
    }

    private static boolean isDockerDesktopProcessRunning() {
        try {
            Process p = new ProcessBuilder("powershell", "-NoProfile", "-Command",
                "Get-Process 'Docker Desktop' -ErrorAction SilentlyContinue | Measure-Object | Select-Object -ExpandProperty Count")
                .redirectErrorStream(true).start();
            p.waitFor(5, TimeUnit.SECONDS);
            String output = readAll(p.getInputStream()).trim();
            return Integer.parseInt(output) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static void stopDockerDesktopIfRunning() {
        try {
            // Attempt graceful shutdown first
            new ProcessBuilder("powershell", "-NoProfile", "-Command",
                "Get-Process 'Docker Desktop' -ErrorAction SilentlyContinue | Stop-Process -Force")
                .inheritIO().start().waitFor();
            sleep(2000); // Give it time to shutdown
            
            // Also kill backend processes
            String[] processesToKill = {
                "com.docker.backend", "com.docker.service", "docker", "dockerd", "vpnkit" // Added vpnkit
            };
            
            for (String process : processesToKill) {
                try {
                    new ProcessBuilder("powershell", "-NoProfile", "-Command",
                        "Get-Process '" + process + "' -ErrorAction SilentlyContinue | Stop-Process -Force")
                        .start().waitFor();
                } catch (Exception e) {}
            }
        } catch (Exception ignored) {}
    }

    private static void startDockerOnMac() throws Exception {
        runIgnoreErrorsWithSpinner(new String[]{"open", "-a", "/Applications/Docker.app"}, "Launching Docker Desktop");
    }

    // --------------- Windows: onboarding + WSL -------------------

    private static void completelyResetDockerDesktop() {
        try {
            System.out.println("Performing complete Docker Desktop reset (nuclear option)...");
            
            // 1. Stop all Docker processes
            stopDockerDesktopIfRunning();
            sleep(5000);
            
            // 2. Kill all related processes (redundant but safe)
            String[] processesToKill = {
                "Docker Desktop", "com.docker.backend", "com.docker.service", 
                "docker", "dockerd", "com.docker.proxy", "vpnkit"
            };
            
            for (String process : processesToKill) {
                try {
                    new ProcessBuilder("powershell", "-NoProfile", "-Command",
                        "Get-Process '" + process + "' -ErrorAction SilentlyContinue | Stop-Process -Force")
                        .start().waitFor();
                } catch (Exception e) {}
            }
            
            // 3. Delete all settings directories
            String appData = System.getenv("APPDATA");
            String localAppData = System.getenv("LOCALAPPDATA");
            
            if (appData != null && localAppData != null) {
                Path[] dirsToDelete = {
                    Paths.get(appData, "Docker"),
                    Paths.get(appData, "Docker Desktop"),
                    Paths.get(localAppData, "Docker")
                };
                
                for (Path dir : dirsToDelete) {
                    deleteDirectoryRecursively(dir);
                }
            }
            
            // 4. Delete ProgramData Docker directory
            String programData = System.getenv("ProgramData");
            if (programData != null) {
                // Must be run as administrator, using a more robust PowerShell command
                String psCmd = 
                    "if (Test-Path '"+Paths.get(programData, "Docker")+"') {" +
                    "  Remove-Item '"+Paths.get(programData, "Docker")+"' -Recurse -Force -ErrorAction SilentlyContinue;" +
                    "}";
                new ProcessBuilder("powershell", "-NoProfile", "-Command", psCmd).start().waitFor();
                System.out.println("Cleaned ProgramData\\Docker");
            }
            
            // 5. Reset registry completely
            resetDockerRegistrySettings();
            
            // 6. Clear credential manager entries
            clearDockerCredentials();
            
            System.out.println("✔ Complete Docker Desktop reset finished.");
            
        } catch (Exception e) {
            System.out.println("⚠ Complete reset failed: " + e.getMessage());
        }
    }

    private static void deleteDirectoryRecursively(Path path) {
        try {
            if (Files.exists(path)) {
                // Use PowerShell for reliable recursive deletion, even for protected files
                String psCmd = String.format(
                    "Remove-Item -Path '%s' -Recurse -Force -ErrorAction SilentlyContinue",
                    path.toString()
                );
                new ProcessBuilder("powershell", "-NoProfile", "-Command", psCmd).start().waitFor();
                System.out.println("Deleted: " + path);
            }
        } catch (Exception e) {
            System.out.println("Could not delete: " + path + " - " + e.getMessage());
        }
    }

    private static void resetDockerRegistrySettings() {
        try {
            String[] regCommands = {
                // Delete user-specific settings
                "REG DELETE \"HKCU\\Software\\Docker Inc.\" /f",
                // Delete machine-wide settings (errors if not admin/doesn't exist, so ignore)
                "REG DELETE \"HKLM\\Software\\Docker Inc.\" /f 2>nul",
                // Re-add keys to specifically skip first-run UI
                "REG ADD \"HKCU\\Software\\Docker Inc.\\Docker Desktop\" /v FirstRun /t REG_DWORD /d 0 /f",
                "REG ADD \"HKCU\\Software\\Docker Inc.\\Docker Desktop\" /v HasRun /t REG_DWORD /d 1 /f",
                "REG ADD \"HKCU\\Software\\Docker Inc.\\Docker Desktop\" /v ShowWelcome /t REG_DWORD /d 0 /f",
                "REG ADD \"HKCU\\Software\\Docker Inc.\\Docker Desktop\" /v HideFirstRunWelcome /t REG_DWORD /d 1 /f"
            };
            
            for (String cmd : regCommands) {
                Process p = Runtime.getRuntime().exec(cmd);
                p.waitFor(10, TimeUnit.SECONDS);
            }
            System.out.println("✔ Registry completely reset and configured to skip first run.");
        } catch (Exception e) {
            System.out.println("⚠ Registry reset failed: " + e.getMessage());
        }
    }

    private static void clearDockerCredentials() {
        try {
            // Find and delete all credentials containing "docker"
            String cmd = "cmdkey /list | findstr -i docker | for /f \"tokens=1,2 delims= \" %a in ('more') do @cmdkey /delete:%b";
            new ProcessBuilder("cmd", "/c", cmd).start().waitFor();
            System.out.println("✔ Docker credentials cleared.");
        } catch (Exception e) {
            System.out.println("⚠ Could not clear credentials: " + e.getMessage());
        }
    }

    private static void createPreConfiguredSettings() {
        try {
            String appData = System.getenv("APPDATA");
            if (appData == null) return;

            // Comprehensive settings flags to skip ALL onboarding/tutorial screens
            String comprehensiveSettings = "{"
                + "\"onboardingShown\": true,"
                + "\"welcomeShown\": true,"
                + "\"tutorialShown\": true,"
                + "\"tipsShown\": true,"
                + "\"allowCollectUsageData\": false,"
                + "\"loginOnStartup\": false,"
                + "\"displayWelcomeMessage\": false,"
                + "\"displayWelcomePopup\": false,"
                + "\"showWhatsNew\": false,"
                + "\"autoStart\": true,"
                + "\"startOnLogin\": true,"
                + "\"disableCredentialHelper\": true,"
                + "\"skipLoginPrompt\": true,"
                + "\"hideFirstRunWelcome\": true,"
                + "\"hasSeenAccountPrompt\": true,"
                + "\"initializationScreenShown\": true,"
                + "\"displayTutorial\": false,"
                + "\"newsFeedShown\": false,"
                + "\"useDockerDesktopWSL2\": true,"
                + "\"wslEngineEnabled\": true,"
                + "\"kubernetesEnabled\": false" // Explicitly disable Kubernetes unless needed
                + "}";

            // Write to both possible locations
            Path[] settingsFiles = {
                Paths.get(appData, "Docker", "settings-store.json"),
                Paths.get(appData, "Docker Desktop", "settings.json")
            };
            
            for (Path file : settingsFiles) {
                Files.createDirectories(file.getParent());
                Files.writeString(file, comprehensiveSettings, 
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
            
            System.out.println("✔ Pre-configured settings created.");
            
        } catch (Exception e) {
            System.out.println("⚠ Could not create pre-configured settings: " + e.getMessage());
        }
    }

    private static void disableFirstRunViaRegistry() {
        // Re-using the same reset logic for assurance just before the start.
        resetDockerRegistrySettings();
    }

    // NOTE: ensureJsonFlags and setBoolean methods removed as createPreConfiguredSettings writes a complete JSON string now.

    /** Trigger WSL update (kernel) so Desktop can start the engine. */
    private static void updateWSL() {
        try {
            // Use wsl --update and set default to v2 for best performance with Docker Desktop
            String ps = "Start-Process powershell -Verb RunAs -WindowStyle Hidden -ArgumentList '"
                      + "wsl --update; wsl --set-default-version 2; wsl --status; exit 0'";
            new ProcessBuilder("powershell", "-NoProfile", "-Command", ps)
                    .inheritIO().start().waitFor();
            System.out.println("✔ WSL update requested (check Desktop after restart).");
        } catch (Exception e) {
            System.out.println("⚠ WSL update could not be triggered: " + e.getMessage());
        }
    }

    // --------------- Readiness wait (with fallback) -------------------

    /**
     * Wait for engine; after ~180s, restart Desktop once and keep waiting.
     * Uses 'docker version' (negotiates API) with env sanitized.
     */
    private static boolean waitUntilDockerReadyWithFallback(int totalTimeoutSeconds) {
        long start = System.currentTimeMillis();
        boolean restarted = false;
        int tick = 0;

        while ((System.currentTimeMillis() - start) / 1000 < totalTimeoutSeconds) {
            try {
                Process p = dockerPb("docker", "version", "--format", "{{json .}}")
                        .redirectErrorStream(true).start();
                boolean finished = p.waitFor(10, TimeUnit.SECONDS);
                String out = readAll(p.getInputStream()).trim();
                if (finished && p.exitValue() == 0 && !out.isEmpty() && !"null".equalsIgnoreCase(out)) {
                    System.out.print("\r");
                    System.out.println("✔ Docker engine is ready.");
                    return true;
                }
            } catch (Exception ignored) {}

            // If we've waited ~180s and not restarted yet (Windows), try restarting Desktop once
            if (!restarted && isWindows() && (System.currentTimeMillis() - start) / 1000 > 180) {
                System.out.print("\r↻ Restarting Docker Desktop…");
                stopDockerDesktopIfRunning();
                try { startDockerOnWindows(); } catch (Exception ignored) {}
                restarted = true;
            }

            spinnerTick("Waiting for Docker engine", ++tick);
            sleep(500);
        }
        System.out.println();
        return false;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
    }

    // ---------------- Helpers ----------------

    private static boolean dockerCliPresent() {
        try {
            Process p = dockerPb("docker", "--version").redirectErrorStream(true).start();
            p.waitFor(5, TimeUnit.SECONDS);
            return p.exitValue() == 0;
        } catch (Exception e) { return false; }
    }

    private static boolean isDockerEngineReady() {
        try {
            Process p = dockerPb("docker", "version").start();
            return p.waitFor(10, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static int run(String[] cmd) throws Exception {
        System.out.println("$ " + String.join(" ", cmd));
        Process p = new ProcessBuilder(cmd).inheritIO().start();
        p.waitFor();
        return p.exitValue();
    }

    /** Run with spinner and stream output without flooding the console. */
    private static int runWithSpinner(String[] cmd, String label) throws Exception {
        System.out.println("$ " + String.join(" ", cmd));
        int i = 0;
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        while (p.isAlive()) {
            while (br.ready() && (line = br.readLine()) != null) {
                System.out.print("\r" + padRight(line, 78));
            }
            spinnerTick(label, ++i);
            sleep(120);
        }
        int code = p.waitFor();
        System.out.print("\r");
        System.out.println((code == 0 ? "✔ " : "✖ ") + label + " (exit " + code + ")");
        return code;
    }

    private static void runIgnoreErrorsWithSpinner(String[] cmd, String label) {
        try { runWithSpinner(cmd, label); } catch (Exception ignored) {}
    }

    /** Single-line MB-only download progress */
    private static void downloadSingleLine(String url, Path dest, String label) throws IOException {
        System.out.println("Downloading " + label + ":");
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setInstanceFollowRedirects(true);
        c.setRequestProperty("User-Agent", "Java AutoDockerInstall");
        int contentLength = c.getContentLength();

        try (InputStream in = new BufferedInputStream(c.getInputStream());
             OutputStream out = new BufferedOutputStream(Files.newOutputStream(dest, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {

            byte[] buf = new byte[1 << 16];
            long read = 0, last = 0;
            printSingleLineProgress(read, contentLength, label);
            int n;
            while ((n = in.read(buf)) >= 0) {
                out.write(buf, 0, n);
                read += n;
                long now = System.nanoTime();
                if (now - last > 80_000_000L) {
                    last = now;
                    printSingleLineProgress(read, contentLength, label);
                }
            }
            printSingleLineProgress(read, contentLength, label);
            System.out.println();
        }

        if (dest.toString().endsWith(".sh")) {
            try {
                Files.setPosixFilePermissions(dest, EnumSet.of(
                        PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE,
                        PosixFilePermission.GROUP_READ, PosixFilePermission.OTHERS_READ));
            } catch (Exception ignored) {}
        }
        System.out.println("Saved: " + dest);
    }

    private static void printSingleLineProgress(long readBytes, int contentLengthBytes, String label) {
        double readMB = readBytes / (1024.0 * 1024.0);
        if (contentLengthBytes > 0) {
            double totalMB = contentLengthBytes / (1024.0 * 1024.0);
            int pct = (int) Math.min(100, Math.round((readMB / totalMB) * 100));
            String line = String.format("\r%s: %.1f / %.1f MB (%d%%)", label, readMB, totalMB, pct);
            System.out.print(line);
        } else {
            String line = String.format("\r%s: %.1f MB", label, readMB);
            System.out.print(line);
        }
        System.out.flush();
    }

    private static String readAll(InputStream in) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            StringBuilder sb = new StringBuilder();
            for (String line; (line = br.readLine()) != null; ) sb.append(line).append('\n');
            return sb.toString();
        }
    }

    private static boolean isRoot() {
        try {
            Process p = new ProcessBuilder("id", "-u").start();
            p.waitFor(3, TimeUnit.SECONDS);
            String out = readAll(p.getInputStream()).trim();
            return "0".equals(out);
        } catch (Exception e) { return false; }
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    // ---------- Docker env-sanitized execution ----------

    private static ProcessBuilder dockerPb(String... cmd) {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        Map<String, String> env = pb.environment();
        env.remove("DOCKER_API_VERSION");
        env.remove("DOCKER_HOST");
        return pb;
    }

    private static int dockerRun(String... cmd) throws Exception {
        System.out.println("$ " + String.join(" ", cmd));
        Process p = dockerPb(cmd).inheritIO().start();
        p.waitFor();
        return p.exitValue();
    }

    // ---------- Tiny console UI helpers ----------

    private static void section(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }

    private static void spinnerTick(String label, int tick) {
        char[] frames = {'|', '/', '-', '\\'};
        char f = frames[tick % frames.length];
        String line = String.format("\r%c %s...", f, label);
        System.out.print(padRight(line, 80));
    }

    private static String padRight(String s, int width) {
        if (s.length() >= width) return s.substring(0, Math.min(width, s.length()));
        StringBuilder sb = new StringBuilder(width);
        sb.append(s);
        while (sb.length() < width) sb.append(' ');
        return sb.toString();
    }
}