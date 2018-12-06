package java8;

public class NotInit {
    public static void main(String[] args) {
        //不会触发SuperClass的类的初始化
        System.out.println(SubClass.a);
    }
}
