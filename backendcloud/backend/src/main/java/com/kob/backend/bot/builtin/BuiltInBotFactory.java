package com.kob.backend.bot.builtin;

import java.util.List;

public class BuiltInBotFactory {
    public static List<BuiltInGomokuBot> evaluationBots() {
        return List.of(
                new RandomGomokuBot(),
                new GreedyGomokuBot(),
                new PatternGomokuBot(),
                new AlphaBetaBuiltInBot()
        );
    }
}
