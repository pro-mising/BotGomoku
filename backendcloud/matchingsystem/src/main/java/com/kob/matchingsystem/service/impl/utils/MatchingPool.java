package com.kob.matchingsystem.service.impl.utils;

import com.kob.matchingsystem.client.BackendClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Component //为了能将spring里面的bean注入进来，要加一个Component,当要用到Autowired的时候就要加component
public class MatchingPool extends Thread { //是多线程的一个类，所以要继承自Thread
    private static final int RATING_DELTA_PER_SECOND = 10;
    private static final int FORCE_MATCH_WAITING_SECONDS = 5;
    private static List<Player> players = new ArrayList<Player>();
    private final ReentrantLock lock = new ReentrantLock();
    private static BackendClient backendClient;

    @Autowired
    public void setBackendClient(BackendClient backendClient) {
        MatchingPool.backendClient = backendClient;
    }

    public void addPlayer(Integer userId, Integer rating, Integer botId) {
        lock.lock();
        try {
            players.removeIf(player -> player.getUserId().equals(userId));
            players.add(new Player(userId, rating, botId,0));
        } finally {
            lock.unlock();
        }
    }

    public void removePlayer(Integer userId) {
        lock.lock();
        try {
            List<Player> newPlayer = new ArrayList<>();
            for (Player player : players) {
                if(!player.getUserId().equals(userId)){
                    newPlayer.add(player);
                }
            }
            players = newPlayer;
        } finally {
            lock.unlock();
        }
    }

    private void increaseWaitingTime() { //将所有当前玩家的等待时间 + 1
        for(Player player : players){
            player.setWaitingTime(player.getWaitingTime() + 1);
        }
    }

    private Boolean checkMatched(Player a, Player b) {  //判断两名玩家是否匹配
        int ratingDelta = Math.abs(a.getRating() - b.getRating());
        int waitingTime = Math.min(a.getWaitingTime(), b.getWaitingTime());
        return waitingTime >= FORCE_MATCH_WAITING_SECONDS || ratingDelta <= waitingTime * RATING_DELTA_PER_SECOND;
    }

    private void sendResult(Player a, Player b) {  //返回a和b的匹配结果
        System.out.println("send result: " + a + " " + b);
        backendClient.startGame(
                a.getUserId().toString(),
                a.getBotId().toString(),
                b.getUserId().toString(),
                b.getBotId().toString()
        );
    }

    private void matchPlayers() { //尝试匹配所有玩家
        System.out.println("match players" + players.toString());
        boolean[] used = new boolean[players.size()];
        for(int i = 0; i < players.size(); i++){
            if(used[i]) continue;
            for(int j =  i + 1; j < players.size(); j++){
                if(used[j]) continue;
                Player a = players.get(i);
                Player b = players.get(j);
                if(checkMatched(a, b)){
                    used[i] = used[j] = true;
                    sendResult(a, b);
                    break;
                }
            }
        }

        List<Player> newPlayers = new ArrayList<>();
        for(int i = 0; i < players.size(); i++){
            if(!used[i]) newPlayers.add(players.get(i));
        }

        players = newPlayers; //重新更新一下players
    }

    @Override
    public void run() {
        while(true){
            try {
                Thread.sleep(1000);
                lock.lock();
                try{ //保护的是同一份数据，所以要放在一起
                    increaseWaitingTime();
                    matchPlayers();
                } finally {
                    lock.unlock();
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
    }
}
