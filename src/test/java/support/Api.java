package support;

import io.qameta.allure.Step;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

public final class Api {

    public static final String PATH = "/endpoint";
    public static final String API_KEY_HEADER = "X-API-Key";

    private Api() {
    }

    public static RequestSpecification request() {
        return rawRequest().header(API_KEY_HEADER, App.SECRET);
    }

    // Без API-ключа, для негативных тестов аутентификации.
    public static RequestSpecification rawRequest() {
        return RestAssured.given()
                .baseUri("http://localhost")
                .port(App.port())
                .contentType(ContentType.URLENC)
                .accept(ContentType.JSON)
                .filter(new AllureRestAssured());
    }

    @Step("Отправляю запрос: токен {token}, действие {action}")
    public static Response send(String token, String action) {
        return request()
                .formParam("token", token)
                .formParam("action", action)
                .post(PATH);
    }

    @Step("Пользователь входит в систему (токен {token})")
    public static Response login(String token) {
        return request()
                .formParam("token", token)
                .formParam("action", "LOGIN")
                .post(PATH);
    }

    @Step("Пользователь выполняет действие (токен {token})")
    public static Response action(String token) {
        return request()
                .formParam("token", token)
                .formParam("action", "ACTION")
                .post(PATH);
    }

    @Step("Пользователь завершает работу (токен {token})")
    public static Response logout(String token) {
        return request()
                .formParam("token", token)
                .formParam("action", "LOGOUT")
                .post(PATH);
    }
}
