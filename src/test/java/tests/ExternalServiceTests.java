package tests;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import support.Api;
import support.BaseTest;
import support.Responses;
import support.Tokens;

// Что происходит, когда внешний сервис отвечает с ошибкой. Проверяется и то,
// что пришло клиенту, и состояние хранилища пользователей после сбоя.
@Epic("Внутренний сервис обработки действий пользователя")
@Feature("Сбои во внешнем сервисе")
@Owner("bykhtiatov")
class ExternalServiceTests extends BaseTest {

    @ParameterizedTest(name = "внешняя система отвечает с кодом {0}")
    @ValueSource(ints = {400, 401, 403, 404, 500, 502, 503})
    @DisplayName("Если внешняя система не подтверждает вход, пользователь получает ошибку")
    @Description("При входе наш сервис обязательно подтверждает пользователя во "
            + "внешней системе. Если внешняя система отвечает с любой ошибкой "
            + "(неважно, 4xx или 5xx), пользователь получает понятное сообщение "
            + "о внутренней ошибке сервиса.")
    void loginFailsWhenAuthReturnsNon2xx(int status) {
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/auth"))
                .willReturn(WireMock.aResponse().withStatus(status)));

        Response r = Api.login(Tokens.random());

        Responses.assertError(r, 500, "Internal Server Error");
    }

    @Test
    @DisplayName("Если внешняя система отказала при входе, пользователь не считается активным")
    @Description("Проверяем, что сервис не записывает пользователя в активные, пока "
            + "внешняя система не подтвердила вход. Если подтверждение не пришло, "
            + "последующая попытка выполнить действие должна отклоняться.")
    void failedLoginDoesNotPersistToken() {
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/auth"))
                .willReturn(WireMock.aResponse().withStatus(500)));
        String token = Tokens.random();
        Api.login(token);

        // Внешняя система снова в норме. Сам факт того, что ACTION всё равно
        // возвращает ошибку, доказывает, что токен не сохранился после сбоя.
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/auth"))
                .willReturn(WireMock.aResponse().withStatus(200)));

        Response r = Api.action(token);
        Responses.assertError(r, 403, "Token '" + token + "' not found");
    }

    @Test
    @DisplayName("Если внешняя система не выполнила действие, пользователь остаётся активным")
    @Description("Сбой при выполнении действия не выкидывает пользователя из системы. "
            + "После того как внешняя система снова заработала, пользователь "
            + "продолжает работать как ни в чём не бывало.")
    void failedActionKeepsTokenInStorage() {
        String token = Tokens.random();
        Responses.assertOk(Api.login(token));
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/doAction"))
                .willReturn(WireMock.aResponse().withStatus(503)));

        Responses.assertError(Api.action(token), 500, "Internal Server Error");

        // Внешняя система снова в норме. Если бы токен удалился из хранилища,
        // следующий ACTION вернул бы 403, а не 200.
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/doAction"))
                .willReturn(WireMock.aResponse().withStatus(200)));
        Responses.assertOk(Api.action(token));
    }

    @ParameterizedTest(name = "внешняя система отвечает с кодом {0}")
    @ValueSource(ints = {200, 201, 202, 204})
    @DisplayName("Любой успешный ответ внешней системы считается подтверждением входа")
    @Description("Сервис смотрит только на то, успешен ли ответ внешней системы, "
            + "а не на конкретный код. Разные коды успеха (200, 201, 202, 204) "
            + "одинаково трактуются как подтверждение.")
    void loginSucceedsOnAny2xx(int status) {
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/auth"))
                .willReturn(WireMock.aResponse().withStatus(status)));

        Responses.assertOk(Api.login(Tokens.random()));
    }
}
