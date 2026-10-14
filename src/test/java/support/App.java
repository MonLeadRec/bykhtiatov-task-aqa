package support;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

// Запуск тестируемого jar как отдельного процесса. Один раз на JVM,
// гасится shutdown-хуком.
public final class App {

    public static final String SECRET = "qazWSXedc";

    private static Process process;
    private static int appPort;

    private App() {
    }

    public static synchronized void start(int port, int mockPort) {
        if (process != null) {
            return;
        }
        appPort = port;

        Path jar = resolveJar();
        List<String> cmd = List.of(
                javaExecutable(),
                "-Dsecret=" + SECRET,
                "-Dmock=http://localhost:" + mockPort,
                "-Dserver.port=" + port,
                "-jar",
                jar.toAbsolutePath().toString()
        );

        try {
            ProcessBuilder pb = new ProcessBuilder(cmd).redirectErrorStream(true);
            process = pb.start();
        } catch (IOException e) {
            throw new IllegalStateException("Не смог запустить jar: " + jar, e);
        }

        // Выливаем stdout приложения в консоль тестов, чтобы в CI не терялось.
        Thread pump = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    System.out.println("[app] " + line);
                }
            } catch (IOException ignored) {
            }
        }, "app-log-pump");
        pump.setDaemon(true);
        pump.start();

        Runtime.getRuntime().addShutdownHook(new Thread(App::stop, "app-shutdown"));

        waitForPort(port, 60_000);
    }

    public static int port() {
        return appPort;
    }

    private static synchronized void stop() {
        if (process == null) {
            return;
        }
        process.destroy();
        try {
            if (!process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        process = null;
    }

    private static Path resolveJar() {
        String override = System.getProperty("app.jar");
        if (override != null && !override.isBlank() && !override.contains("${")) {
            Path p = Paths.get(override);
            if (!Files.isRegularFile(p)) {
                throw new IllegalStateException("app.jar указан, но файла нет: " + p);
            }
            return p;
        }
        Path def = Paths.get("app", "internal-0.0.1-SNAPSHOT.jar");
        if (!Files.isRegularFile(def)) {
            throw new IllegalStateException(
                    "Jar не найден. Положи internal-0.0.1-SNAPSHOT.jar в app/ "
                            + "или передай путь через -Dapp.jar=...");
        }
        return def;
    }

    private static String javaExecutable() {
        String home = System.getProperty("java.home");
        String name = System.getProperty("os.name", "").toLowerCase().contains("win")
                ? "java.exe" : "java";
        return Paths.get(home, "bin", name).toString();
    }

    private static void waitForPort(int port, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            try (Socket s = new Socket("localhost", port)) {
                return;
            } catch (IOException ignored) {
                if (!process.isAlive()) {
                    throw new IllegalStateException("Приложение упало до того, как открыло порт " + port);
                }
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
        throw new IllegalStateException("Приложение не открыло порт " + port + " за " + timeoutMs + " мс");
    }
}
