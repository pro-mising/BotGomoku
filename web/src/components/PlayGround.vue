<template>
    <div class="playground">
        <section class="game-panel">
            <header class="match-header">
                <div class="match-title-group">
                    <div class="match-eyebrow">REALTIME MATCH</div>
                    <div class="match-title-row">
                        <h1>五子棋对战</h1>
                        <div class="room-number">房间号：{{ roomId }}</div>
                    </div>
                </div>

                <div class="turn-status">
                    <span class="turn-status-dot"></span>
                    <span>{{ currentTurnText }}</span>
                </div>
            </header>

            <div class="game-layout">
                <PlayerCard
                    class="player-column"
                    color="black"
                    :player="blackPlayer"
                    :active="currentPlayerIsBlack"
                    :turn-text="blackTurnText"
                />

                <div class="board-column">
                    <div class="board-shell">
                        <GameMap />
                    </div>
                </div>

                <PlayerCard
                    class="player-column"
                    color="white"
                    :player="whitePlayer"
                    :active="!currentPlayerIsBlack"
                    :turn-text="whiteTurnText"
                />
            </div>

            <div class="game-rules">
                <div class="rules-heading">
                    <strong>规则说明</strong>
                    <span>五子连珠即可获胜</span>
                </div>

                <div class="rule-item">
                    <span class="rule-demo horizontal-demo">
                        <i></i><i></i><i></i><i></i><i></i>
                    </span>
                    <span>横向五连</span>
                </div>

                <div class="rule-item">
                    <span class="rule-demo vertical-demo">
                        <i></i><i></i><i></i><i></i><i></i>
                    </span>
                    <span>纵向五连</span>
                </div>

                <div class="rule-item">
                    <span class="rule-demo diagonal-demo">
                        <i></i><i></i><i></i><i></i><i></i>
                    </span>
                    <span>斜向五连</span>
                </div>

                <div class="rule-item rule-win">
                    <span class="win-mark">胜</span>
                    <span>任意方向五连即胜</span>
                </div>
            </div>
        </section>
    </div>
</template>

<script>
import GameMap from './GameMap.vue';
import PlayerCard from './PlayerCard.vue';
import { computed } from 'vue';
import { useStore } from 'vuex';

export default {
    components: {
        GameMap,
        PlayerCard,
    },
    setup() {
        const store = useStore();

        const me = computed(() => ({
            id: Number(store.state.user.id),
            username: store.state.user.username || "我",
            photo: store.state.user.photo,
        }));

        const opponent = computed(() => ({
            id: Number(store.state.pk.a_id) === Number(store.state.user.id)
                ? Number(store.state.pk.b_id)
                : Number(store.state.pk.a_id),
            username: store.state.pk.opponent_username || "对手",
            photo: store.state.pk.opponent_photo,
        }));

        const blackPlayer = computed(() => {
            if (Number(store.state.pk.a_id) === Number(store.state.user.id)) return me.value;
            return opponent.value;
        });

        const whitePlayer = computed(() => {
            if (Number(store.state.pk.b_id) === Number(store.state.user.id)) return me.value;
            return opponent.value;
        });

        const roomId = computed(() => {
            const blackId = Number(store.state.pk.a_id) || 0;
            const whiteId = Number(store.state.pk.b_id) || 0;
            return `${blackId}-${whiteId}`;
        });

        const currentPlayerIsBlack = computed(() => Number(store.state.pk.current_player) === Number(store.state.pk.a_id));
        const isMyTurn = computed(() => Number(store.state.pk.current_player) === Number(store.state.user.id));
        const gameEnded = computed(() => store.state.pk.loser !== "none" || store.state.pk.winner !== "none");
        const isRecord = computed(() => store.state.record.is_record);

        const currentTurnText = computed(() => {
            if (isRecord.value) return "对局回放";
            if (gameEnded.value) return "对局结束";
            if (isMyTurn.value) return "轮到你落子";
            return currentPlayerIsBlack.value ? "黑方回合" : "白方回合";
        });

        const blackTurnText = computed(() => {
            if (gameEnded.value) return "对局结束";
            if (currentPlayerIsBlack.value) return isMyTurn.value ? "你的回合" : "落子中";
            return "等待中";
        });

        const whiteTurnText = computed(() => {
            if (gameEnded.value) return "对局结束";
            if (!currentPlayerIsBlack.value) return isMyTurn.value ? "你的回合" : "落子中";
            return "等待中";
        });

        return {
            blackPlayer,
            whitePlayer,
            roomId,
            currentPlayerIsBlack,
            currentTurnText,
            blackTurnText,
            whiteTurnText,
        }
    }
}
</script>

<style scoped>
.playground {
    --game-panel-bg: rgba(250, 248, 243, 0.96);
    --game-card-bg: rgba(255, 253, 249, 0.94);
    --game-text-primary: #2e2925;
    --game-text-secondary: #766d64;
    --game-border: rgba(91, 71, 51, 0.12);
    --game-accent: #e4a13b;
    --game-accent-dark: #bf781d;
    --game-accent-soft: #fff1d8;
    --game-online: #35b879;
    --game-radius-small: 8px;
    --game-radius-medium: 14px;
    --game-shadow: 0 14px 32px rgba(52, 36, 22, 0.12);

    width: 100%;
    min-height: 78vh;
    margin: 0 auto;
    padding: 24px 0;
    display: flex;
    align-items: center;
    justify-content: center;
    box-sizing: border-box;
}

.game-panel {
    width: min(1320px, calc(100vw - 80px));
    margin: 0 auto;
    padding: 34px 36px 26px;
    border-radius: 10px;
    background: var(--game-panel-bg);
    border: 1px solid rgba(255, 255, 255, 0.55);
    box-shadow:
        0 18px 50px rgba(38, 25, 18, 0.18),
        0 2px 8px rgba(38, 25, 18, 0.08);
    box-sizing: border-box;
}

.match-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 24px;
    min-height: 72px;
    margin-bottom: 18px;
}

.match-eyebrow {
    margin-bottom: 2px;
    color: #c8872c;
    font-size: 13px;
    font-weight: 700;
    letter-spacing: 0.04em;
}

.match-title-row {
    display: flex;
    align-items: flex-end;
    gap: 18px;
    flex-wrap: wrap;
}

.match-title-row h1 {
    margin: 0;
    color: #27231f;
    font-size: 30px;
    line-height: 1.15;
    font-weight: 700;
}

.room-number {
    padding-bottom: 3px;
    color: #786e64;
    font-size: 14px;
    white-space: nowrap;
}

.turn-status {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    min-height: 40px;
    padding: 0 18px;
    border-radius: 999px;
    background: #35312c;
    color: #f2c77d;
    font-size: 15px;
    font-weight: 700;
    box-shadow: 0 5px 14px rgba(54, 41, 27, 0.14);
    white-space: nowrap;
}

.turn-status-dot {
    width: 9px;
    height: 9px;
    border-radius: 50%;
    background: #f0c777;
}

.game-layout {
    display: grid;
    grid-template-columns: 240px minmax(500px, 600px) 240px;
    justify-content: center;
    align-items: center;
    column-gap: 54px;
}

.board-column {
    display: flex;
    flex-direction: column;
    align-items: center;
    min-width: 0;
}

.board-shell {
    width: min(600px, 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 7px;
    border-radius: var(--game-radius-small);
    background: rgba(255, 255, 255, 0.76);
    border: 1px solid rgba(122, 91, 52, 0.12);
    box-shadow:
        0 14px 26px rgba(74, 50, 28, 0.12),
        0 2px 5px rgba(74, 50, 28, 0.07);
    box-sizing: border-box;
}

.game-rules {
    display: flex;
    align-items: stretch;
    justify-content: center;
    width: min(860px, 88%);
    min-height: 64px;
    margin: 22px auto 0;
    border-radius: var(--game-radius-medium);
    background: rgba(255, 252, 246, 0.86);
    border: 1px solid rgba(105, 82, 56, 0.12);
    box-shadow: 0 6px 18px rgba(58, 42, 25, 0.07);
    overflow: hidden;
}

.rules-heading,
.rule-item {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
    padding: 12px 22px;
}

.rules-heading {
    flex-direction: column;
    align-items: flex-start;
    min-width: 170px;
}

.rules-heading strong {
    color: #35302b;
    font-size: 15px;
}

.rules-heading span {
    color: #8a8076;
    font-size: 12px;
}

.rule-item {
    flex: 1;
    color: #62594f;
    font-size: 13px;
    border-left: 1px solid rgba(105, 82, 56, 0.10);
    white-space: nowrap;
}

.rule-demo {
    position: relative;
    display: inline-grid;
    width: 62px;
    height: 28px;
}

.rule-demo i {
    position: absolute;
    width: 7px;
    height: 7px;
    border-radius: 50%;
    background: radial-gradient(circle at 34% 30%, #49413a 0%, #26221d 70%);
    box-shadow: 0 1px 2px rgba(47, 34, 22, 0.24);
}

.horizontal-demo i {
    top: 10px;
}

.horizontal-demo i:nth-child(1) {
    left: 3px;
}

.horizontal-demo i:nth-child(2) {
    left: 15px;
}

.horizontal-demo i:nth-child(3) {
    left: 27px;
}

.horizontal-demo i:nth-child(4) {
    left: 39px;
}

.horizontal-demo i:nth-child(5) {
    left: 51px;
}

.vertical-demo {
    width: 28px;
}

.vertical-demo i {
    left: 10px;
}

.vertical-demo i:nth-child(1) {
    top: 0;
}

.vertical-demo i:nth-child(2) {
    top: 5px;
}

.vertical-demo i:nth-child(3) {
    top: 10px;
}

.vertical-demo i:nth-child(4) {
    top: 15px;
}

.vertical-demo i:nth-child(5) {
    top: 20px;
}

.diagonal-demo i:nth-child(1) {
    left: 5px;
    top: 20px;
}

.diagonal-demo i:nth-child(2) {
    left: 17px;
    top: 15px;
}

.diagonal-demo i:nth-child(3) {
    left: 29px;
    top: 10px;
}

.diagonal-demo i:nth-child(4) {
    left: 41px;
    top: 5px;
}

.diagonal-demo i:nth-child(5) {
    left: 53px;
    top: 0;
}

.rule-win {
    color: var(--game-accent-dark);
    font-weight: 700;
}

.win-mark {
    display: inline-grid;
    place-items: center;
    width: 24px;
    height: 24px;
    border-radius: 50%;
    background: var(--game-accent-soft);
    color: var(--game-accent-dark);
    font-size: 12px;
    font-weight: 800;
}

@media (max-width: 1180px) {
    .game-layout {
        grid-template-columns: 205px minmax(460px, 560px) 205px;
        column-gap: 28px;
    }
}

@media (max-width: 960px) {
    .playground {
        padding: 18px 0;
    }

    .game-panel {
        width: calc(100vw - 32px);
        padding: 22px 18px;
    }

    .match-header {
        flex-direction: column;
        min-height: auto;
    }

    .game-layout {
        display: flex;
        flex-wrap: wrap;
        gap: 20px;
    }

    .player-column {
        flex: 1;
        min-width: 220px;
        order: 1;
    }

    .board-column {
        width: 100%;
        order: 2;
    }

    .game-rules {
        flex-wrap: wrap;
        overflow: visible;
    }

    .rules-heading,
    .rule-item {
        flex: 1 1 180px;
        border-left: 0;
        border-top: 1px solid rgba(105, 82, 56, 0.10);
    }

    .rules-heading {
        border-top: 0;
    }
}
</style>
