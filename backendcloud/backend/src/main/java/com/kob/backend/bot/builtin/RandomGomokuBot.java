package com.kob.backend.bot.builtin;

import java.util.List;
import java.util.Random;

public class RandomGomokuBot implements BuiltInGomokuBot {
    private final Random random = new Random();

    @Override
    public String name() {
        return "RandomBot";
    }

    @Override
    public String level() {
        return "入门";
    }

    @Override
    public Integer nextMove(int[][] board, int player) {
        List<GomokuBotSupport.Move> candidates = GomokuBotSupport.generateCandidates(board, player, 80);
        if (candidates.isEmpty()) return GomokuBotSupport.firstEmpty(board);
        GomokuBotSupport.Move move = candidates.get(random.nextInt(candidates.size()));
        return GomokuBotSupport.encode(move.row, move.col);
    }
}
