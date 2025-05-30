# ApidechJlauncher

**Lightweight Java Application Launcher**

ApidechJlauncher is a standalone, cross-platform Java launcher designed to start Java applications with custom JVM options (e.g. `-Xmx`) and application arguments. It optionally displays a splash screen before exiting, running the target application in the background.

---

## Features

* **Standalone**: No external libraries required.
* **Custom JVM options**: Configure memory flags, module paths, and more via `launcher.ini`.
* **Custom application arguments**: Pass parameters to your `main()` method.
* **Splash screen**: Optional Swing-based splash display from a bundled image.
* **Cross-platform**: Works on Windows, Linux, and macOS.
* **Background execution**: Launcher exits immediately after spawning the child process, leaving your app running.

---

## Getting Started

### Prerequisites

* Java 21 or higher
* Maven (for building)

### Build

```bash
# Clone the repository
git clone https://github.com/tackleza/ApidechJlauncher.git
cd ApidechJlauncher

# Build the executable JAR
mvn clean package
```

After building, the JAR will be located at:

```
target/apidech-jlauncher-1.0.0.jar
```

### Installation

1. Copy `apidechjlauncher-0.0.1-SNAPSHOT.jar` and your application JAR (e.g., `app.jar`) into a directory.
2. Copy your splash image to `src/main/resources/com/apidech/apidechjlauncher/resources/splash.jpg` (already bundled).

---

## Configuration (`launcher.ini`)

On first run, the launcher will generate a default `launcher.ini` next to the JAR. Edit it to customize the launch behavior.

```ini
# ApidechJlauncher configuration (edit values as needed)

# Java executable (full path or on PATH)
javaExecutable=java

# JVM options (memory flags, module-path, add-modules, etc.)
javaOptions=-Xmx512m

# The JAR to launch (relative to this directory)
appJar=app.jar

# Arguments passed to your main() method
appArgs=<your args here>

# Show splash screen? (true/false)
showSplash=true

# Splash screen duration (seconds)
splashDuration=10
```

---

## Usage

```bash
# First run: generates launcher.ini and exits
java -jar apidech-jlauncher-1.0.0.jar

# After configuring launcher.ini, run:
java -jar apidech-jlauncher-1.0.0.jar
```

On Windows, double-clicking the JAR will also work (if `.jar` files are associated with Java).

---

## License

This project is licensed under the **Apache License 2.0**. See [LICENSE](LICENSE) for details.
