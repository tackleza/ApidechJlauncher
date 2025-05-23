package com.apidech.apidechjlauncher;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.swing.JOptionPane;

/**
 * Idea by Apidech, need this to run desktop app. like AverstCore in desktop mode,
 * Don't need console for that. can define -Xmx flag and other
 * 
 * Mostly code by ChatGPT o4-mini-high
 * Started project on 24/5/2025 02:40
 * 
 */
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
     */
    private static File getBaseDirectory() {
        // Attempt via java.class.path preserving symlink path
        String cp = System.getProperty("java.class.path", "");
        if (cp != null) {
            String[] entries = cp.split(File.pathSeparator);
            if (entries.length > 0 && entries[0].toLowerCase().endsWith(".jar")) {
                File jar = new File(entries[0]);
                if (!jar.isAbsolute()) {
                    jar = new File(System.getProperty("user.dir", "."), entries[0]);
                }
                if (jar.exists()) {
                    File dir = jar.getAbsoluteFile().getParentFile();
                    if (dir != null) return dir;
                }
            }
        }
        // Fallback to code source
        try {
            File jarFile = new File(ApidechJlauncher.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI());
            File dir = jarFile.getParentFile();
            if (dir != null) return dir;
        } catch (URISyntaxException ignored) {
        }
        // Fallback to working directory
        return new File(System.getProperty("user.dir", "."));
    }

    private static List<String> buildCommand(Properties cfg) {
        List<String> cmd = new ArrayList<>();
        cmd.add(cfg.getProperty("javaExecutable", "java"));

        String opts = cfg.getProperty("javaOptions", "").trim();
        if (!opts.isEmpty()) {
            cmd.addAll(tokenize(opts));
        }

        cmd.add("-jar");
        cmd.add(cfg.getProperty("appJar", "app.jar"));

        String appArgs = cfg.getProperty("appArgs", "<your args here>").trim();
        if (!appArgs.isEmpty()) {
            cmd.addAll(tokenize(appArgs));
        }

        return cmd;
    }

    /**
     * Launches the process in the base directory
     */
    private static int launchProcess(List<String> cmd) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(cmd)
            .directory(getBaseDirectory())
            .inheritIO();
        Process p = pb.start();
        return p.waitFor();
    }

    /**
     * Logs messages via JOptionPane in GUI mode or console otherwise.
     */
    private static void log(String title, String message, int type) {
        if (GraphicsEnvironment.isHeadless()) {
            if (type == JOptionPane.ERROR_MESSAGE) System.err.println(title + ": " + message);
            else System.out.println(title + ": " + message);
        } else {
            JOptionPane.showMessageDialog(null, message, title, type);
        }
    }

    /**
     * Create default configuration file
     */
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

    /**
     * Splits a command-line string into tokens, honoring quotes
     */
    private static List<String> tokenize(String str) {
        List<String> tokens = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        boolean inQuote = false;
        char quoteChar = 0;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (inQuote) {
                if (c == quoteChar) inQuote = false;
                else buf.append(c);
            } else {
                if (c == '"' || c == '\'') {
                    inQuote = true;
                    quoteChar = c;
                } else if (Character.isWhitespace(c)) {
                    if (buf.length() > 0) {
                        tokens.add(buf.toString()); buf.setLength(0);
                    }
                } else buf.append(c);
            }
        }
        if (buf.length() > 0) tokens.add(buf.toString());
        return tokens;
    }
}
