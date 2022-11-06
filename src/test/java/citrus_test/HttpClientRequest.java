package citrus_test;

import com.consol.citrus.TestActionRunner;
import com.consol.citrus.TestBehavior;
import com.consol.citrus.context.TestContext;
import com.consol.citrus.message.builder.ObjectMappingPayloadBuilder;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.hu.Ha;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static com.consol.citrus.actions.FailAction.Builder.fail;
import static com.consol.citrus.http.actions.HttpActionBuilder.http;

public class HttpClientRequest implements TestBehavior {

    TestContext context;

    private boolean fork;
    private String sortCode;
    private String accountNumber;
    private String sum;
    private String currency;

    public HttpClientRequest(TestContext context, boolean fork, String sortCode, String accountNumber, String sum, String currency) {
        this.context = context;
        this.fork = fork;
        this.sortCode = sortCode;
        this.accountNumber = accountNumber;
        this.sum = sum;
        this.currency = currency;
    }

    @Override
    public void apply(TestActionRunner testActionRunner) {
        testActionRunner.run(http()
                .client("httpClient")
                .send()
                .post("api/v1/convert")
                .message()
                .contentType("application/json")
                .header("X-ID-Request", "${X-ID-Request}")
                .body(generateBody(testActionRunner))
                .fork(fork)
        );
    }

    private String generateBody(TestActionRunner testActionRunner) {
        HashMap<String,String> request = new HashMap<>();
        request.put("sortCode", sortCode);
        request.put("accountNumber", accountNumber);
        request.put("sum", sum);
        request.put("currency", currency);

        for(Map.Entry<String, String> entry : request.entrySet()) {
            context.setVariable(entry.getKey(), entry.getValue());
        }

        try {
            return new ObjectMapper()
                    .writeValueAsString(request);
        } catch (JsonProcessingException e) {
            testActionRunner
                    .run(fail("This test can`t generate request"));
            e.printStackTrace();
        }

        // Один из вариантов подготовить модель заранее использовать так:
        //return new ObjectMappingPayloadBuilder(new TodoEntry(uuid, "${todoName}", "${todoDescription}"), objectMapper));

        //Так же можно указать сразу путь к ресурсу
        // new ClassPathResource("templates/todo.json")
        // Удобно тем, что todo.json может сразу содержать переменные ${todoName}
        // Правда на мой взгляд не достаточно гибкости при таком подходе.
        // Примеры смотреть в https://github.com/citrusframework/citrus-samples/tree/main/samples-json
        return null;
    }
}
