package citrus_test;

import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.context.TestContext;
import org.springframework.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.Optional;
import org.testng.annotations.Test;

import com.consol.citrus.annotations.CitrusTest;
import com.consol.citrus.testng.spring.TestNGCitrusSpringSupport;

import static com.consol.citrus.actions.EchoAction.Builder.echo;
import static com.consol.citrus.dsl.JsonPathSupport.jsonPath;
import static com.consol.citrus.http.actions.HttpActionBuilder.http;

/**
 * This is a sample Java DSL Citrus integration test.
 *
 * @author Citrus
 */
@Test
public class SampleJavaIT extends TestNGCitrusSpringSupport {

    @CitrusTest
    public void echoToday() {
        variable("now", "citrus:currentDate()");

        run(echo("Today is: ${now}"));
    }

    @CitrusTest(name = "SampleJavaTest.sayHello")
    public void sayHello() {
        run(echo("Hello Citrus!"));
    }

    private boolean received = false;
    @CitrusTest
    public void positive(@Optional @CitrusResource TestContext contex) {
        variable("X-ID-Request", "citrus:randomNumber(10)");
        variable("accountNumber", "citrus:randomNumber(8)");
        // Отправка запроса от клиента к системе
        run(http() // <--------- http для работы с HTTP
                .client("httpClient") // <--------- client, так как отправка зпроса от клиента,  httpClient - id из citrus-context.xml
                .send() // <--------- send отправка запроса
                .post("api/v1/convert") // <--------- post запрос + path
                .message() // <--------- формируем сообщение
                .contentType("application/json") // <--------- contentType
                .header("X-ID-Request", "${X-ID-Request}") // <--------- ${X-ID-Request} подставляет значение
                .body("{\n" +
                        "   \"sortCode\":\"35-16-67\",\n" +
                        "   \"accountNumber\":\"${accountNumber}\",\n" +  // <--------- ${accountNumber} подставляет значение
                        "   \"sum\":\"100\",\n" +
                        "   \"currency\":\"USD\"\n" +
                        "}\n")
                .fork(true)
        );
        // Перехват и проверка запроса от системы во внешние системы (интеграции)
        run(http()
                        .server("restServer") // <------ server + id из citrus-context.xml
                        .receive() // <------ receive входящий запрос от теструемой системы
                        .get()// ---> Првоерка Get/Post/etc.. не работает из-за validation
                        .selector("citrus_http_method='POST' AND citrus_endpoint_uri='/v6/latest/USD'")
                        .validate((message, testContext) -> {
                            // Альтернативный способ валидации метода запроса, вместо selector
                            Assert.assertEquals(message.getHeaders().get("citrus_http_method"), "POST");
                            if (message.getPayload().toString()
                                    .contains(testContext.getVariable("accountNumber"))) {
                                received = true;
                            }
                        })
                // Способов валидации множество, один из них:
//                      .validate(jsonPath().expression("$.accountNumber", "@contains(95753174)@"))
        );

        // Если запрос действительно наш, о чём говорит флаг, присылаем ответ в тестируемую систему
        if (received) {
            run(http()
                    .server("restServer")
                    .send()
                    .response(HttpStatus.OK)
                    .message()
                    .contentType("application/json;charset=UTF-8")
                    .body("OK")
            );
        }
        // Валидация ответа клиенту на запрос, который был оптравлен первым шагом
        run(
                http()
                        .client("httpClient")
                        .receive()
                        .response()
                        .validate((message, testContext) -> {
                            String accountNumber = testContext.getVariable("accountNumber");
                            String accountNumberTestContext = contex.getVariable("accountNumber");
                            // accountNumberTestContext equals accountNumber
                            // Разница лишь в том, что testContext недоступен в другиз частях кода
                            Assert.assertTrue(message.getPayload().toString().contains(accountNumber));
                        })
        );
    }

    @CitrusTest
    public void testNegative() {
        variable("X-ID-Request", "citrus:randomNumber(11)");
        variable("accountNumber", "citrus:randomNumber(8)");

        // Отправка запроса от клиента к системе
        run(http() // <--------- http для работы с HTTP
                .client("httpClient") // <--------- client, так как отправка зпроса от клиента,  httpClient - id из citrus-context.xml
                .send() // <--------- send отправка запроса
                .post("api/v1/convert") // <--------- post запрос + path
                .message() // <--------- формируем сообщение
                .contentType("application/json") // <--------- contentType
                .header("X-ID-Request", "${X-ID-Request}") // <--------- ${X-ID-Request} подставляет значение
                .body("{\n" +
                        "   \"sortCode\":\"35-16-67\",\n" +
                        "   \"accountNumber\":\"${accountNumber}\",\n" +  // <--------- ${accountNumber} подставляет значение
                        "   \"sum\":\"100\",\n" +
                        "   \"currency\":\"USD\"\n" +
                        "}\n")
                .fork(false) // <---- fork false для негативного сценария, так как получаем ответ сразу, без доп шагов
        );

        run(http()
                .client("httpClient")
                .receive()
                .response(HttpStatus.NOT_ACCEPTABLE)
                .validate(jsonPath().expression("$.error", "@contains(${X-ID-Request})@"))
        );
    }

    @CitrusTest

    public void testNegativeUseBehavior(@Optional @CitrusResource TestContext contex) {
        variable("X-ID-Request", "citrus:randomNumber(11)");
        variable("accountNumber", "citrus:randomNumber(8)");

        //Тоже самое что и вдругих тестах, только логика завернута в TestBehavior
        // Один из возможных примеров
        run(applyBehavior(new HttpClientRequest(
                contex,
                false,
                "123",
                contex.getVariable("accountNumber"),
                "100",
                "USD"
        )));

        run(http()
                .client("httpClient")
                .receive()
                .response(HttpStatus.NOT_ACCEPTABLE)
                .validate(jsonPath().expression("$.error", "@contains(${X-ID-Request})@"))
        );
    }
}
