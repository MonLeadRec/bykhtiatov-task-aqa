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
@Feature("Выполнение действия")
@Owner("bykhtiatov")
class ActionTests extends BaseTest {

    @Test
    @DisplayName("Активный пользователь может выполнить действие")
    @Description("Пользователь, уже вошедший в систему, запрашивает выполнение действия. "
            + "Сервис подтверждает, что действие принято, и передаёт его во внешнюю "
            + "систему одним запросом с тем же идентификатором пользователя.")
    void actionAfterLoginSucceeds() {
        String token = Tokens.random();
        Responses.assertOk(Api.login(token));
        WireMock.resetAllRequests();

        Response r = Api.action(token);

        Responses.assertOk(r);
        WireMock.verify(1, WireMock.postRequestedFor(WireMock.urlEqualTo("/doAction"))
                .withRequestBody(WireMock.containing("token=" + token)));
    }

    @Test
    @DisplayName("Пользователь без входа действие выполнить не может")
    @Description("Если пользователь не входил в систему, попытка выполнить действие "
            + "отклоняется — сервис сообщает, что такой пользователь ему неизвестен.")
    void actionWithoutLoginIsForbidden() {
        String token = Tokens.random();

        Response r = Api.action(token);

        Responses.assertError(r, 403, "Token '" + token + "' not found");
    }

    @Test
    @DisplayName("После выхода действия пользователя снова недоступны")
    @Description("После того как пользователь завершил работу, он снова считается "
            + "неактивным. Попытка выполнить действие от его имени отклоняется.")
    void actionAfterLogoutIsForbidden() {
        String token = Tokens.random();
        Api.login(token);
        Api.logout(token);

        Response r = Api.action(token);

        Responses.assertError(r, 403, "Token '" + token + "' not found");
    }

    @Test
    @DisplayName("Пользователь может выполнять действия подряд, пока активен")
    @Description("Один вход в систему разрешает любое количество действий — сервис "
            + "не разлогинивает пользователя после первого действия. "
            + "Все три действия должны проходить и уходить во внешнюю систему.")
    void actionCanBeRepeated() {
        String token = Tokens.random();
        Api.login(token);

        Responses.assertOk(Api.action(token));
        Responses.assertOk(Api.action(token));
        Responses.assertOk(Api.action(token));

        WireMock.verify(3, WireMock.postRequestedFor(WireMock.urlEqualTo("/doAction")));
    }

    @Test
    @DisplayName("Пользователи не влияют друг на друга")
    @Description("Два разных пользователя действуют независимо. Если один вошёл "
            + "в систему, а второй — нет, их статусы не смешиваются: активный "
            + "продолжает работать, неактивному по-прежнему всё запрещено.")
    void actionDoesNotLeakAcrossTokens() {
        String a = Tokens.random();
        String b = Tokens.random();
        Api.login(a);

        Responses.assertError(Api.action(b), 403, "Token '" + b + "' not found");
        Responses.assertOk(Api.action(a));
    }
}
