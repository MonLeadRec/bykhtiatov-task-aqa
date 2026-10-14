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
@Feature("Вход пользователя")
@Owner("bykhtiatov")
class LoginTests extends BaseTest {

    @Test
    @DisplayName("Пользователь успешно входит в систему")
    @Description("Новый пользователь впервые заходит в систему. Сервис подтверждает "
            + "вход и регистрирует пользователя во внешней системе — именно "
            + "одним запросом, с тем же идентификатором, что указал пользователь.")
    void loginSucceedsAndCallsAuthOnce() {
        String token = Tokens.random();

        Response r = Api.login(token);

        Responses.assertOk(r);
        WireMock.verify(1, WireMock.postRequestedFor(WireMock.urlEqualTo("/auth"))
                .withRequestBody(WireMock.containing("token=" + token)));
    }

    @Test
    @DisplayName("Повторный вход под тем же пользователем отклоняется")
    @Description("Если пользователь уже вошёл в систему, повторная попытка входа "
            + "под тем же идентификатором отклоняется — сервис сообщает, что "
            + "такой пользователь уже активен.")
    void doubleLoginReturnsConflict() {
        String token = Tokens.random();
        Responses.assertOk(Api.login(token));

        Response second = Api.login(token);

        Responses.assertError(second, 409, "Token '" + token + "' already exists");
    }

    @Test
    @DisplayName("Повторный вход не беспокоит внешнюю систему")
    @Description("При повторной попытке входа сервис сам видит, что пользователь "
            + "уже активен, и отвечает отказом сразу, не дёргая внешнюю систему. "
            + "Это экономит ресурсы и защищает внешний сервис от лишней нагрузки.")
    void duplicateLoginDoesNotHitExternal() {
        String token = Tokens.random();
        Api.login(token);
        WireMock.resetAllRequests();

        Api.login(token);

        WireMock.verify(0, WireMock.postRequestedFor(WireMock.urlEqualTo("/auth")));
    }

    @Test
    @DisplayName("После выхода тот же пользователь может войти снова")
    @Description("После завершения работы пользователь считается неактивным, и "
            + "следующий вход под тем же идентификатором проходит нормально. "
            + "Проверяем, что выход действительно освобождает учётную запись.")
    void loginAgainAfterLogoutWorks() {
        String token = Tokens.random();
        Responses.assertOk(Api.login(token));
        Responses.assertOk(Api.logout(token));

        Responses.assertOk(Api.login(token));
    }
}
