package com.example.demo.leetcode.ii;

/**
 * Description: 64. 最小路径和
 * 给定一个包含非负整数的 m x n 网格 grid ，请找出一条从左上角到右下角的路径，使得路径上的数字总和为最小。
 * 说明：每次只能向下或者向右移动一步。
 *
 * @author Zeti
 * @date 2023/4/20 10:32
 */
public class DP_MinPathSum {
    public static void main(String[] args) {
        int[][] g1 = {{1,3,1}, {1,5,1}, {4,2,1}};   // 7
        System.err.println(minPathSum(g1));

        int[][] g2 = {{1,2,3}, {4,5,6}};   // 12
        System.err.println(minPathSum(g2));

    }

    // 1  3  1
    // 1  5  1
    // 4  2  1
    // 2  4  1
    public static int minPathSum(int[][] grid) {
        int m = grid.length;
        int n = grid[0].length;

        int[][] dp = new int[m][n];
        dp[0][0] = grid[0][0];

        for (int i = 1; i < dp.length; i++) {
            dp[i][0] = dp[i-1][0] + grid[i][0];
        }

        for (int j = 1; j < dp[0].length; j++) {
            dp[0][j] = dp[0][j-1] + grid[0][j];
        }

        for (int i = 1; i < m; i++) {
            for (int j = 1; j < n; j++) {
                dp[i][j] = Math.min(dp[i][j-1], dp[i-1][j]) + grid[i][j];
            }
        }
        return dp[m-1][n-1];
    }

}
