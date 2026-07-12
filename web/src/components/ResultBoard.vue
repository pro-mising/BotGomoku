<template>
    <div class="result-backdrop">
        <div class="result-board">
            <div :class="['result-piece', winnerClass]"></div>
            <div class="result-kicker">{{ winnerText }}</div>
            <div class="result-board-text">{{ resultText }}</div>
            <div class="result-detail">{{ detailText }}</div>
            <div class="result-divider"></div>
            <div class="result-countdown">{{ countdown }} 秒后返回匹配首页</div>
            <div class="result-board-btn">
                <button @click="restart" type="button" class="result-return-btn">
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

        const winnerClass = computed(() => {
            if (winner.value === "A") return "black-piece";
            if (winner.value === "B") return "white-piece";
            return "draw-piece";
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
            winnerClass,
        }
    }
}
</script>

<style scoped>
.result-backdrop {
    position: fixed;
    inset: 0;
    background: rgba(31, 23, 18, 0.36);
    backdrop-filter: blur(2px);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 20;
    padding: 20px;
}

.result-board {
    width: min(440px, 90vw);
    padding: 30px 32px 28px;
    border-radius: 14px;
    background:
        linear-gradient(180deg, rgba(255, 253, 249, 0.98), rgba(250, 244, 235, 0.98));
    color: #2e2925;
    text-align: center;
    border: 1px solid rgba(255, 255, 255, 0.62);
    box-shadow:
        0 24px 70px rgba(38, 25, 18, 0.26),
        0 2px 10px rgba(38, 25, 18, 0.10);
}

.result-piece {
    width: 56px;
    height: 56px;
    margin: 0 auto 14px;
    border-radius: 50%;
    box-shadow:
        0 10px 18px rgba(52, 39, 27, 0.18),
        inset -3px -5px 8px rgba(0, 0, 0, 0.12);
}

.black-piece {
    background: radial-gradient(circle at 35% 28%, #777 0%, #242424 42%, #050505 78%);
}

.white-piece {
    background: radial-gradient(circle at 35% 28%, #ffffff 0%, #f4f1ea 58%, #c9c1b4 100%);
    border: 1px solid rgba(90, 80, 69, 0.16);
}

.draw-piece {
    background:
        linear-gradient(90deg, #1c1c1c 0 50%, #f5f1e9 50% 100%);
    border: 1px solid rgba(90, 80, 69, 0.16);
}

.result-kicker {
    color: #bf781d;
    font-size: 16px;
    font-weight: 700;
    margin-bottom: 6px;
}

.result-board-text {
    color: #27231f;
    font-size: 44px;
    font-weight: 900;
    line-height: 1.1;
}

.result-detail {
    color: #766d64;
    font-size: 14px;
    margin-top: 12px;
    min-height: 22px;
    line-height: 1.7;
}

.result-divider {
    width: 100%;
    height: 1px;
    margin: 18px 0 12px;
    background: rgba(105, 82, 56, 0.13);
}

.result-countdown {
    color: #8a8076;
    font-size: 13px;
    margin-top: 10px;
}

.result-board-btn {
    padding-top: 18px;
}

.result-return-btn {
    min-width: 132px;
    min-height: 40px;
    padding: 0 22px;
    border: 0;
    border-radius: 999px;
    background: #35312c;
    color: #f2c77d;
    font-size: 15px;
    font-weight: 800;
    box-shadow: 0 7px 18px rgba(54, 41, 27, 0.18);
}

.result-return-btn:hover {
    background: #2d2925;
}
</style>
