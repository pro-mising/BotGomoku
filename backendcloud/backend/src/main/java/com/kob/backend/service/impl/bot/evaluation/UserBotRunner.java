package com.kob.backend.service.impl.bot.evaluation;

import com.kob.botrunningsystem.utils.BotInterface;
import org.joor.Reflect;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class UserBotRunner {
    private static final long MOVE_TIMEOUT_MS = 1200;
    private final BotInterface bot;

    public UserBotRunner(String code) {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        String className = "com.kob.botrunningsystem.utils.Bot" + uid;
        String source = addUid(code, uid);
        this.bot = (BotInterface) Reflect.compile(className, source).create().get();
    }

    public MoveResult nextMove(String input) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Callable<Integer> callable = () -> bot.nextMove(input);
        Future<Integer> future = executor.submit(callable);
        try {
            Integer action = future.get(MOVE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            return MoveResult.success(action);
        } catch (Exception e) {
            future.cancel(true);
            return MoveResult.timeout();
        } finally {
            executor.shutdownNow();
        }
    }

    private String addUid(String code, String uid) {
        if (code == null) throw new IllegalArgumentException("Bot代码不能为空");
        int k = code.indexOf(" implements com.kob.botrunningsystem.utils.BotInterface");
        if (k == -1) throw new IllegalArgumentException("Bot代码必须实现 BotInterface");
        return code.substring(0, k) + uid + code.substring(k);
    }

    public static class MoveResult {
        public final Integer action;
        public final boolean timeout;

        private MoveResult(Integer action, boolean timeout) {
            this.action = action;
            this.timeout = timeout;
        }

        public static MoveResult success(Integer action) {
            return new MoveResult(action, false);
        }

        public static MoveResult timeout() {
            return new MoveResult(null, true);
        }
    }
}
