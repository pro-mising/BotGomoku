<template>
    <div class="result-backdrop">
        <div class="result-board">
            <div class="result-kicker">{{ winnerText }}</div>
            <div class="result-board-text">{{ resultText }}</div>
            <div class="result-detail">{{ detailText }}</div>
            <div class="result-countdown">{{ countdown }} 秒后返回匹配首页</div>
            <div class="result-board-btn">
                <button @click="restart" type="button" class="btn btn-warning btn-lg">
                    立即返回
                </button>
            </div>
        </div>
    </div>
</template>

<script>
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { useStore } from 'vuex';

export default {
    setup () {
        const store = useStore();
        const countdown = ref(3);
        let timer = null;

        const mySide = computed(() => {
            if (Number(store.state.pk.a_id) === Number(store.state.user.id)) return "A";
            if (Number(store.state.pk.b_id) === Number(store.state.user.id)) return "B";
            return "";
        });

        const winner = computed(() => {
            if (store.state.pk.winner && store.state.pk.winner !== "none") return store.state.pk.winner;
            if (store.state.pk.loser === "all") return "all";
            return store.state.pk.loser === "A" ? "B" : "A";
        });

        const resultText = computed(() => {
            if (winner.value === "all") return "平局";
            return winner.value === mySide.value ? "你赢了" : "你输了";
        });

        const winnerText = computed(() => {
            if (winner.value === "all") return "棋盘已满，双方平局";
            return winner.value === "A" ? "黑子获胜" : "白子获胜";
        });

        const detailText = computed(() => {
            if (store.state.pk.result_reason === "timeout") return "一方超时未落子，系统判负。";
            if (store.state.pk.result_reason === "invalid-move") return "一方落在非法位置，系统判负。";
            if (winner.value === "all") return "双方没有形成五连。";
            return "横向、纵向或任意斜向连续 5 个同色棋子，即可获胜。";
        });

        const restart = () => {
            if (timer) {
                clearInterval(timer);
                timer = null;
            }
            store.commit("updateStatus", "matching");
            store.commit("resetResult");
            store.commit("updateOpponent", {
                username: "我的对手",
                photo: "https://cdn.acwing.com/media/article/image/2022/08/09/1_1db2488f17-anonymous.png",
            });
        }

        onMounted(() => {
            timer = setInterval(() => {
                countdown.value--;
                if (countdown.value <= 0) restart();
            }, 1000);
        });

        onUnmounted(() => {
            if (timer) clearInterval(timer);
        });

        return {
            restart,
            resultText,
            winnerText,
            detailText,
            countdown,
        }
    }
}
</script>

<style scoped>
.result-backdrop {
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.32);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 20;
}

.result-board {
    width: min(430px, 88vw);
    padding: 30px;
    border-radius: 8px;
    background: rgba(31, 33, 29, 0.96);
    color: #f7f0df;
    text-align: center;
    border: 1px solid rgba(244, 201, 93, 0.34);
    box-shadow: 0 22px 60px rgba(0, 0, 0, 0.36);
}

.result-kicker {
    color: #f4c95d;
    font-size: 16px;
    font-weight: 800;
    margin-bottom: 8px;
}

.result-board-text {
    font-size: 48px;
    font-weight: 800;
    line-height: 1.1;
}

.result-detail {
    color: #d8ccb1;
    font-size: 14px;
    margin-top: 12px;
    min-height: 22px;
}

.result-countdown {
    color: #aeb7b4;
    font-size: 13px;
    margin-top: 10px;
}

.result-board-btn {
    padding-top: 22px;
}
</style>
