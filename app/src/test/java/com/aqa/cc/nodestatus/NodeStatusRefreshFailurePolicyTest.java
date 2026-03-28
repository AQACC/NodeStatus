package com.aqa.cc.nodestatus;

import org.junit.Assert;
import org.junit.Test;

public class NodeStatusRefreshFailurePolicyTest {
    @Test
    public void classify_treats_configuration_errors_as_non_retryable() {
        Assert.assertEquals(
                NodeStatusRefreshFailurePolicy.Disposition.FAIL,
                NodeStatusRefreshFailurePolicy.classify(new IllegalStateException("bad config"))
        );
    }

    @Test
    public void classify_treats_http_401_as_non_retryable() {
        Assert.assertEquals(
                NodeStatusRefreshFailurePolicy.Disposition.FAIL,
                NodeStatusRefreshFailurePolicy.classify(new RuntimeException("Request failed with HTTP 401 for https://panel"))
        );
    }

    @Test
    public void classify_treats_http_429_as_retryable() {
        Assert.assertEquals(
                NodeStatusRefreshFailurePolicy.Disposition.RETRY,
                NodeStatusRefreshFailurePolicy.classify(new RuntimeException("Request failed with HTTP 429 for https://panel"))
        );
    }

    @Test
    public void classify_treats_unknown_runtime_failures_as_retryable() {
        Assert.assertEquals(
                NodeStatusRefreshFailurePolicy.Disposition.RETRY,
                NodeStatusRefreshFailurePolicy.classify(new RuntimeException("socket timeout"))
        );
    }
}
