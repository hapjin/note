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



### 3 ThreadLocal

-  p257 ThreadLoal对象通常是由private static 修饰，因为都需要复制进入本地线程，所以非static 作用不大。如何理解？（我的理解是为了正确地发布 ThreadLocal 变量，参考《Java 并发编程实战》p43 安全发布对象的方式：1，在静态初始化函数中初始化一个对象引用；2，将对象引用保存到某个正确构造对象的final类型域中）
- p263 页 main方法代码，没有采用自定义线程池，因此不符合编码规范。