package support;

import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;

public final class Responses {

    private Responses() {
    }

    public static void assertOk(Response r) {
        Assertions.assertAll("Ожидался успешный ответ",
                () -> Assertions.assertEquals(200, r.statusCode(), "HTTP код"),
                () -> Assertions.assertEquals("OK", r.jsonPath().getString("result"), "result"),
                () -> Assertions.assertNull(r.jsonPath().get("message"), "message")
        );
    }

    public static void assertError(Response r, int expectedStatus, String expectedMessage) {
        Assertions.assertAll("Ожидалась ошибка " + expectedStatus,
                () -> Assertions.assertEquals(expectedStatus, r.statusCode(), "HTTP код"),
                () -> Assertions.assertEquals("ERROR", r.jsonPath().getString("result"), "result"),
                () -> Assertions.assertEquals(expectedMessage, r.jsonPath().getString("message"), "message")
        );
    }

    // Когда точный текст message зависит от локали JVM (Jakarta Validation).
    public static void assertErrorStartsWith(Response r, int expectedStatus, String messagePrefix) {
        Assertions.assertEquals(expectedStatus, r.statusCode(), "HTTP код");
        Assertions.assertEquals("ERROR", r.jsonPath().getString("result"), "result");
        String msg = r.jsonPath().getString("message");
        Assertions.assertNotNull(msg, "message не должен быть null");
        Assertions.assertTrue(msg.startsWith(messagePrefix),
                "Ожидалось сообщение, начинающееся с '" + messagePrefix + "', а пришло: '" + msg + "'");
    }
}
