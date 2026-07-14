package com.kob.backend.bot.builtin;

import com.kob.backend.bot.alphabeta.AlphaBetaBot;

public class AlphaBetaBuiltInBot implements BuiltInGomokuBot {
    private final AlphaBetaBot bot = new AlphaBetaBot();

    @Override
    public String name() {
        return "AlphaBetaBot";
    }

    @Override
    public String level() {
        return "高级";
    }

    @Override
    public Integer nextMove(int[][] board, int player) {
        return bot.nextMove(board, player);
    }
}
