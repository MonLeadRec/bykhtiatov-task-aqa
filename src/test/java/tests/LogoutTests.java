package tests;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import support.Api;
import support.BaseTest;
import support.Responses;
import support.Tokens;

@Epic("Внутренний сервис обработки действий пользователя")
@Feature("Завершение работы")
@Owner("bykhtiatov")
class LogoutTests extends BaseTest {

    @Test
    @DisplayName("Пользователь завершает работу — внешнюю систему это не беспокоит")
    @Description("Выход из системы — внутренняя операция. Сервис подтверждает "
            + "выход сразу, не отправляя никаких запросов во внешнюю систему.")
    void logoutAfterLoginSucceeds() {
        String token = Tokens.random();
        Api.login(token);
        WireMock.resetAllRequests();

        Response r = Api.logout(token);

        Responses.assertOk(r);
        WireMock.verify(0, WireMock.postRequestedFor(WireMock.urlEqualTo("/auth")));
        WireMock.verify(0, WireMock.postRequestedFor(WireMock.urlEqualTo("/doAction")));
    }

    @Test
    @DisplayName("Нельзя выйти, если не входил")
    @Description("Если пользователь не входил в систему, попытка выйти "
            + "отклоняется — выходить просто некому.")
    void logoutWithoutLoginIsForbidden() {
        String token = Tokens.random();

        Response r = Api.logout(token);

        Responses.assertError(r, 403, "Token '" + token + "' not found");
    }

    @Test
    @DisplayName("Два выхода подряд: второй уже отклоняется")
    @Description("Первый выход завершает работу пользователя как положено. "
            + "Повторная попытка выйти под тем же пользователем отклоняется "
            + "по той же причине, что и выход без входа.")
    void doubleLogoutReturnsForbiddenOnSecond() {
        String token = Tokens.random();
        Api.login(token);
        Responses.assertOk(Api.logout(token));

        Response second = Api.logout(token);

        Responses.assertError(second, 403, "Token '" + token + "' not found");
    }
}
