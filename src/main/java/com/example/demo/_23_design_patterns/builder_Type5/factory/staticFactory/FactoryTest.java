package com.example.demo._23_design_patterns.builder_Type5.factory.staticFactory;

/**
 * 静态工厂方法模式
 * 将上面的多个工厂方法模式里的方法置为静态的，不需要创建实例，直接调用即可。
 */
public class FactoryTest {
    public static void main(String[] args) {
        SendFactory.produceSms().send();
    }
}
