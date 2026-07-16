<template>
    <div
        :class="[
            'player-card',
            color === 'black' ? 'player-card-black' : 'player-card-white',
            { 'player-card-active': active }
        ]"
    >
        <div class="player-piece-row">
            <span class="piece-preview"></span>
            <span>{{ sideText }} · {{ orderText }}</span>
        </div>

        <img class="player-avatar" :src="player.photo" alt="玩家头像">

        <div class="player-name">{{ player.username }}</div>

        <div class="player-online">
            <span class="online-dot"></span>
            在线
        </div>

        <div class="player-turn-badge">
            {{ turnText }}
        </div>
    </div>
</template>

<script>
export default {
    props: {
        player: {
            type: Object,
            required: true,
        },
        color: {
            type: String,
            required: true,
        },
        active: {
            type: Boolean,
            default: false,
        },
        turnText: {
            type: String,
            required: true,
        },
    },
    computed: {
        sideText() {
            return this.color === "black" ? "黑子" : "白子";
        },
        orderText() {
            return this.color === "black" ? "先手" : "后手";
        },
    },
}
</script>

<style scoped>
.player-card {
    position: relative;
    width: 100%;
    min-height: 348px;
    padding: 30px 22px 28px;
    border-radius: var(--game-radius-medium);
    background: var(--game-card-bg);
    border: 1px solid var(--game-border);
    box-shadow:
        0 10px 24px rgba(52, 39, 27, 0.10),
        0 2px 5px rgba(52, 39, 27, 0.06);
    box-sizing: border-box;
    text-align: center;
    transition:
        transform 180ms ease,
        border-color 180ms ease,
        box-shadow 180ms ease;
}

.player-card-active {
    border-color: var(--game-accent);
    animation: activePlayerPulse 2.4s ease-in-out infinite;
}

.player-piece-row {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
    color: #51483f;
    font-size: 15px;
    font-weight: 700;
}

.piece-preview {
    display: inline-block;
    width: 23px;
    height: 23px;
    border-radius: 50%;
}

.player-card-black .piece-preview {
    background:
        radial-gradient(circle at 35% 28%, #5b5b5b 0%, #1c1c1c 40%, #050505 75%);
    box-shadow:
        inset -2px -3px 4px rgba(0, 0, 0, 0.45),
        0 2px 4px rgba(0, 0, 0, 0.25);
}

.player-card-white .piece-preview {
    background:
        radial-gradient(circle at 35% 28%, #ffffff 0%, #f0eee9 52%, #c9c6bf 100%);
    border: 1px solid rgba(90, 80, 69, 0.16);
    box-shadow:
        inset -1px -2px 3px rgba(100, 95, 88, 0.14),
        0 2px 4px rgba(0, 0, 0, 0.12);
}

.player-avatar {
    display: block;
    width: 96px;
    height: 96px;
    min-width: 96px;
    min-height: 96px;
    aspect-ratio: 1 / 1;
    margin: 30px auto 16px;
    border-radius: 50%;
    object-fit: cover;
    border: 5px solid #f4eee4;
    box-shadow: 0 6px 16px rgba(52, 39, 27, 0.14);
}

.player-card-active .player-avatar {
    border-color: #f2d6a4;
}

.player-name {
    max-width: 100%;
    color: var(--game-text-primary);
    font-size: 21px;
    line-height: 1.3;
    font-weight: 700;
    overflow-wrap: anywhere;
}

.player-online {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 7px;
    margin-top: 12px;
    color: #70675f;
    font-size: 13px;
}

.online-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: var(--game-online);
}

.player-turn-badge {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-height: 32px;
    margin-top: 26px;
    padding: 0 16px;
    border-radius: 999px;
    background: #f2eee9;
    color: #898078;
    font-size: 13px;
    font-weight: 700;
}

.player-card-active .player-turn-badge {
    background: var(--game-accent-soft);
    color: var(--game-accent-dark);
}

@keyframes activePlayerPulse {
    0%,
    100% {
        box-shadow:
            0 12px 28px rgba(193, 126, 31, 0.13),
            0 0 0 1px rgba(230, 163, 59, 0.16);
    }

    50% {
        box-shadow:
            0 14px 32px rgba(193, 126, 31, 0.20),
            0 0 0 3px rgba(230, 163, 59, 0.08);
    }
}

@media (prefers-reduced-motion: reduce) {
    .player-card-active {
        animation: none;
    }
}

@media (max-width: 960px) {
    .player-card {
        min-height: auto;
        padding: 22px;
    }

    .player-avatar {
        width: 72px;
        height: 72px;
        min-width: 72px;
        min-height: 72px;
        margin-top: 16px;
    }
}
</style>
