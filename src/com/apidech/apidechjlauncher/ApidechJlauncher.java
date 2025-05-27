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

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JWindow;
import javax.swing.Timer;

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

        // First-run: create default config
        if (!ini.exists()) {
            createDefaultConfig(cfg, ini);
            log("Configuration", "Created default " + ini.getAbsolutePath() + ". Please edit and re-run.", JOptionPane.INFORMATION_MESSAGE);
            System.exit(0);
        }

        // Load configuration
        try (FileReader reader = new FileReader(ini)) {
            cfg.load(reader);
        } catch (IOException e) {
            log("Configuration Error", "Error reading " + ini.getAbsolutePath() + ": " + e.getMessage(), JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        // Read splash settings
        boolean showSplash = Boolean.parseBoolean(cfg.getProperty("showSplash", "true"));
        int splashDuration = parseInt(cfg.getProperty("splashDuration", "8"), 8);

        // Build command
        List<String> cmd = buildCommand(cfg);

        // Launch target app in background
        try {
            new ProcessBuilder(cmd)
                .directory(baseDir)
                .inheritIO()
                .start();
        } catch (IOException e) {
            log("Launch Error", "Failed to start target app: " + e.getMessage(), JOptionPane.ERROR_MESSAGE);
            System.exit(2);
        }

        // Show splash screen if requested
        if (showSplash && !GraphicsEnvironment.isHeadless()) {
            showSplashScreen(splashDuration);
        }

        // Exit launcher immediately
        System.exit(0);
    }

    private static void showSplashScreen(int seconds) {
        JWindow splash = new JWindow();
        ImageIcon icon = null;
        try {
            icon = new ImageIcon(ApidechJlauncher.class.getResource(
                "/com/apidech/apidechjlauncher/resources/splash.jpg"));
        } catch (Exception ignored) {}
        if (icon != null) {
            splash.getContentPane().add(new JLabel(icon));
            splash.pack();
            splash.setLocationRelativeTo(null);
            splash.setVisible(true);

            // Close after duration using Swing Timer
            new Timer(seconds * 1000, evt -> splash.dispose()).start();

            // Block to keep splash visible
            try {
                Thread.sleep(seconds * 1000L);
            } catch (InterruptedException ignored) {}
        }
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

        String appArgs = cfg.getProperty("appArgs", "").trim();
        if (!appArgs.isEmpty()) {
            cmd.addAll(tokenize(appArgs));
        }

        return cmd;
    }

    private static void log(String title, String message, int type) {
        if (GraphicsEnvironment.isHeadless()) {
            if (type == JOptionPane.ERROR_MESSAGE) System.err.println(title + ": " + message);
            else System.out.println(title + ": " + message);
        } else {
            JOptionPane.showMessageDialog(null, message, title, type);
        }
    }

    private static void createDefaultConfig(Properties cfg, File ini) {
        cfg.setProperty("javaExecutable", "java");
        cfg.setProperty("javaOptions", "-Xmx512m");
        cfg.setProperty("appJar", "app.jar");
        cfg.setProperty("appArgs", "<your args here>");
        cfg.setProperty("showSplash", "true");
        cfg.setProperty("splashDuration", "8");

        try (FileWriter writer = new FileWriter(ini)) {
            cfg.store(writer, "ApidechJlauncher configuration (edit values as needed)");
        } catch (IOException e) {
            log("File Error", "Unable to write default config: " + e.getMessage(), JOptionPane.ERROR_MESSAGE);
        }
    }

    private static File getBaseDirectory() {
        // Attempt via java.class.path preserving symlink
        String cp = System.getProperty("java.class.path", "");
        if (cp != null) {
            String[] entries = cp.split(File.pathSeparator);
            if (entries.length > 0 && entries[0].toLowerCase().endsWith(".jar")) {
                File jar = new File(entries[0]);
                if (!jar.isAbsolute()) jar = new File(System.getProperty("user.dir", "."), entries[0]);
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
        } catch (URISyntaxException ignored) {}
        // Fallback to working dir
        return new File(System.getProperty("user.dir", "."));
    }

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

    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return def; }
    }
}
