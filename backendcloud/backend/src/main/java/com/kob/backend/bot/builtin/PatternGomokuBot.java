package com.kob.backend.bot.builtin;

import java.util.List;

public class PatternGomokuBot implements BuiltInGomokuBot {
    @Override
    public String name() {
        return "PatternBot";
    }

    @Override
    public String level() {
        return "中等";
    }

    @Override
    public Integer nextMove(int[][] board, int player) {
        Integer win = GomokuBotSupport.findImmediateMove(board, player);
        if (win != null) return win;
        Integer block = GomokuBotSupport.findImmediateMove(board, GomokuBotSupport.opponent(player));
        if (block != null) return block;
        List<GomokuBotSupport.Move> candidates = GomokuBotSupport.generateCandidates(board, player, 8);
        if (candidates.isEmpty()) return GomokuBotSupport.firstEmpty(board);
        GomokuBotSupport.Move move = candidates.get(0);
        return GomokuBotSupport.encode(move.row, move.col);
    }
}
