package com.kob.botrunningsystem.service.impl.utils;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class BotPool extends Thread{
    //如果这里是空的，那么我们的线程会被阻塞住，如果这里不是空的，那么我们的线程会被唤醒
    //生产者-消费者模型，生产者不断给任务，消费者不断消耗任务
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();
    private final Queue<Bot> bots = new LinkedList<>(); //是一个常量

    //消息队列
    public void addBot(Integer userId, String botCode, String input){
        lock.lock();
        try{
//            System.out.println("addBot里面的borCode:" +botCode);
            bots.add(new Bot(userId, botCode, input));
            condition.signalAll(); //signal()是唤醒一个线程，signalAll是唤醒所有线程，但是这里只有一个线程，所以都一样
        } finally {
            lock.unlock();
        }
    }

    //只能编译java代码
    //如果要改进，可以先搜索java里面怎么执行终端命令，然后启动一个docker在docker里面执行，然后返回结果
    private void consume(Bot bot) {
        //每次执行代码都要开启一个新的线程，来操控他的执行时间，超时就会自动断开
        Consumer consumer = new Consumer();
        consumer.startTimeout(2000, bot);
    }

    @Override
    public void run() {
        while (true) {
            lock.lock();
            if(bots.isEmpty()){  //如果Bots是空的，要把当前线程阻塞住
                try {
                    condition.await(); //让当前线程阻塞住直到它被唤醒  这个函数自动包含一个锁释放的操作
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    lock.unlock();
                    break;
                }
            } else {
//                System.out.println("BotPool里面的bots" + bots.toString());
                Bot bot = bots.remove(); //返回并且删除队头
//                System.out.println("BootPool里面的bot:" + bot.getBotCode());
                lock.unlock();
                consume(bot); //这个函数比较耗时间，可能会执行几秒钟  因为是 在执行代码
            }
        }
    }
}
