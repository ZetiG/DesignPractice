package com.example.demo.leetcode.ii;

import java.util.Arrays;

/**
 * Description: 287. 寻找重复数
 * 给定一个包含n + 1 个整数的数组nums ，其数字都在[1, n]范围内（包括 1 和 n），可知至少存在一个重复的整数。
 * 假设 nums 只有 一个重复的整数 ，返回这个重复的数 。
 * 你设计的解决方案必须 不修改 数组 nums 且只用常量级 O(1) 的额外空间。
 *
 * 来源：力扣（LeetCode）
 * 链接：https://leetcode.cn/problems/find-the-duplicate-number
 * 著作权归领扣网络所有。商业转载请联系官方授权，非商业转载请注明出处。
 *
 * @author Zeti
 * @date 2023/5/8 15:26
 */
public class FindDuplicate {
    public static void main(String[] args) {
        int[] n1 = {1,3,4,2,2};
        System.err.println(findDuplicate(n1));  // 2

        int[] n2 = {3,1,3,4,2};
        System.err.println(findDuplicate(n2));  // 3

    }


    // 快慢指针
    public static int findDuplicate(int[] nums) {
        int slow = 0, fast = 0;
        do {
            slow = nums[slow];
            fast = nums[nums[fast]];
        } while (slow != fast);

        slow = 0;
        while (slow != fast) {
            slow = nums[slow];
            fast = nums[fast];
        }
        return slow;
    }


    // 暴力循环<但是！题目要求不修改原数组，这里重排序了！pass!>，nums[n+1] - nums[n]
    public int findDuplicate_1(int[] nums) {
        Arrays.sort(nums);
        int res = nums[0];
        for (int i = 1; i < nums.length; ++i) {
            if (nums[i] == res) {
                break;
            } else {
                res = nums[i];
            }
        }
        return res;
    }

}
