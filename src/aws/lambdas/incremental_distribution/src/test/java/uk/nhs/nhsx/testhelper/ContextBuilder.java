package uk.nhs.nhsx.testhelper;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

import java.nio.charset.StandardCharsets;

public class ContextBuilder {
    public static ContextBuilder context() {
        return new ContextBuilder();
    }

    public static Context aContext() {
        return new ContextBuilder().build();
    }

    public Context build() {
        return new TestContext();
    }

    public static class TestContext implements Context {
        @Override
        public String getAwsRequestId() {
            throw new UnsupportedOperationException("james didn't write");
        }

        @Override
        public String getLogGroupName() {
            return "test-log-group";
        }

        @Override
        public String getLogStreamName() {
            return "test-log-stream";
        }

        @Override
        public String getFunctionName() {
            return "function-name";
        }

        @Override
        public String getFunctionVersion() {
            return "function-version";
        }

        @Override
        public String getInvokedFunctionArn() {
            return "lambda-function-arn";
        }

        @Override
        public CognitoIdentity getIdentity() {
            return null;
        }

        @Override
        public ClientContext getClientContext() {
            throw new UnsupportedOperationException("james didn't write");
        }

        @Override
        public int getRemainingTimeInMillis() {
            return 1000;
        }

        @Override
        public int getMemoryLimitInMB() {
            return 100;
        }

        @Override
        public LambdaLogger getLogger() {
            return new LambdaLogger() {
                @Override
                public void log(String s) {
                    System.out.println("s = " + s);
                }

                @Override
                public void log(byte[] bytes) {
                    System.out.println("bytes = " + new String(bytes, StandardCharsets.UTF_8));
                }
            };
        }
    }
}
