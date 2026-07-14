package com.kob.backend.bot.builtin;

public interface BuiltInGomokuBot {
    String name();

    String level();

    Integer nextMove(int[][] board, int player);
}
