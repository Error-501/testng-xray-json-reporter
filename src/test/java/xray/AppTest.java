package xray;

import org.testng.Assert;
import org.testng.annotations.Test;
import xray.annotations.XrayTest;

import java.util.ArrayList;
import java.util.Arrays;

public class AppTest {

    @XrayTest(description = "xray-description", labels = {"testng", "dummy", "xray"})
    @Test(groups = {"test", "SubNumbers_UT"}, description = "Test to sub 2 numbers")
    public void CanAddNumbers() {
        Assert.assertEquals((1 + 1), 2);
        Assert.assertEquals((-1 + 1), 0);
    }
}