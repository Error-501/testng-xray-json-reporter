package xray.dummytests;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import xray.annotations.XrayTest;

public class AppTest {

    @DataProvider(name = "numbers")
    public static Object[][] evenNumbers() {
        return new Object[][]{{1, 2}, {2, 3}, {4, 5}};
    }

    @XrayTest(description = "xray-description", labels = {"testng", "dummy", "xray"})
    @Test(groups = {"test", "SubNumbers_UT"}, description = "Test to sub 2 numbers", dataProvider = "numbers")
    public void CanAddNumbers(int number, int expected) {
        Assert.assertEquals((1 + number), expected);
    }
}