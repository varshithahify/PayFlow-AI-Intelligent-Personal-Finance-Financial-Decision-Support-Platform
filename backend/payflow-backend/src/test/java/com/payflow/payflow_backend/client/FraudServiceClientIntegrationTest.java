package com.payflow.payflow_backend.client;

import com.payflow.payflow_backend.dto.FraudScoreRequest;
import com.payflow.payflow_backend.dto.FraudScoreResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        classes = FraudServiceClientIntegrationTest.TestApplication.class,
        properties = {
                "fraud.service.url=http://localhost:8083"
        }
)
class FraudServiceClientIntegrationTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableFeignClients(clients = FraudServiceClient.class)
    static class TestApplication {
    }

    @Autowired
    private FraudServiceClient fraudServiceClient;

    @Test
    void shouldCommunicateWithPythonFraudService() {

        FraudScoreRequest request = new FraudScoreRequest();

        request.setTransactionAmt(250.0);
        request.setCard1(1001.0);
        request.setCard2(200.0);
        request.setAddr1(100.0);
        request.setDist1(10.0);
        request.setC1(1.0);
        request.setC2(2.0);
        request.setV1(0.0);
        request.setV2(1.0);
        request.setV3(2.0);

        FraudScoreResponse response =
                fraudServiceClient.score(request);

        assertNotNull(response);
        assertTrue(response.getScore() >= 0);
        assertNotNull(response.getAction());
        assertTrue(response.getMlScore() >= 0);
        assertTrue(response.getVelocityScore() >= 0);
        assertNotNull(response.getTriggeredRules());

        System.out.println("========== FEIGN INTEGRATION RESULT ==========");
        System.out.println("Score: " + response.getScore());
        System.out.println("Action: " + response.getAction());
        System.out.println("ML Score: " + response.getMlScore());
        System.out.println("Velocity Score: " + response.getVelocityScore());
        System.out.println("Triggered Rules: " + response.getTriggeredRules());
    }
}