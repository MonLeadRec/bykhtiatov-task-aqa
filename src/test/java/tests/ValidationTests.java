package tests;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import support.Api;
import support.BaseTest;
import support.Responses;
import support.Tokens;

import java.util.stream.Stream;

// В задании написано, что идентификатор пользователя — 32 символа из A-Z0-9,
// но сам сервис принимает более узкий набор: только 0-9 и A-F.
// Тесты проверяют реальное поведение сервиса.
@Epic("Внутренний сервис обработки действий пользователя")
@Feature("Проверка входных данных")
@Owner("bykhtiatov")
class ValidationTests extends BaseTest {

    @ParameterizedTest
    @MethodSource("badTokens")
    @DisplayName("Идентификатор пользователя неправильного формата отклоняется")
    @Description("Идентификатор должен быть длиной ровно 32 символа и состоять "
            + "только из цифр 0-9 и латинских букв A-F в верхнем регистре. "
            + "Если он короче, длиннее, содержит буквы в нижнем регистре, "
            + "спецсимволы или пробелы — сервис его не принимает.")
    void tokenFormatRejected(String token) {
        Response r = Api.login(token);

        Responses.assertErrorStartsWith(r, 400, "token:");
    }

    static Stream<Arguments> badTokens() {
        return Stream.of(
                Arguments.of(Named.of("пустая строка", "")),
                Arguments.of(Named.of("короче 32 символов", "ABC")),
                Arguments.of(Named.of("длиннее 32 символов", "ABCDEF0123456789ABCDEF0123456789A")),
                Arguments.of(Named.of("строчные буквы", "abcdef0123456789abcdef0123456789")),
                Arguments.of(Named.of("буква G, не из допустимого набора", "ABCDEF0123456789ABCDEF012345678G")),
                Arguments.of(Named.of("спецсимвол на конце", "ABCDEF0123456789ABCDEF012345678!")),
                Arguments.of(Named.of("пробел внутри", "ABCDEF0123456789ABCDEF012345678 "))
        );
    }

    @Test
    @DisplayName("Запрос без идентификатора пользователя не принимается")
    @Description("Если в запросе нет идентификатора пользователя, сервис "
            + "сообщает, что обязательное поле не заполнено.")
    void missingTokenRejected() {
        Response r = Api.request()
                .formParam("action", "LOGIN")
                .post(Api.PATH);

        Responses.assertErrorStartsWith(r, 400, "token:");
    }

    @ParameterizedTest(name = "действие \"{0}\"")
    @ValueSource(strings = {"login", "Login", "LOG IN", "DELETE", "WAT", ""})
    @DisplayName("Неизвестное действие отклоняется")
    @Description("Сервис знает только три действия: вход, действие и выход. "
            + "Любое другое значение (в том числе написанное в другом регистре "
            + "или с опечаткой) принимается как ошибка клиента.")
    void unknownActionRejected(String action) {
        Response r = Api.send(Tokens.random(), action);

        Responses.assertErrorStartsWith(r, 400, "action:");
    }

    @Test
    @DisplayName("Запрос без действия не принимается")
    @Description("Если в запросе не указано, какое действие хочет выполнить "
            + "пользователь, сервис сообщает, что обязательное поле не заполнено.")
    void missingActionRejected() {
        Response r = Api.request()
                .formParam("token", Tokens.random())
                .post(Api.PATH);

        Responses.assertErrorStartsWith(r, 400, "action:");
    }

    @Test
    @DisplayName("Крайние значения допустимого набора символов принимаются")
    @Description("Проверяем две границы: идентификатор, состоящий только из нулей, "
            + "и идентификатор, состоящий только из букв F. И тот и другой "
            + "соответствуют правилу, поэтому сервис должен их принять.")
    void validHexBoundariesAccepted() {
        Responses.assertOk(Api.login("00000000000000000000000000000000"));
        Responses.assertOk(Api.login("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"));
    }
}
