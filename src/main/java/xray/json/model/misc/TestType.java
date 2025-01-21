package xray.json.model.misc;

import com.fasterxml.jackson.annotation.JsonValue;

public enum TestType {
    MANUAL("Manual"),
    GENERIC("Generic");

    @JsonValue
    public final String testType;

    TestType(String testType) {
        this.testType = testType;
    }
}