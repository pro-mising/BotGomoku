<template>
    <div class="container">
        <div class="row">
            <div class="col-3">
                <div class="card" style="margin-top: 20px">
                    <div class="card-body">
                        <img :src="$store.state.user.photo" alt="" style="width: 100%;">
                    </div>
                </div>
            </div>
            <div class="col-9">
                <div class="card" style="margin-top: 20px">
                    <div class="card-header">
                        <span style="font-size: 130%;">我的Bot</span>
                        <button type="button" class="btn btn-primary float-end" data-bs-toggle="modal" data-bs-target="#add-bot-btn">
                            创建Bot
                        </button>

                        <!-- Modal -->
                        <div class="modal fade" id="add-bot-btn" tabindex="-1">
                            <div class="modal-dialog modal-xl">
                                <div class="modal-content">
                                <div class="modal-header">
                                    <h1 class="modal-title fs-5" id="exampleModalLabel">创建Bot</h1>
                                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="关闭"></button>
                                </div>
                                <div class="modal-body">
                                    <div class="mb-3">
                                        <label for="add-bot-title" class="form-label">名称</label>
                                        <input v-model="botadd.title" type="text" class="form-control" id="exampleFormControlInput1" placeholder="请输入Bot名称">
                                    </div>
                                    <div class="mb-3">
                                        <label for="add-bot-description" class="form-label">简介</label>
                                        <textarea v-model="botadd.description" class="form-control" id="exampleFormControlTextarea1" rows="3" placeholder="请输入Bot简介"></textarea>
                                    </div>
                                    <div class="mb-3">
                                        <label for="add-bot-code" class="form-label">代码</label>
                                        <VAceEditor
                                            v-model:value="botadd.content"
                                            @init="editorInit"
                                            lang="java"
                                            theme="textmate"
                                            style="height: 300px" />
                                    </div>
                                </div>
                                <div class="modal-footer">
                                    <div class="error-message">{{ botadd.error_message }}</div>
                                    <button type="button" class="btn btn-primary" @click="add_bot">创建</button>
                                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
                                </div>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="card-body">
                        <table class="table table-striped table-hover">
                            <thead>
                                <tr>
                                    <th>Bot名称</th>
                                    <th>创建时间</th>
                                    <th>操作</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr v-for="bot in bots" :key="bot.id">
                                    <td>{{ bot.title }}</td>
                                    <td>{{ bot.createtime }}</td>
                                    <td> 
                                        <button type="button" class="btn btn-secondary" style="margin-right: 10px;" data-bs-toggle="modal" :data-bs-target="'#update-bot-modal-' + bot.id ">修改</button>
                                        <button type="button" class="btn btn-danger" @click="show_delete_modal(bot)">删除</button> 

                                        <!-- 删除确认模态框 -->
                                        <div class="modal fade" id="delete-confirm-modal" tabindex="-1" aria-labelledby="deleteConfirmLabel" aria-hidden="true">
                                            <div class="modal-dialog modal-dialog-centered">
                                                <div class="modal-content">
                                                <div class="modal-header">
                                                    <h5 class="modal-title" id="deleteConfirmLabel">确认删除</h5>
                                                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="关闭"></button>
                                                </div>
                                                <div class="modal-body">
                                                    你确定要删除这个 Bot 吗？
                                                </div>
                                                <div class="modal-footer">
                                                    <button type="button" class="btn btn-danger" @click="confirm_delete">确认删除</button>
                                                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
                                                </div>
                                                </div>
                                            </div>
                                        </div>

                                        <!--修改模态框-->
                                        <div class="modal fade" :id="'update-bot-modal-' + bot.id " tabindex="-1">
                                            <div class="modal-dialog modal-xl">
                                                <div class="modal-content">
                                                <div class="modal-header">
                                                    <h1 class="modal-title fs-5">修改Bot</h1>
                                                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="关闭"></button>
                                                </div>
                                                <div class="modal-body">
                                                    <div class="mb-3">
                                                        <label for="add-bot-title" class="form-label">名称</label>
                                                        <input v-model="bot.title" type="text" class="form-control" id="exampleFormControlInput1" placeholder="请输入Bot名称">
                                                    </div>
                                                    <div class="mb-3">
                                                        <label for="add-bot-description" class="form-label">简介</label>
                                                        <textarea v-model="bot.description" class="form-control" id="exampleFormControlTextarea1" rows="3" placeholder="请输入Bot简介"></textarea>
                                                    </div>
                                                    <div class="mb-3">
                                                        <label for="add-bot-code" class="form-label">代码</label>
                                                        <VAceEditor 
                                                            v-model:value="bot.content" 
                                                            @init="editorInit"
                                                            lang="java" 
                                                            theme="textmate" 
                                                            style="height: 300px" 
                                                            :options="{
                                                                enableBasicAutocompletion: true, // 启用基本自动完成
                                                                enableSnippets: true, // 启用代码片段
                                                                enableLiveAutocompletion: true, // 启用实时自动完成
                                                                fontSize: 18, // 设置字号
                                                                tabSize: 4, // 标签大小
                                                                showPrintMargin: false, // 去除编辑器里的竖线
                                                                highlightActiveLine: true, // 高亮当前行
                                                            }" 
                                                        />
                                                    </div>
                                                </div>
                                                <div class="modal-footer">
                                                    <div class="error-message">{{ bot.error_message }}</div>
                                                    <button type="button" class="btn btn-primary" @click="update_bot(bot)">保存修改</button>
                                                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
                                                </div>
                                                </div>
                                            </div>
                                        </div>
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
import { ref, reactive } from 'vue';
import $ from 'jquery'
import { useStore } from 'vuex';
import { Modal } from 'bootstrap/dist/js/bootstrap.min';
import { VAceEditor } from 'vue3-ace-editor';
import ace from 'ace-builds';
import 'ace-builds/src-noconflict/mode-java';
import 'ace-builds/src-noconflict/theme-textmate';

// 引入自动补全和代码片段
import 'ace-builds/src-noconflict/ext-language_tools';

export default {
    components: {
        VAceEditor
    },

    setup() {
        const store = useStore();
        let bots = ref([]);

        ace.config.set(
            "basePath",
            "https://cdn.jsdelivr.net/npm/ace-builds@" +
            require("ace-builds").version +
            "/src-noconflict/")

        const botadd = reactive({ //与后端进行交互
            title:"",
            description: "",
            content: `package com.kob.botrunningsystem.utils;

public class Bot implements com.kob.botrunningsystem.utils.BotInterface {
    private static final int SIZE = 15;

    @Override
    public Integer nextMove(String input) {
        String[] parts = input.split("#");
        String board = parts[0];
        int[][] g = new int[SIZE][SIZE];
        for (int i = 0, k = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++, k++) {
                g[i][j] = board.charAt(k) - '0';
            }
        }

        int center = SIZE / 2;
        if (g[center][center] == 0) return center * SIZE + center;

        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (g[i][j] == 0) return i * SIZE + j;
            }
        }
        return 0;
    }
}`,
            error_message: "",
        });

        const refresh_bots = () => {
            $.ajax({
                url: "http://127.0.0.1:3000/user/bot/getlist/",
                type: "get",
                headers: {
                    Authorization: "Bearer " + store.state.user.token,
                },
                success(resp) {
                    bots.value = resp; //resp是一个列表
                    console.log(resp);
                },
                error(err) {
                    console.error("请求失败", err); // 关键：这里可以看到错误信息
                }
            })
        }

        refresh_bots();  //定义完了需要执行一下

        const add_bot = () => {
            botadd.error_message = "",
            $.ajax({
                url: "http://127.0.0.1:3000/user/bot/add/",
                type: "POST",
                data: {
                    title: botadd.title,
                    description: botadd.description,
                    content: botadd.content,
                },
                headers: {
                    Authorization: "Bearer " + store.state.user.token,
                },
                success(resp) {
                    if(resp.error_message === "success") {
                        botadd.title = "";
                        botadd.description = "";
                        botadd.content = "";
                        Modal.getInstance("#add-bot-btn").hide();
                        refresh_bots();
                    } else {
                        botadd.error_message = resp.error_message;
                        console.log(botadd.error_message);
                    }
                }
            })
        }

        const update_bot = (bot) => {
            botadd.error_message = "",
            $.ajax({
                url: "http://127.0.0.1:3000/user/bot/update/",
                type: "POST",
                data: {
                    bot_id: bot.id,
                    title: bot.title,
                    description: bot.description,
                    content: bot.content,
                },
                headers: {
                    Authorization: "Bearer " + store.state.user.token,
                },
                success(resp) {
                    if(resp.error_message === "success") {
                        bot.title = "";
                        bot.description = "";
                        bot.content = "";
                        Modal.getInstance('#update-bot-modal-' + bot.id ).hide();
                        refresh_bots();
                    } else {
                        bot.error_message = resp.error_message;
                        console.log(bot.error_message);
                    }
                }
            })
        }

        const remove_bot = (bot) => {
            $.ajax({
                url: "http://127.0.0.1:3000/user/bot/remove/",
                type: "POST",
                data: {
                    bot_id: bot.id,
                },
                headers: {
                    Authorization: "Bearer " + store.state.user.token,
                },
                success(resp) {
                    if(resp.error_message === "success") {
                        refresh_bots();
                    }
                }
            })
        }

        const bot_to_delete = ref(null);  // 当前要删除的 bot

        const show_delete_modal = (bot) => {
            bot_to_delete.value = bot;
            const modal = new Modal(document.getElementById('delete-confirm-modal'));
            modal.show();
        };

        const confirm_delete = () => {
            if (!bot_to_delete.value) return;

            $.ajax({
                url: "http://127.0.0.1:3000/user/bot/remove/",
                type: "POST",
                data: {
                    bot_id: bot_to_delete.value.id,
                },
                headers: {
                    Authorization: "Bearer " + store.state.user.token,
                },
                success(resp) {
                    if (resp.error_message === "success") {
                        refresh_bots();
                        const modal = Modal.getInstance(document.getElementById('delete-confirm-modal'));
                        modal.hide();
                    }
                }
            });
        };


        return {
            refresh_bots,
            bots,
            botadd,
            add_bot,
            remove_bot,
            update_bot,
            show_delete_modal,
            confirm_delete,
        }
    }
}
</script>

<style scoped>
div.error-message {
    color: red;
}
</style>
