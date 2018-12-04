## 关于字符串与包装类的一些常识

```java
        //string intern pool
        String str1 = "a";
        String str2 = "a";

        System.out.println(str1.equals(str2));//true
        System.out.println(str1==str2);//true
        System.out.println(str1.hashCode());//97
        System.out.println(str2.hashCode());//97
```

- str1 和 str2 所指向的对象在字符串常量池中，是同一个对象。

  > All literal strings and string-valued constant expressions are interned，When the intern method is invoked, if the pool already contains a string equal to this {@code String} object as determined by the {@link #equals(Object)} method, then the string from the pool is returned.

String.intern()方法的注释：

```java
    /**
     * Returns a canonical representation for the string object.
     * <p>
     * A pool of strings, initially empty, is maintained privately by the
     * class {@code String}.
     * <p>
     * When the intern method is invoked, if the pool already contains a
     * string equal to this {@code String} object as determined by
     * the {@link #equals(Object)} method, then the string from the pool is
     * returned. Otherwise, this {@code String} object is added to the
     * pool and a reference to this {@code String} object is returned.
     * <p>
     * It follows that for any two strings {@code s} and {@code t},
     * {@code s.intern() == t.intern()} is {@code true}
     * if and only if {@code s.equals(t)} is {@code true}.
     * <p>
     * All literal strings and string-valued constant expressions are
     * interned. String literals are defined in section 3.10.5 of the
     * <cite>The Java&trade; Language Specification</cite>.
     *
     * @return  a string that has the same contents as this string, but is
     *          guaranteed to be from a pool of unique strings.
     * @jls 3.10.5 String Literals
     */
    public native String intern();
```



```java

        String str3 = "a";//string intern pool,constant pool
        String str4 = new String("a");//heap object
        System.out.println(str3.equals(str4));//true
        System.out.println(str3==str4);//false
```

- str3所指向的对象在常量池中，str4所指向的对象分配在堆上，== 基于对象的地址来判断，因此返回false

```java
        String str5 = "a";
        String str6 = String.valueOf("a");//string intern pool, constant pool
        System.out.println(str5.equals(str6));//true
        System.out.println(str5 == str6);//true
```

- 看String.valueOf的源码如下：

  ```java
      public static String valueOf(Object obj) {
          return (obj == null) ? "null" : obj.toString();
      }
  ```

- 参数 obj 是 "a"，也即是一个字符串对象，再看String.toString()方法源码如下：返回的是this 对象本身。因此，`System.out.println(str5 == str6);`输出true

  ```java
      /**
       * This object (which is already a string!) is itself returned.
       *
       * @return  the string itself.
       */
      public String toString() {
          return this;
      }
  ```

  ​

### Integer

```java
        Integer integer1 = new Integer(1);// heap obj
        Integer integer2 = new Integer(1);//another heap obj
        System.out.println(integer1.equals(integer2));//true
        System.out.println(integer1 == integer2);//false
```

- 采用关键字 new 创建对象，integer1 和 integer2 是堆上两个不同的对象，因此`System.out.println(integer1 == integer2)`输出false

- JDK9已经不推荐使用 new 来创建 Integer对象，因为new没有使用缓存，有性能问题。

  ```java
      /**
       * Constructs a newly allocated {@code Integer} object that
       * represents the specified {@code int} value.
       *
       * @param   value   the value to be represented by the
       *                  {@code Integer} object.
       *
       * @deprecated
       * It is rarely appropriate to use this constructor. The static factory
       * {@link #valueOf(int)} is generally a better choice, as it is
       * likely to yield significantly better space and time performance.
       */
      @Deprecated(since="9")
      public Integer(int value) {
          this.value = value;
      }
  ```

  ​

```java
        Integer integer3 = 1;
        Integer integer4 = 1;
        System.out.println(integer3.equals(integer4));//true
        System.out.println(integer3==integer4);//true
```

- 包装类Integer对范围-128~127之间的数据进行了缓存。直接赋值方式使得：integer3 和 integer4 指向的是同一个对象。

  ```java
   /**
       * Cache to support the object identity semantics of autoboxing for values between
       * -128 and 127 (inclusive) as required by JLS.
       *
       * The cache is initialized on first usage.  The size of the cache
       * may be controlled by the {@code -XX:AutoBoxCacheMax=<size>} option.
       * During VM initialization, java.lang.Integer.IntegerCache.high property
       * may be set and saved in the private system properties in the
       * jdk.internal.misc.VM class.
       */

      private static class IntegerCache {
          static final int low = -128;
          static final int high;
          static final Integer cache[];
  ```

  ​

```java
        Integer integer5 = 128;
        Integer integer6 = 128;
        System.out.println(integer5.equals(integer6));//true
        System.out.println(integer5 == integer6);//false
```

- 128 超过了默认的缓存范围，因此`System.out.println(integer5 == integer6)`输出false



```java
        Integer integer7 = 127;
        Integer integer8 = Integer.valueOf(127);
        System.out.println(integer7.equals(integer8));//true
        System.out.println(integer7 == integer8);//true
```

- 127在缓存范围之内，Integer.valueOf()方法返回的是缓存的对象，因此 integer7 和 integer8 指向的是同一个对象，故`System.out.println(integer7 == integer8)`输出true

- Integer.valueOf源码如下：

  ```java
      /**
       * Returns an {@code Integer} instance representing the specified
       * {@code int} value.  If a new {@code Integer} instance is not
       * required, this method should generally be used in preference to
       * the constructor {@link #Integer(int)}, as this method is likely
       * to yield significantly better space and time performance by
       * caching frequently requested values.
       *
       * This method will always cache values in the range -128 to 127,
       * inclusive, and may cache other values outside of this range.
       *
       * @param  i an {@code int} value.
       * @return an {@code Integer} instance representing {@code i}.
       * @since  1.5
       */
      @HotSpotIntrinsicCandidate
      public static Integer valueOf(int i) {
          if (i >= IntegerCache.low && i <= IntegerCache.high)
              return IntegerCache.cache[i + (-IntegerCache.low)];
          return new Integer(i);
      }
  ```

  ​

```java
        Integer integer9 = 128;
        Integer integer10 = Integer.valueOf(128);
        System.out.println(integer9.equals(integer10));//true
        System.out.println(integer9 == integer10);//false
```

- 128超过了默认缓存范围，因此`System.out.println(integer9 == integer10)`输出false

写了这么多并不是说要记住每种情况，而是要了解以下几个点：

1. String 字符串常量池，字面量都存储在常量池中。
2. == 比较 与equals比较区别。
3. equals比较与hashCode方法的联系。如果两个对象equals返回true，那么这两个对象的hashCode肯定是相同的；如果两个对象的equals返回false，两个对象的hashCode可以是相同的，hashCode相同意味着发生了冲突，就是常讨论的HashMap 的Key 冲突的情形。
4. 基本数据类型有对应的包装类，包装类有数据缓存的功能，用对了地方能提升性能。
5. 明白 = 赋值创建对象 和 new 创建对象的区别，new一个对象时，对象会分配在堆中。
6. 了解JDK基础类的常用方法的底层实现源码
7. equals比较与hashCode方法的联系

完整代码：

```java
public class StringIntegerCompare {

    public static void main(String[] args) {
        //string intern pool
        String str1 = "a";
        String str2 = "a";

        System.out.println(str1.equals(str2));//true
        System.out.println(str1==str2);//true
        System.out.println(str1.hashCode());//97
        System.out.println(str2.hashCode());//97

        String str3 = "a";//string intern pool,constant pool
        String str4 = new String("a");//heap object
        System.out.println(str3.equals(str4));//true
        System.out.println(str3==str4);//false
        System.out.println(str3.hashCode());//true
        System.out.println(str4.hashCode());//true

        String str5 = "a";
        String str6 = String.valueOf("a");//string intern pool, constant pool
        System.out.println(str5.equals(str6));//true
        System.out.println(str5 == str6);//true
        System.out.println(str5.hashCode());//97
        System.out.println(str6.hashCode());//97

        String str7 = new String("b");//string object on heap
        String str8 = String.valueOf(str7);
        System.out.println(str7.equals(str8));//true
        System.out.println(str7 == str8);//true
        System.out.println(str7.hashCode());
        System.out.println(str8.hashCode());

        System.out.println("-----------------------");
        Integer integer1 = new Integer(1);// heap obj
        Integer integer2 = new Integer(1);//another heap obj
        System.out.println(integer1.equals(integer2));//true
        System.out.println(integer1 == integer2);//false
        System.out.println(integer1.hashCode());//1
        System.out.println(integer2.hashCode());//1

        Integer integer3 = 1;
        Integer integer4 = 1;
        System.out.println(integer3.equals(integer4));//true
        System.out.println(integer3==integer4);//true
        System.out.println(integer3.hashCode());//1
        System.out.println(integer4.hashCode());//1

        Integer integer5 = 128;
        Integer integer6 = 128;
        System.out.println(integer5.equals(integer6));//true
        System.out.println(integer5 == integer6);//false
        System.out.println(integer5.hashCode());//128
        System.out.println(integer6.hashCode());//128

        Integer integer7 = 127;
        Integer integer8 = Integer.valueOf(127);
        System.out.println(integer7.equals(integer8));//true
        System.out.println(integer7 == integer8);//true

        Integer integer9 = 128;
        Integer integer10 = Integer.valueOf(128);
        System.out.println(integer9.equals(integer10));//true
        System.out.println(integer9 == integer10);//false
    }
}

```

JDK版本：

>panda@panda-e550:~$ java -version
>java version "10.0.2" 2018-07-17
>Java(TM) SE Runtime Environment 18.3 (build 10.0.2+13)
>Java HotSpot(TM) 64-Bit Server VM 18.3 (build 10.0.2+13, mixed mode)

