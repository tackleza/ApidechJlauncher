package com.apidech.apidechjlauncher;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.swing.JOptionPane;

public class ApidechJlauncher {
	
	private static final String CONFIG_FILE = "launcher.ini";

    public static void main(String[] args) {
        Properties cfg = new Properties();
        File baseDir = getBaseDirectory();
        File ini = new File(baseDir, CONFIG_FILE);

        if (!ini.exists()) {
            createDefaultConfig(cfg, ini);
            log("Configuration", "Created default " + ini.getAbsolutePath() + ". Please edit it and re-run the launcher.", JOptionPane.INFORMATION_MESSAGE);
            System.exit(0);
        }

        // Load user config
        try (FileReader reader = new FileReader(ini)) {
            cfg.load(reader);
        } catch (IOException e) {
            log("Configuration Error", "Error reading " + ini.getAbsolutePath() + ": " + e.getMessage(), JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        // Build command
        List<String> cmd = buildCommand(cfg);

        // Launch and wait
        try {
            int exitCode = launchProcess(cmd);
            System.exit(exitCode);
        } catch (IOException | InterruptedException e) {
            log("Launch Error", "Failed to launch target app: " + e.getMessage(), JOptionPane.ERROR_MESSAGE);
            System.exit(2);
        }
    }

    /**
     * Determine the directory where the JAR is located.
     * Tries to use the jar path from java.class.path (which preserves symlink path if launched via -jar).
     * Falls back to code source location or current directory.
     */
    private static File getBaseDirectory() {
        // 1. Try system property java.class.path
        String cp = System.getProperty("java.class.path", "");
        if (cp != null) {
            String[] entries = cp.split(File.pathSeparator);
            if (entries.length > 0) {
                String first = entries[0];
                if (first.toLowerCase().endsWith(".jar")) {
                    File jar = new File(first);
                    if (!jar.isAbsolute()) {
                        jar = new File(System.getProperty("user.dir", "."), first);
                    }
                    if (jar.exists()) {
                        File dir = jar.getAbsoluteFile().getParentFile();
                        if (dir != null) {
                            return dir;
                        }
                    }
                }
            }
        }
        // 2. Fallback to code source URL
        try {
            File jarFile = new File(ApidechJlauncher.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI());
            File dir = jarFile.getParentFile();
            if (dir != null) {
                return dir;
            }
        } catch (URISyntaxException e) {
            // ignore
        }
        // 3. Fallback to working directory
        return new File(System.getProperty("user.dir", "."));
    }

    private static List<String> buildCommand(Properties cfg) {
        List<String> cmd = new ArrayList<>();
        cmd.add(cfg.getProperty("javaExecutable", "java"));

        String opts = cfg.getProperty("javaOptions", "").trim();
        if (!opts.isEmpty()) {
            cmd.addAll(Arrays.asList(opts.split("\\s+")));
        }

        cmd.add("-jar");
        cmd.add(cfg.getProperty("appJar", "app.jar"));

        String appArgs = cfg.getProperty("appArgs", "<your args here>").trim();
        if (!appArgs.isEmpty()) {
            cmd.addAll(Arrays.asList(appArgs.split("\\s+")));
        }

        return cmd;
    }

    private static int launchProcess(List<String> cmd) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(cmd)
            .directory(getBaseDirectory())
            .inheritIO();
        Process p = pb.start();
        return p.waitFor();
    }

    private static void log(String title, String message, int type) {
        if (GraphicsEnvironment.isHeadless()) {
            if (type == JOptionPane.ERROR_MESSAGE) {
                System.err.println(title + ": " + message);
            } else {
                System.out.println(title + ": " + message);
            }
        } else {
            JOptionPane.showMessageDialog(null, message, title, type);
        }
    }

    private static void createDefaultConfig(Properties cfg, File ini) {
        cfg.setProperty("javaExecutable", "java");
        cfg.setProperty("javaOptions", "-Xmx512m");
        cfg.setProperty("appJar", "app.jar");
        cfg.setProperty("appArgs", "<your args here>");

        try (FileWriter writer = new FileWriter(ini)) {
            cfg.store(writer, "ApidechJlauncher configuration (edit values as needed)");
        } catch (IOException e) {
            log("File Error", "Unable to write default config: " + e.getMessage(), JOptionPane.ERROR_MESSAGE);
        }
    }
}
