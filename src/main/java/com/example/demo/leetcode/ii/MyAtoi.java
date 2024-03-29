package com.example.demo.leetcode.ii;

/**
 * Description: 8. 字符串转换整数 (atoi)
 * 请你来实现一个 myAtoi(string s) 函数，使其能将字符串转换成一个 32 位有符号整数（类似 C/C++ 中的 atoi 函数）。
 *
 * 函数 myAtoi(string s) 的算法如下：
 *
 * 读入字符串并丢弃无用的前导空格
 * 检查下一个字符（假设还未到字符末尾）为正还是负号，读取该字符（如果有）。 确定最终结果是负数还是正数。 如果两者都不存在，则假定结果为正。
 * 读入下一个字符，直到到达下一个非数字字符或到达输入的结尾。字符串的其余部分将被忽略。
 * 将前面步骤读入的这些数字转换为整数（即，"123" -> 123， "0032" -> 32）。如果没有读入数字，则整数为 0 。必要时更改符号（从步骤 2 开始）。
 * 如果整数数超过 32 位有符号整数范围 [−231,  231 − 1] ，需要截断这个整数，使其保持在这个范围内。具体来说，小于 −231 的整数应该被固定为 −231 ，大于 231 − 1 的整数应该被固定为 231 − 1 。
 * 返回整数作为最终结果。
 *
 * 注意：
 *
 * 本题中的空白字符只包括空格字符 ' ' 。
 * 除前导空格或数字后的其余字符串外，请勿忽略 任何其他字符。
 *
 * @author Zeti
 * @date 2024/1/30 14:12
 */
public class MyAtoi {
    public static void main(String[] args) {
        String s1 = "42";
        System.err.println(myAtoi(s1));

        String s2 = "   -42";
        System.err.println(myAtoi(s2));

        String s3 = "4193 with words";
        System.err.println(myAtoi(s3));

        String s4 = "-91283472332";
        System.err.println(myAtoi(s4));

        String s5 = "+-12";
        System.err.println(myAtoi(s5));

        String s6 = "+12";
        System.err.println(myAtoi(s6));

        String s7 = "-2147483648";
        System.err.println(myAtoi(s7));

    }

    public static int myAtoi(String s) {
        while (s.startsWith(" ")) {
            s = s.substring(1);
        }

        int flag = 1;
        long res = 0;
        for (int i = 0; i < s.length(); i++) {
            int c = s.charAt(i);
            if (i == 0 && (s.startsWith("-") || s.startsWith("+"))) {
                flag = s.startsWith("-") ? -1 : 1;
                continue;
            }

            if (c >= 48 && c <= 57) {
                res = res*10 + (c-48);
                if (res*flag <= Integer.MIN_VALUE) {
                    return Integer.MIN_VALUE;
                } else if (res*flag >= Integer.MAX_VALUE) {
                    return Integer.MAX_VALUE;
                }
            } else {
                return (int) res*flag;
            }
        }
        return (int) res*flag;
    }

}
