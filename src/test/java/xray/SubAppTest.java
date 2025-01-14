package xray;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class SubAppTest {


    @DataProvider(name = "numbers")
    public static Object[][] evenNumbers() {
        return new Object[][]{{1, false}, {2, false}, {4, true}};
    }

    @Test(dataProvider = "numbers")
    public void CanSubNumbersDataProvider(Integer number, boolean expected) {
        assertEquals(expected, number % 2 == 0);
    }
}
