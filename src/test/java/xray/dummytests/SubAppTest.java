package xray.dummytests;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class SubAppTest {


    @DataProvider(name = "numbers")
    public static Object[][] evenNumbers() {
        return new Object[][]{{1, true}, {2, true}, {4, false}};
    }

    @Test(dataProvider = "numbers", groups = {"AddNumbers_UT"})
    public void CanSubNumbersDataProvider(int number, boolean expected) throws InterruptedException {
        Thread.sleep(1000);
        assertEquals(number % 2 == 0, expected);
    }
}