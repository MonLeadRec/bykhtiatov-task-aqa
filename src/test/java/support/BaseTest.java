package support;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

public abstract class BaseTest {

    private static boolean started;

    @BeforeAll
    static void bootstrap() {
        if (started) {
            return;
        }
        int appPort = intProp("app.port", 8080);
        int mockPort = intProp("mock.port", 8888);

        Mock.start(mockPort);
        Mock.resetToDefaults();
        App.start(appPort, mockPort);
        started = true;
    }

    @BeforeEach
    void resetStubs() {
        Mock.resetToDefaults();
    }

    private static int intProp(String name, int fallback) {
        String raw = System.getProperty(name);
        if (raw == null || raw.isBlank() || raw.contains("${")) {
            return fallback;
        }
        return Integer.parseInt(raw.trim());
    }
}
