package tests;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import support.Api;
import support.App;
import support.BaseTest;
import support.Responses;
import support.Tokens;

@Epic("Внутренний сервис обработки действий пользователя")
@Feature("Проверка ключа доступа")
@Owner("bykhtiatov")
class AuthenticationTests extends BaseTest {

    @Test
    @DisplayName("Без ключа доступа сервис не отвечает на запросы")
    @Description("Если клиент обращается к сервису без заголовка с ключом доступа, "
            + "сервис должен отказать в обслуживании и вернуть сообщение о том, "
            + "что ключ отсутствует или не подходит. Это защищает сервис от "
            + "запросов из внешнего мира от неизвестных отправителей.")
    void missingApiKeyReturns401() {
        Response r = Api.rawRequest()
                .formParam("token", Tokens.random())
                .formParam("action", "LOGIN")
                .post(Api.PATH);

        Responses.assertError(r, 401, "Missing or invalid API Key");
    }

    @Test
    @DisplayName("С неправильным ключом доступа сервис тоже отвечает отказом")
    @Description("Если клиент передаёт ключ доступа, но значение не совпадает "
            + "с ожидаемым, сервис отвечает тем же отказом, что и для запроса "
            + "без ключа вовсе.")
    void wrongApiKeyReturns401() {
        Response r = Api.rawRequest()
                .header(Api.API_KEY_HEADER, "definitely-not-the-secret")
                .formParam("token", Tokens.random())
                .formParam("action", "LOGIN")
                .post(Api.PATH);

        Responses.assertError(r, 401, "Missing or invalid API Key");
    }

    @Test
    @DisplayName("Ключ доступа чувствителен к регистру букв")
    @Description("Строчные и заглавные буквы в ключе — разные символы. "
            + "Тот же секрет, но написанный в другом регистре, уже не считается "
            + "правильным, и сервис отказывает в доступе.")
    void apiKeyIsCaseSensitive() {
        Response r = Api.rawRequest()
                .header(Api.API_KEY_HEADER, App.SECRET.toUpperCase())
                .formParam("token", Tokens.random())
                .formParam("action", "LOGIN")
                .post(Api.PATH);

        Responses.assertError(r, 401, "Missing or invalid API Key");
    }
}
