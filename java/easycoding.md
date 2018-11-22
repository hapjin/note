p161页，ListToArray.java

```java
package net.hapjin.java;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class List2Array {
    public static void main(String[] args) {
        List<String> stringList = new ArrayList<>();
        stringList.add("1");
        stringList.add("2");
        stringList.add("3");

        String[] array2 = new String[2];
        stringList.toArray(array2);
        System.out.println(Arrays.asList(array2));

		
        String[] anotherArray2 = new String[2];
        anotherArray2 = stringList.toArray(anotherArray2);
        //[1,2,3]
        System.out.println(Arrays.asList(anotherArray2));

        String[] array3 = new String[3];
        stringList.toArray(array3);
        System.out.println(Arrays.asList(array3));

    }
}

```



## 2

 page124 "依然存活的对象会被移送到Survivor区, 所以这个区真是名副其实的存在"?名副其实的存在？表述有问题                    