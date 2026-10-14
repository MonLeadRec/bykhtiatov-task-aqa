# Автотесты internal-сервиса /endpoint

Java 17 + JUnit 5 + WireMock + RestAssured + Allure + Maven.

Тестируется Spring Boot сервис с одним эндпоинтом `POST /endpoint`
и тремя действиями (`LOGIN`, `ACTION`, `LOGOUT`). Внутри он ходит
во внешний сервис по `/auth` и `/doAction`; этот внешний сервис
подменяется WireMock'ом.

## Что понадобится

* JDK 17+
* Maven 3.6+
* сам jar приложения, лежит в `app/internal-0.0.1-SNAPSHOT.jar`
  (в репозиторий не коммичу, положить руками)

Порты по умолчанию: приложение на 8080, WireMock на 8888. Если заняты,
переопределяется через `-Dapp.port=... -Dmock.port=...`.

## Запуск

```bash
mvn clean test
```

Процессом приложения рулят сами тесты. jar стартует один раз в
`@BeforeAll`, адрес внешнего сервиса передаётся через `-Dmock=...`,
останавливается shutdown-хуком.

Пара других вариантов:

```bash
mvn clean test -Dtest=LoginTests
mvn clean test -Dtest=ValidationTests#tokenFormatRejected
mvn clean test -Dapp.jar="C:/path/to/internal-0.0.1-SNAPSHOT.jar"
```

## Allure

Результаты пишутся в `target/allure-results` каждый прогон. Поднять отчёт:

```bash
mvn allure:serve
```

Либо, если установлен CLI:

```bash
allure serve target/allure-results
```

## Что покрыто

В Allure тесты сгруппированы по направлениям (Feature):

| Направление | Файл | Что внутри |
| --- | --- | --- |
| Проверка ключа доступа | `AuthenticationTests` | нет ключа, не тот ключ, другой регистр |
| Вход пользователя | `LoginTests` | валидный вход, повторный вход, что он не беспокоит внешний сервис, вход после выхода |
| Выполнение действия | `ActionTests` | действие после входа и без него, после выхода, подряд несколько раз, изоляция между пользователями |
| Завершение работы | `LogoutTests` | выход после входа (во внешний сервис не ходит), выход без входа, двойной выход |
| Проверка входных данных | `ValidationTests` | формат идентификатора пользователя, отсутствие и неизвестные значения действия |
| Сбои во внешнем сервисе | `ExternalServiceTests` | /auth и /doAction отдают разные 2xx/4xx/5xx: что возвращается клиенту, что остаётся в хранилище |
| Полный цикл работы | `FullFlowTests` | сценарий «вход → три действия → выход → повторный вход» |

Всего 45 тестов. У каждого есть короткое описание в `@Description` — его видно
в Allure, и оно написано без технических терминов.

## Расхождение с заданием

В тексте задания написано, что `token` — 32 символа из `A-Z0-9`.
Фактический валидатор в приложении строже: `^[0-9A-F]{32}$` (видно по
аннотациям на `UserRequest`, подтверждается в `ValidationTests`).

Токены вида `GHIJ...` или вообще что-то с буквами вне hex-диапазона
валидатор не пропускает, возвращает 400. Тесты писал на реальное
поведение сервиса; если это баг, они упадут, когда его починят.

Генератор `Tokens.random()` соответственно отдаёт только `[0-9A-F]`.

## Структура

```
src/test/java/
├── support/
│   ├── App.java         запуск/остановка jar, ожидание порта
│   ├── Mock.java        WireMock + дефолтные стабы на /auth и /doAction
│   ├── Api.java         RestAssured-обёртка над POST /endpoint
│   ├── Tokens.java      генератор валидных токенов
│   ├── Responses.java   короткие ассерты assertOk / assertError
│   └── BaseTest.java    @BeforeAll поднимает всё, @BeforeEach сбрасывает стабы
└── tests/
    ├── AuthenticationTests.java
    ├── LoginTests.java
    ├── ActionTests.java
    ├── LogoutTests.java
    ├── ValidationTests.java
    ├── ExternalServiceTests.java
    └── FullFlowTests.java
```
