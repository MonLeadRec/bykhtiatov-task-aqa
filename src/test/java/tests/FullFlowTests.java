package tests;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import support.Api;
import support.BaseTest;
import support.Responses;
import support.Tokens;

// Один сценарный тест на весь цикл работы пользователя. Если он проходит,
// значит все состояния сервиса переключаются в правильном порядке.
@Epic("Внутренний сервис обработки действий пользователя")
@Feature("Полный цикл работы")
@Owner("bykhtiatov")
class FullFlowTests extends BaseTest {

    @Test
    @DisplayName("Пользователь входит, делает три действия, выходит и заходит снова")
    @Description("Проходим весь жизненный цикл одного пользователя: вход, "
            + "несколько действий, выход, повторный вход. По пути проверяем, "
            + "что после выхода действия становятся недоступны, а после "
            + "повторного входа — снова разрешены. В конце считаем, сколько "
            + "раз сервис обратился во внешнюю систему: должно быть два входа "
            + "и три действия, выход наружу не ходит.")
    void loginActionsLogoutLoginAgain() {
        String token = Tokens.random();

        Responses.assertOk(Api.login(token));
        Responses.assertOk(Api.action(token));
        Responses.assertOk(Api.action(token));
        Responses.assertOk(Api.action(token));
        Responses.assertOk(Api.logout(token));

        // После выхода действие больше недоступно.
        Responses.assertError(Api.action(token), 403, "Token '" + token + "' not found");

        // Хранилище очистилось, значит вход проходит снова.
        Responses.assertOk(Api.login(token));

        WireMock.verify(2, WireMock.postRequestedFor(WireMock.urlEqualTo("/auth")));
        WireMock.verify(3, WireMock.postRequestedFor(WireMock.urlEqualTo("/doAction")));
    }
}
