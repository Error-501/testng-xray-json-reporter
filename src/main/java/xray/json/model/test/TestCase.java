package xray.json.model.test;

import java.util.ArrayList;
import xray.json.model.misc.TestType;

public class TestCase {
        private String projectKey;
        private String summary;
        private TestType type;
        private String requirementKeys;
        private String labels;
        private ArrayList<TestStep> steps;
        private String definition;
}
