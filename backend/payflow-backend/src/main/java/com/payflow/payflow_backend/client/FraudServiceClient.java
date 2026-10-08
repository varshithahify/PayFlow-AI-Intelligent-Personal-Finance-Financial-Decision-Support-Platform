package com.payflow.payflow_backend.client;

import com.payflow.payflow_backend.dto.FraudScoreRequest;
import com.payflow.payflow_backend.dto.FraudScoreResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "fraud-service",
        url = "${fraud.service.url}"
)
public interface FraudServiceClient {

    @PostMapping("/score")
    FraudScoreResponse score(@RequestBody FraudScoreRequest request);
}