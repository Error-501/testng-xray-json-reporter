package xray.json.model.misc;

public enum TestStatus {
    PASSED(1),
    FAILED(2),
    SKIPPED(3),
    EXECUTING(16),
    TODO(-1);
    //custom status...
    //KNOWN_FAILURE

    private final int testNgStatus;

    TestStatus(int testNgStatus) {
        this.testNgStatus = testNgStatus;
    }

    public static TestStatus getStatus(int curStatus) {
        for (TestStatus e : values()) {
            if (e.testNgStatus == curStatus) {
                return e;
            }
        }
        return null;
    }
}