package support;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

// WireMock для внешнего сервиса. Порт фиксированный, поэтому один
// инстанс на всю JVM и подняться должен раньше, чем сам jar.
public final class Mock {

    private static WireMockServer server;

    private Mock() {
    }

    public static synchronized void start(int port) {
        if (server != null) {
            return;
        }
        server = new WireMockServer(WireMockConfiguration.options().port(port).notifier(
                new com.github.tomakehurst.wiremock.common.ConsoleNotifier(false)));
        server.start();
        WireMock.configureFor("localhost", port);
    }

    public static WireMockServer server() {
        if (server == null) {
            throw new IllegalStateException("WireMock ещё не стартовал");
        }
        return server;
    }

    // Дефолт: оба эндпоинта возвращают 200. Тест может переопределить.
    public static void resetToDefaults() {
        server().resetAll();
        server().stubFor(WireMock.post(WireMock.urlEqualTo("/auth"))
                .willReturn(WireMock.aResponse().withStatus(200)));
        server().stubFor(WireMock.post(WireMock.urlEqualTo("/doAction"))
                .willReturn(WireMock.aResponse().withStatus(200)));
    }
}
