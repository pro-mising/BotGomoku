<template>
    <div class="matchground">
        <section class="match-panel">
            <header class="match-header">
                <div>
                    <p class="eyebrow">实时匹配</p>
                    <h1>五子棋匹配</h1>
                    <span>选择亲自出马或派出 Bot，点击按钮进入匹配队列</span>
                </div>
                <div :class="['status-pill', isMatching ? 'matching' : 'ready']">
                    <i></i>
                    {{ isMatching ? "匹配中" : "准备就绪" }}
                </div>
            </header>

            <div class="arena-preview">
                <div class="player-preview">
                    <img :src="$store.state.user.photo" alt="我的头像">
                    <strong>{{ $store.state.user.username }}</strong>
                    <span>{{ selectedEntryLabel }}</span>
                </div>

                <div class="vs-core">
                    <span class="stone black"></span>
                    <strong>VS</strong>
                    <span class="stone white"></span>
                </div>

                <div class="player-preview">
                    <img :src="$store.state.pk.opponent_photo" alt="对手头像">
                    <strong>{{ opponentName }}</strong>
                    <span>{{ isMatching ? "寻找中" : "等待匹配" }}</span>
                </div>
            </div>

            <div class="match-control">
                <div class="control-copy">
                    <h2>出战方式</h2>
                    <p>{{ select_bot === "-1" ? "亲自落子，与在线玩家进行实时五子棋对局。" : "派出选中的 Bot，由代码自动完成落子决策。" }}</p>
                </div>

                <div class="user-select-bot">
                    <label for="match-bot-select">选择出战方式</label>
                    <select
                        id="match-bot-select"
                        v-model="select_bot"
                        class="form-select"
                        aria-label="选择出战方式"
                        :disabled="isMatching"
                    >
                        <option value="-1">亲自出马</option>
                        <option v-for="bot in bots" :key="bot.id" :value="bot.id">
                            {{ bot.title }}
                        </option>
                    </select>
                </div>

                <div class="selected-summary">
                    <span>当前选择</span>
                    <strong>{{ selectedEntryLabel }}</strong>
                </div>

                <button
                    type="button"
                    :class="['match-btn', isMatching ? 'cancel' : 'primary']"
                    :disabled="!isMatching && !socketReady"
                    @click="click_match_btn"
                >
                    {{ !isMatching && !socketReady ? "连接中..." : match_btn_info }}
                </button>
            </div>

            <div class="quick-links">
                <router-link :to="{name: 'user_bot_index'}">管理我的 Bot</router-link>
                <router-link :to="{name: 'bot_evaluation'}">进入 Bot 测试</router-link>
                <router-link :to="{name: 'record_index'}">查看对局列表</router-link>
            </div>
        </section>
    </div>
</template>

<script>
import { computed, ref } from "vue";
import { useStore } from "vuex";
import $ from "jquery";

export default {
    setup() {
        const store = useStore();
        const START_MATCH_TEXT = "开始匹配";
        const CANCEL_MATCH_TEXT = "取消匹配";
        const match_btn_info = ref(START_MATCH_TEXT);
        const bots = ref([]);
        const select_bot = ref("-1");

        const isMatching = computed(() => match_btn_info.value !== START_MATCH_TEXT);
        const socketReady = computed(() => (
            store.state.pk.socket &&
            store.state.pk.socket.readyState === WebSocket.OPEN
        ));

        const opponentName = computed(() => store.state.pk.opponent_username || "我的对手");

        const selectedEntryLabel = computed(() => {
            if (select_bot.value === "-1") return "亲自出马";
            const bot = bots.value.find(item => String(item.id) === String(select_bot.value));
            return bot ? bot.title : "已选择 Bot";
        });

        const click_match_btn = () => {
            if (match_btn_info.value === START_MATCH_TEXT) {
                if (!socketReady.value) return;
                try {
                    store.state.pk.socket.send(JSON.stringify({
                        event: "start-matching",
                        bot_id: select_bot.value,
                    }));
                    match_btn_info.value = CANCEL_MATCH_TEXT;
                } catch (err) {
                    console.error("发送匹配请求失败", err);
                    match_btn_info.value = START_MATCH_TEXT;
                }
            } else {
                try {
                    if (socketReady.value) {
                        store.state.pk.socket.send(JSON.stringify({
                            event: "stop-matching",
                        }));
                    }
                } catch (err) {
                    console.error("发送取消匹配请求失败", err);
                } finally {
                    match_btn_info.value = START_MATCH_TEXT;
                }
            }
        };

        const refresh_bots = () => {
            $.ajax({
                url: "http://127.0.0.1:3000/user/bot/getlist/",
                type: "get",
                headers: {
                    Authorization: "Bearer " + store.state.user.token,
                },
                success(resp) {
                    bots.value = resp || [];
                },
                error(err) {
                    console.error("获取 Bot 列表失败", err);
                }
            });
        };

        refresh_bots();

        return {
            match_btn_info,
            click_match_btn,
            bots,
            select_bot,
            isMatching,
            socketReady,
            opponentName,
            selectedEntryLabel,
        };
    }
};
</script>

<style scoped>
.matchground {
    width: min(980px, calc(100vw - 48px));
    margin: 40px auto;
}

.match-panel {
    padding: 34px;
    border: 1px solid rgba(232, 219, 199, 0.92);
    border-radius: 14px;
    background: rgba(250, 248, 243, 0.96);
    box-shadow: 0 20px 54px rgba(35, 25, 18, 0.18);
}

.match-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 24px;
    padding-bottom: 22px;
    border-bottom: 1px solid #eadfce;
}

.eyebrow {
    margin: 0 0 4px;
    color: #c68121;
    font-size: 13px;
    font-weight: 800;
    letter-spacing: 0;
}

.match-header h1 {
    margin: 0;
    color: #2f2925;
    font-size: 34px;
    font-weight: 900;
}

.match-header span {
    display: inline-block;
    margin-top: 6px;
    color: #7a7269;
    font-size: 15px;
}

.status-pill {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    flex: 0 0 auto;
    margin-top: 6px;
    padding: 10px 16px;
    border-radius: 999px;
    background: #f4efe8;
    color: #746a60;
    font-weight: 800;
}

.status-pill i {
    width: 9px;
    height: 9px;
    border-radius: 50%;
    background: #3fc279;
}

.status-pill.matching i {
    background: #d89a3c;
    animation: pulse 1s infinite;
}

.arena-preview {
    display: grid;
    grid-template-columns: 1fr auto 1fr;
    align-items: center;
    gap: 22px;
    padding: 30px 0 22px;
}

.player-preview {
    display: grid;
    justify-items: center;
    gap: 9px;
    min-width: 0;
}

.player-preview img {
    width: 124px;
    height: 124px;
    border-radius: 50%;
    object-fit: cover;
    border: 5px solid #f2d59d;
    box-shadow: 0 16px 34px rgba(72, 47, 23, 0.18);
}

.player-preview strong {
    max-width: 190px;
    overflow: hidden;
    color: #2f2925;
    font-size: 22px;
    font-weight: 900;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.player-preview span {
    color: #6f7e6b;
    font-size: 13px;
    font-weight: 700;
}

.vs-core {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
    min-width: 156px;
    padding: 12px 18px;
    border-radius: 999px;
    background: #fff8ed;
    border: 1px solid #eadcc8;
    color: #2f2925;
}

.vs-core strong {
    font-size: 22px;
    font-weight: 900;
}

.stone {
    display: inline-block;
    width: 22px;
    height: 22px;
    border-radius: 50%;
}

.stone.black {
    background: radial-gradient(circle at 30% 25%, #626262, #111 64%, #000);
    box-shadow: 0 3px 8px rgba(0, 0, 0, 0.28);
}

.stone.white {
    background: radial-gradient(circle at 30% 25%, #ffffff, #e7e7e7 70%, #c9c9c9);
    box-shadow: 0 3px 8px rgba(0, 0, 0, 0.18);
}

.match-control {
    display: grid;
    gap: 12px;
    max-width: 470px;
    margin: 0 auto;
    padding: 16px 18px;
    border: 1px solid #eadfce;
    border-radius: 12px;
    background: rgba(255, 253, 248, 0.94);
    box-shadow: 0 14px 34px rgba(58, 42, 28, 0.08);
}

.control-copy h2 {
    margin: 0 0 4px;
    color: #2f2925;
    font-size: 19px;
    font-weight: 900;
}

.control-copy p {
    margin: 0;
    color: #7a7269;
    font-size: 13px;
}

.user-select-bot {
    display: grid;
    gap: 6px;
}

.user-select-bot label {
    color: #5f554c;
    font-size: 13px;
    font-weight: 800;
}

.user-select-bot select {
    min-height: 40px;
    border-color: #eadcc8;
    color: #2f2925;
    font-weight: 700;
}

.user-select-bot select:focus {
    border-color: #d89a3c;
    box-shadow: 0 0 0 0.2rem rgba(216, 154, 60, 0.18);
}

.selected-summary {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    padding: 10px 12px;
    border-radius: 10px;
    background: #f8f1e7;
}

.selected-summary span {
    color: #8c8177;
    font-size: 13px;
    font-weight: 700;
}

.selected-summary strong {
    min-width: 0;
    overflow: hidden;
    color: #2f2925;
    font-size: 15px;
    font-weight: 900;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.match-btn {
    width: 100%;
    min-height: 44px;
    border: 0;
    border-radius: 10px;
    font-size: 16px;
    font-weight: 900;
    transition: transform 0.18s ease, box-shadow 0.18s ease, background 0.18s ease;
}

.match-btn:hover {
    transform: translateY(-1px);
}

.match-btn:disabled {
    background: #e5ded3;
    color: #9a9187;
    cursor: not-allowed;
    box-shadow: none;
    transform: none;
}

.match-btn.primary {
    background: #d89a3c;
    color: #2c2116;
    box-shadow: 0 12px 22px rgba(216, 154, 60, 0.25);
}

.match-btn.cancel {
    background: #2f2925;
    color: #fff8ed;
    box-shadow: 0 12px 22px rgba(47, 41, 37, 0.18);
}

.quick-links {
    display: flex;
    justify-content: center;
    gap: 12px;
    flex-wrap: wrap;
    margin-top: 22px;
    padding-top: 18px;
    border-top: 1px solid #eadfce;
}

.quick-links a {
    padding: 8px 14px;
    border-radius: 999px;
    background: #f5eee4;
    color: #6f6257;
    font-size: 13px;
    font-weight: 800;
    text-decoration: none;
}

.quick-links a:hover {
    color: #9a6415;
    background: #fff1dc;
}

@keyframes pulse {
    0% {
        box-shadow: 0 0 0 0 rgba(216, 154, 60, 0.36);
    }
    70% {
        box-shadow: 0 0 0 8px rgba(216, 154, 60, 0);
    }
    100% {
        box-shadow: 0 0 0 0 rgba(216, 154, 60, 0);
    }
}

@media (max-width: 768px) {
    .matchground {
        width: min(100% - 24px, 680px);
        margin: 24px auto;
    }

    .match-panel {
        padding: 24px;
    }

    .match-header {
        display: grid;
    }

    .arena-preview {
        grid-template-columns: 1fr;
        gap: 16px;
    }

    .player-preview img {
        width: 96px;
        height: 96px;
    }

    .vs-core {
        justify-self: center;
        min-width: 160px;
    }

    .match-control {
        max-width: 100%;
        padding: 16px;
    }
}
</style>
