package com.jessin;

/**
 * @author zexin.guo
 * @create 2018-02-18 下午8:28
 **/
public class Monkey {
    private Hello.World world;
    private Hello hello ;
    public Monkey() {
        world = hello.new World() {
            @Override
            public int add(int a, int b) {
                return a + b;
            }
        };
    }
    public int add(int a, int b) {
        return world.add(a, b);
    }
    public static void main(String[] args) {
        System.out.println(new Monkey().add(1, 2));
    }
}
