package xray;

import org.testng.Assert;
import org.testng.annotations.Test;
import xray.annotations.XrayTest;

public class AppTest {

    @XrayTest(key = "AUTO-2", labels = "testng dummy xray")
    @Test
    public void CanAddNumbers() {
        Assert.assertEquals((1 + 1), 2);
        Assert.assertEquals((-1 + 1), 0);
    }
}
