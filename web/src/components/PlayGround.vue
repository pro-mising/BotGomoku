<template>
    <div class="playground">
        <div class="arena">
            <div class="arena-topbar">
                <div>
                    <div class="eyebrow">实时对战</div>
                    <div class="game-title">五子棋对战</div>
                </div>
                <div :class="['status-pill', { ready: isMyTurn }]">
                    {{ statusText }}
                </div>
            </div>

            <div class="match-panel">
                <div :class="['player-card', 'black-card', { active: currentPlayerIsBlack }]">
                    <div class="piece-orbit">
                        <span class="piece-dot black"></span>
                    </div>
                    <img :src="blackPlayer.photo" alt="" class="player-avatar">
                    <div class="player-meta">
                        <div class="piece-name">黑子 · 先手</div>
                        <div class="player-name">{{ blackPlayer.username }}</div>
                    </div>
                    <div class="turn-chip" v-if="currentPlayerIsBlack">落子中</div>
                </div>

                <div class="board-shell">
                    <GameMap />
                </div>

                <div :class="['player-card', 'white-card', { active: !currentPlayerIsBlack }]">
                    <div class="piece-orbit">
                        <span class="piece-dot white"></span>
                    </div>
                    <img :src="whitePlayer.photo" alt="" class="player-avatar">
                    <div class="player-meta">
                        <div class="piece-name">白子 · 后手</div>
                        <div class="player-name">{{ whitePlayer.username }}</div>
                    </div>
                    <div class="turn-chip" v-if="!currentPlayerIsBlack">落子中</div>
                </div>
            </div>

            <div class="rule-strip">
                <span>横向五连</span>
                <span>纵向五连</span>
                <span>斜向五连</span>
                <span>任意一种即胜</span>
            </div>
        </div>
    </div>
</template>

<script>
import GameMap from './GameMap.vue';
import { computed } from 'vue';
import { useStore } from 'vuex';

export default {
    components: {
        GameMap,
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

        const currentPlayerIsBlack = computed(() => Number(store.state.pk.current_player) === Number(store.state.pk.a_id));
        const isMyTurn = computed(() => Number(store.state.pk.current_player) === Number(store.state.user.id));

        const statusText = computed(() => isMyTurn.value ? "轮到你落子" : "等待对手");

        return {
            blackPlayer,
            whitePlayer,
            currentPlayerIsBlack,
            isMyTurn,
            statusText,
        }
    }
}
</script>

<style scoped>
.playground {
    width: min(1220px, 96vw);
    min-height: 78vh;
    margin: 22px auto;
    display: flex;
    align-items: center;
    justify-content: center;
}

.arena {
    width: 100%;
    padding: 22px;
    border-radius: 8px;
    background: linear-gradient(135deg, #eaf2ee 0%, #f7f5ef 52%, #e8eef3 100%);
    border: 1px solid rgba(31, 73, 66, 0.12);
    box-shadow: 0 22px 70px rgba(27, 47, 48, 0.16);
}

.arena-topbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 18px;
}

.eyebrow {
    color: #48756d;
    font-size: 12px;
    font-weight: 800;
    text-transform: uppercase;
}

.game-title {
    color: #182522;
    font-size: 28px;
    line-height: 1.1;
    font-weight: 900;
}

.status-pill {
    min-width: 112px;
    padding: 9px 14px;
    border-radius: 999px;
    background: #dbe4e2;
    color: #34514b;
    font-weight: 900;
    text-align: center;
    border: 1px solid rgba(31, 73, 66, 0.12);
}

.status-pill.ready {
    background: #123c36;
    color: #fff7df;
}

.match-panel {
    display: grid;
    grid-template-columns: minmax(150px, 200px) minmax(430px, 660px) minmax(150px, 200px);
    gap: 20px;
    align-items: center;
}

.board-shell {
    padding: 14px;
    border-radius: 8px;
    background: #ffffff;
    border: 1px solid rgba(31, 73, 66, 0.14);
    box-shadow: 0 18px 48px rgba(44, 67, 63, 0.18);
}

.player-card {
    position: relative;
    min-height: 224px;
    padding: 18px;
    border-radius: 8px;
    background: rgba(255, 255, 255, 0.88);
    color: #1e2725;
    border: 1px solid rgba(31, 73, 66, 0.12);
    box-shadow: 0 12px 34px rgba(44, 67, 63, 0.12);
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 12px;
}

.player-card.active {
    border-color: #d08f2d;
    box-shadow: 0 0 0 2px rgba(208, 143, 45, 0.2), 0 18px 42px rgba(44, 67, 63, 0.18);
}

.piece-orbit {
    width: 46px;
    height: 46px;
    display: grid;
    place-items: center;
    border-radius: 50%;
    background: #edf3f1;
    border: 1px solid rgba(31, 73, 66, 0.1);
}

.player-avatar {
    width: 78px;
    height: 78px;
    border-radius: 50%;
    object-fit: cover;
    border: 3px solid #ffffff;
    box-shadow: 0 8px 18px rgba(31, 73, 66, 0.16);
}

.player-meta {
    text-align: center;
}

.piece-dot {
    width: 24px;
    height: 24px;
    border-radius: 50%;
    display: inline-block;
}

.piece-dot.black {
    background: radial-gradient(circle at 35% 35%, #8c8c8c, #101010 70%);
}

.piece-dot.white {
    background: radial-gradient(circle at 35% 35%, #ffffff, #d6d0c4 78%);
    border: 1px solid #aaa396;
}

.piece-name {
    color: #5a706b;
    font-size: 13px;
    font-weight: 800;
    margin-bottom: 5px;
}

.player-name {
    max-width: 160px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: 18px;
    font-weight: 900;
}

.turn-chip {
    padding: 5px 12px;
    border-radius: 999px;
    background: #d08f2d;
    color: #fffaf0;
    font-size: 13px;
    font-weight: 900;
}

.rule-strip {
    margin-top: 16px;
    display: flex;
    justify-content: center;
    gap: 10px;
    flex-wrap: wrap;
}

.rule-strip span {
    padding: 6px 10px;
    border-radius: 999px;
    background: rgba(255, 255, 255, 0.72);
    color: #4a615c;
    font-size: 13px;
    font-weight: 800;
    border: 1px solid rgba(31, 73, 66, 0.1);
}

@media (max-width: 920px) {
    .match-panel {
        grid-template-columns: 1fr;
    }

    .player-card {
        min-height: 116px;
        flex-direction: row;
    }
}
</style>
