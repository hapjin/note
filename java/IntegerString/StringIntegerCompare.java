package net.hapjin.java;

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
        Integer integer1 = new Integer(1);
        Integer integer2 = new Integer(1);
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

    }
}
