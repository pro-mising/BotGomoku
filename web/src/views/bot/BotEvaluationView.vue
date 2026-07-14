<template>
    <ContentField>
        <div class="evaluation-page">
            <header class="evaluation-header">
                <div>
                    <h2>Bot测试</h2>
                    <p>选择你的 Bot，系统会与多个内置 Bot 自动对战，并生成多维度测评报告。</p>
                </div>
            </header>

            <section class="control-panel">
                <div class="field-group">
                    <label>选择Bot</label>
                    <select v-model="selected_bot_id" class="form-select" @change="load_latest_report(true)">
                        <option v-if="bots.length === 0" value="">暂无可测试 Bot</option>
                        <option v-for="bot in bots" :key="bot.id" :value="bot.id">{{ bot.title }}</option>
                    </select>
                </div>

                <div class="field-group">
                    <label>测评模式</label>
                    <div class="mode-switch">
                        <button :class="{ active: mode === 'quick' }" @click="mode = 'quick'">快速测评</button>
                        <button :class="{ active: mode === 'standard' }" @click="mode = 'standard'">标准测评</button>
                    </div>
                </div>

                <button class="btn btn-primary run-btn" :disabled="running" @click="run_evaluation">
                    {{ running ? "测评中..." : "开始测评" }}
                </button>
            </section>

            <div class="error-message">{{ error_message }}</div>

            <section class="report" v-if="report">
                <div class="score-card">
                    <div>
                        <div class="score-label">综合评分</div>
                        <div class="score-value">{{ report.metrics.overall_score }}</div>
                    </div>
                    <div class="summary">{{ report.summary }}</div>
                </div>

                <div class="metric-grid">
                    <article class="metric-card">
                        <span>胜率</span>
                        <strong>{{ report.metrics.win_rate }}%</strong>
                    </article>
                    <article class="metric-card">
                        <span>进攻能力</span>
                        <strong>{{ report.metrics.attack_score }}</strong>
                    </article>
                    <article class="metric-card">
                        <span>防守能力</span>
                        <strong>{{ report.metrics.defense_score }}</strong>
                    </article>
                    <article class="metric-card">
                        <span>稳定性</span>
                        <strong>{{ report.metrics.stability_score }}</strong>
                    </article>
                    <article class="metric-card">
                        <span>效率</span>
                        <strong>{{ report.metrics.efficiency_score }}</strong>
                    </article>
                    <article class="metric-card">
                        <span>平均步数</span>
                        <strong>{{ report.metrics.average_steps }}</strong>
                    </article>
                </div>

                <section class="analysis-card">
                    <div class="analysis-head">
                        <h3>赛后分析</h3>
                        <button class="btn btn-outline-primary btn-sm" :disabled="deepseek_running || !report" @click="run_deepseek_analysis">
                            {{ deepseek_running ? "分析中..." : "DeepSeek赛后分析" }}
                        </button>
                    </div>
                    <div class="analysis-grid" v-if="hasDeepSeekAnalysis">
                        <article class="analysis-section">
                            <span>测评发现</span>
                            <p>{{ analysis_report.findings }}</p>
                        </article>
                        <article class="analysis-section">
                            <span>主要弱点</span>
                            <p>{{ analysis_report.weaknesses }}</p>
                        </article>
                        <article class="analysis-section">
                            <span>改进建议</span>
                            <p>{{ analysis_report.suggestions }}</p>
                        </article>
                    </div>
                    <p v-else>{{ report.ai_report }}</p>
                    <div class="formula">综合评分 = 胜率 40% + 防守能力 20% + 进攻能力 20% + 稳定性 10% + 效率 10%</div>
                    <div class="optimized-code" v-if="optimized_code">
                        <h4>DeepSeek 优化后的 Bot 代码</h4>
                        <pre><code v-html="highlightedCode"></code></pre>
                    </div>
                </section>

                <section class="game-table">
                    <h3>对局明细</h3>
                    <table class="table table-hover align-middle">
                        <thead>
                            <tr>
                                <th>对手</th>
                                <th>难度</th>
                                <th>执棋</th>
                                <th>结果</th>
                                <th>步数</th>
                                <th>原因</th>
                                <th>回放</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr v-for="(game, index) in report.games" :key="index">
                                <td>{{ game.opponent_name }}</td>
                                <td>{{ game.opponent_level }}</td>
                                <td>{{ game.user_side }}</td>
                                <td>
                                    <span :class="['result-pill', game.result === '胜利' ? 'win' : (game.result === '平局' ? 'draw' : 'lose')]">
                                        {{ game.result }}
                                    </span>
                                </td>
                                <td>{{ game.steps }}</td>
                                <td>{{ reasonText(game.reason) }}</td>
                                <td>
                                    <button class="btn btn-outline-secondary btn-sm" @click="openReplay(game)">查看</button>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </section>
            </section>

            <div class="replay-backdrop" v-if="replay_game" @click.self="closeReplay">
                <div class="replay-modal">
                    <div class="replay-head">
                        <div>
                            <h3>{{ replay_game.opponent_name }} 对局回放</h3>
                            <p>{{ replay_game.user_side }} · {{ replay_game.result }} · 第 {{ replay_index }} / {{ replay_game.moves.length }} 手</p>
                        </div>
                        <button class="btn-close" type="button" aria-label="关闭" @click="closeReplay"></button>
                    </div>

                    <div class="replay-board">
                        <div
                            :class="['replay-cell', {
                                'first-row': cell.row === 0,
                                'last-row': cell.row === 14,
                                'first-col': cell.col === 0,
                                'last-col': cell.col === 14,
                            }]"
                            v-for="cell in replayCells"
                            :key="cell.index"
                        >
                            <span class="star-point" v-if="cell.star"></span>
                            <span
                                v-if="cell.piece"
                                :class="['replay-piece', cell.piece === 1 ? 'black' : 'white', { last: cell.step === replay_index }]"
                            ></span>
                        </div>
                    </div>

                    <div class="replay-controls">
                        <button class="btn btn-outline-secondary btn-sm" @click="restartReplay">重新播放</button>
                        <div class="replay-progress">
                            <span :style="{ width: replayProgress + '%' }"></span>
                        </div>
                        <button class="btn btn-outline-secondary btn-sm" @click="toggleReplay">
                            {{ replay_playing ? "暂停" : "继续" }}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    </ContentField>
</template>

<script>
import ContentField from "@/components/ContentField.vue";
import { computed, onBeforeUnmount, ref } from "vue";
import { useStore } from "vuex";
import $ from "jquery";

export default {
    components: {
        ContentField,
    },
    setup() {
        const store = useStore();
        const bots = ref([]);
        const selected_bot_id = ref("");
        const mode = ref("quick");
        const report = ref(null);
        const running = ref(false);
        const error_message = ref("");
        const replay_game = ref(null);
        const replay_index = ref(0);
        const deepseek_running = ref(false);
        const analysis_report = ref(null);
        const optimized_code = ref("");
        const replay_playing = ref(false);
        let replay_timer = null;

        const authHeaders = () => ({
            Authorization: "Bearer " + store.state.user.token,
        });

        const refresh_bots = () => {
            $.ajax({
                url: "http://127.0.0.1:3000/user/bot/getlist/",
                type: "GET",
                headers: authHeaders(),
                success(resp) {
                    bots.value = resp;
                    if (!selected_bot_id.value && resp.length > 0) {
                        selected_bot_id.value = resp[0].id;
                    }
                    load_latest_report(false);
                },
                error() {
                    error_message.value = "获取 Bot 列表失败";
                }
            });
        };

        const run_evaluation = () => {
            error_message.value = "";
            report.value = null;
            if (!selected_bot_id.value) {
                error_message.value = "请先选择一个 Bot";
                return;
            }
            running.value = true;
            $.ajax({
                url: "http://127.0.0.1:3000/bot/evaluation/run/",
                type: "POST",
                data: {
                    bot_id: selected_bot_id.value,
                    mode: mode.value,
                },
                headers: authHeaders(),
                success(resp) {
                    if (resp.error_message === "success") {
                        report.value = resp.report;
                        analysis_report.value = null;
                        optimized_code.value = "";
                    } else {
                        error_message.value = resp.error_message;
                    }
                },
                error() {
                    error_message.value = "测评失败，请检查后端服务";
                },
                complete() {
                    running.value = false;
                }
            });
        };

        const run_deepseek_analysis = () => {
            if (!report.value) return;
            error_message.value = "";
            deepseek_running.value = true;
            $.ajax({
                url: "http://127.0.0.1:3000/bot/evaluation/deepseek/",
                type: "POST",
                data: {
                    bot_id: selected_bot_id.value,
                    report_id: report.value.report_id,
                },
                headers: authHeaders(),
                success(resp) {
                    if (resp.error_message === "success") {
                        analysis_report.value = resp.analysis;
                        optimized_code.value = resp.optimized_code;
                    } else {
                        error_message.value = resp.error_message;
                    }
                },
                error() {
                    error_message.value = "DeepSeek 分析请求失败";
                },
                complete() {
                    deepseek_running.value = false;
                }
            });
        };

        const load_latest_report = (onlySelectedBot = false) => {
            const data = {};
            if (onlySelectedBot && selected_bot_id.value) {
                data.bot_id = selected_bot_id.value;
            }
            $.ajax({
                url: "http://127.0.0.1:3000/bot/evaluation/latest/",
                type: "GET",
                data,
                headers: authHeaders(),
                success(resp) {
                    if (resp.error_message !== "success") {
                        if (onlySelectedBot) {
                            report.value = null;
                            analysis_report.value = null;
                            optimized_code.value = "";
                        }
                        return;
                    }
                    report.value = resp.report;
                    selected_bot_id.value = resp.bot_id;
                    analysis_report.value = resp.analysis;
                    optimized_code.value = resp.optimized_code || "";
                }
            });
        };

        const reasonText = reason => {
            const map = {
                "five-in-a-row": "五连获胜",
                "invalid-move": "非法落子",
                "timeout": "超时",
                "draw": "平局",
                "opponent-invalid": "对手非法落子",
            };
            return map[reason] || reason;
        };

        const openReplay = game => {
            replay_game.value = game;
            replay_index.value = 0;
            startReplay();
        };

        const closeReplay = () => {
            stopReplay();
            replay_game.value = null;
            replay_index.value = 0;
        };

        const stopReplay = () => {
            if (replay_timer) {
                clearInterval(replay_timer);
                replay_timer = null;
            }
            replay_playing.value = false;
        };

        const startReplay = () => {
            stopReplay();
            if (!replay_game.value || replay_game.value.moves.length === 0) return;
            replay_playing.value = true;
            replay_timer = setInterval(() => {
                if (!replay_game.value || replay_index.value >= replay_game.value.moves.length) {
                    stopReplay();
                    return;
                }
                replay_index.value++;
            }, 420);
        };

        const toggleReplay = () => {
            if (replay_playing.value) {
                stopReplay();
            } else {
                if (replay_game.value && replay_index.value >= replay_game.value.moves.length) {
                    replay_index.value = 0;
                }
                startReplay();
            }
        };

        const restartReplay = () => {
            replay_index.value = 0;
            startReplay();
        };

        const replayProgress = computed(() => {
            if (!replay_game.value || replay_game.value.moves.length === 0) return 0;
            return Math.round(replay_index.value * 100 / replay_game.value.moves.length);
        });

        const replayCells = computed(() => {
            const cells = Array.from({ length: 225 }, (_, index) => ({
                index,
                row: Math.floor(index / 15),
                col: index % 15,
                piece: 0,
                step: 0,
                star: [48, 56, 112, 168, 176].includes(index),
            }));
            if (!replay_game.value) return cells;
            replay_game.value.moves.slice(0, replay_index.value).forEach(move => {
                cells[move.action] = {
                    ...cells[move.action],
                    index: move.action,
                    piece: move.piece,
                    step: move.step,
                };
            });
            return cells;
        });

        const hasDeepSeekAnalysis = computed(() => {
            return analysis_report.value
                && analysis_report.value.status === "success"
                && (analysis_report.value.findings || analysis_report.value.weaknesses || analysis_report.value.suggestions);
        });

        const escapeHtml = text => (text || "")
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;");

        const highlightedCode = computed(() => {
            const escaped = escapeHtml(optimized_code.value);
            return escaped
                .replace(/("(?:\\.|[^"\\])*")/g, '<span class="code-string">$1</span>')
                .replace(/\b(package|import|public|private|protected|class|interface|implements|extends|static|final|return|if|else|for|while|switch|case|break|continue|new|try|catch|int|long|double|boolean|String|void|null|true|false)\b/g, '<span class="code-keyword">$1</span>')
                .replace(/\b([A-Z][A-Za-z0-9_]*)\b/g, '<span class="code-type">$1</span>');
        });

        onBeforeUnmount(stopReplay);

        refresh_bots();

        return {
            bots,
            selected_bot_id,
            mode,
            report,
            running,
            error_message,
            replay_game,
            replay_index,
            replayCells,
            replay_playing,
            replayProgress,
            deepseek_running,
            analysis_report,
            hasDeepSeekAnalysis,
            optimized_code,
            highlightedCode,
            run_evaluation,
            run_deepseek_analysis,
            load_latest_report,
            reasonText,
            openReplay,
            closeReplay,
            toggleReplay,
            restartReplay,
        };
    }
}
</script>

<style scoped>
.evaluation-page {
    display: grid;
    gap: 18px;
}

.evaluation-header {
    padding-bottom: 12px;
    border-bottom: 1px solid #e5e7eb;
}

.eyebrow {
    color: #c8872c;
    font-size: 13px;
    font-weight: 800;
}

h2,
h3,
p {
    margin: 0;
}

h2 {
    color: #1f2937;
    font-size: 28px;
    font-weight: 900;
}

.evaluation-header p {
    margin-top: 6px;
    color: #64748b;
}

.control-panel {
    display: grid;
    grid-template-columns: minmax(220px, 1fr) minmax(260px, 1fr) auto;
    gap: 14px;
    align-items: end;
    padding: 16px;
    border-radius: 8px;
    background: #f8fafc;
    border: 1px solid #e5e7eb;
}

.field-group {
    display: grid;
    gap: 7px;
}

.field-group label {
    color: #475569;
    font-weight: 800;
    font-size: 13px;
}

.mode-switch {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 8px;
}

.mode-switch button {
    min-height: 38px;
    border-radius: 8px;
    border: 1px solid #cbd5e1;
    background: white;
    color: #475569;
    font-weight: 800;
}

.mode-switch button.active {
    border-color: #bf781d;
    background: #fff1d8;
    color: #bf781d;
}

.run-btn {
    min-height: 38px;
    min-width: 120px;
}

.error-message {
    min-height: 22px;
    color: #dc3545;
    font-weight: 700;
}

.report {
    display: grid;
    gap: 16px;
}

.score-card {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 18px;
    padding: 20px;
    border-radius: 8px;
    background: linear-gradient(135deg, #fff7e8, #ffffff);
    border: 1px solid rgba(191, 120, 29, 0.18);
}

.score-label {
    color: #92400e;
    font-weight: 800;
}

.score-value {
    color: #1f2937;
    font-size: 44px;
    line-height: 1;
    font-weight: 900;
}

.summary {
    color: #475569;
    line-height: 1.7;
}

.metric-grid {
    display: grid;
    grid-template-columns: repeat(6, minmax(0, 1fr));
    gap: 12px;
}

.metric-card {
    padding: 14px;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    background: white;
}

.metric-card span {
    display: block;
    color: #64748b;
    font-size: 13px;
    font-weight: 800;
}

.metric-card strong {
    display: block;
    margin-top: 8px;
    color: #0f172a;
    font-size: 24px;
}

.analysis-card,
.game-table {
    padding: 16px;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    background: white;
}

.analysis-card h3,
.game-table h3 {
    margin-bottom: 10px;
    font-size: 18px;
    font-weight: 900;
}

.analysis-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 10px;
}

.analysis-head h3 {
    margin: 0;
}

.analysis-card p {
    color: #475569;
    line-height: 1.8;
}

.analysis-grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 12px;
}

.analysis-section {
    min-height: 150px;
    padding: 14px;
    border: 1px solid rgba(191, 120, 29, 0.18);
    border-radius: 8px;
    background: linear-gradient(180deg, #fffaf0, #ffffff);
}

.analysis-section span {
    display: inline-flex;
    margin-bottom: 8px;
    color: #92400e;
    font-size: 13px;
    font-weight: 900;
}

.analysis-section p {
    white-space: pre-line;
    font-size: 14px;
}

.formula {
    margin-top: 10px;
    color: #92400e;
    font-weight: 800;
}

.optimized-code {
    margin-top: 14px;
}

.optimized-code h4 {
    margin: 0 0 8px;
    color: #0f172a;
    font-size: 15px;
    font-weight: 900;
}

.optimized-code pre {
    max-height: 420px;
    overflow: auto;
    padding: 14px;
    border-radius: 8px;
    background: #111827;
    color: #e5e7eb;
    font-size: 13px;
    line-height: 1.7;
}

.optimized-code :deep(.code-keyword) {
    color: #f59e0b;
    font-weight: 800;
}

.optimized-code :deep(.code-type) {
    color: #93c5fd;
}

.optimized-code :deep(.code-string) {
    color: #86efac;
}

.result-pill {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 48px;
    padding: 4px 9px;
    border-radius: 999px;
    font-size: 12px;
    font-weight: 900;
}

.result-pill.win {
    background: #dcfce7;
    color: #166534;
}

.result-pill.lose {
    background: #fee2e2;
    color: #991b1b;
}

.result-pill.draw {
    background: #e5e7eb;
    color: #374151;
}

.replay-backdrop {
    position: fixed;
    inset: 0;
    z-index: 40;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 24px;
    background: rgba(31, 23, 18, 0.42);
}

.replay-modal {
    width: min(620px, 94vw);
    padding: 18px;
    border-radius: 8px;
    background: #fffdf9;
    box-shadow: 0 24px 70px rgba(38, 25, 18, 0.26);
}

.replay-head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 14px;
}

.replay-head p {
    margin-top: 4px;
    color: #64748b;
}

.replay-board {
    display: grid;
    grid-template-columns: repeat(15, 1fr);
    width: min(540px, 84vw);
    aspect-ratio: 1 / 1;
    margin: 0 auto;
    padding: 26px;
    border: 1px solid rgba(110, 72, 27, 0.55);
    border-radius: 8px;
    box-shadow:
        inset 0 0 0 6px rgba(255, 248, 232, 0.42),
        inset 0 0 0 10px rgba(137, 92, 34, 0.18),
        0 18px 38px rgba(64, 40, 18, 0.18);
    background:
        linear-gradient(90deg, rgba(119, 70, 22, 0.08), rgba(255, 242, 195, 0.2), rgba(119, 70, 22, 0.08)),
        repeating-linear-gradient(90deg, rgba(100, 62, 22, 0.06) 0 1px, transparent 1px 18px),
        #e8be73;
}

.replay-cell {
    position: relative;
    display: grid;
    place-items: center;
    min-width: 0;
    min-height: 0;
}

.replay-cell::before,
.replay-cell::after {
    content: "";
    position: absolute;
    z-index: 0;
    background: rgba(62, 43, 22, 0.52);
}

.replay-cell::before {
    left: 0;
    top: 50%;
    width: 100%;
    height: 1px;
    transform: translateY(-50%);
}

.replay-cell::after {
    left: 50%;
    top: 0;
    width: 1px;
    height: 100%;
    transform: translateX(-50%);
}

.replay-cell.first-col::before {
    left: 50%;
    width: 50%;
}

.replay-cell.last-col::before {
    width: 50%;
}

.replay-cell.first-row::after {
    top: 50%;
    height: 50%;
}

.replay-cell.last-row::after {
    height: 50%;
}

.star-point {
    position: relative;
    z-index: 1;
    width: 7px;
    height: 7px;
    border-radius: 50%;
    background: rgba(44, 31, 17, 0.75);
    box-shadow: 0 1px 2px rgba(255, 232, 177, 0.32);
}

.replay-piece {
    position: relative;
    z-index: 2;
    width: 72%;
    height: 72%;
    border-radius: 50%;
    box-shadow: 0 2px 5px rgba(0, 0, 0, 0.22);
}

.replay-piece.black {
    background: radial-gradient(circle at 35% 30%, #777, #050505 72%);
}

.replay-piece.white {
    background: radial-gradient(circle at 35% 30%, #fff, #c9c1b4 100%);
    border: 1px solid rgba(90, 80, 69, 0.22);
}

.replay-piece.last {
    outline: 3px solid #e45b3f;
    outline-offset: 2px;
}

.replay-controls {
    display: grid;
    grid-template-columns: auto 1fr auto;
    align-items: center;
    gap: 12px;
    margin-top: 16px;
}

.replay-progress {
    height: 8px;
    overflow: hidden;
    border-radius: 999px;
    background: #efe4d3;
}

.replay-progress span {
    display: block;
    height: 100%;
    border-radius: inherit;
    background: linear-gradient(90deg, #d18a2d, #f0b85a);
    transition: width 0.18s ease;
}

@media (max-width: 1000px) {
    .control-panel,
    .metric-grid,
    .analysis-grid {
        grid-template-columns: 1fr 1fr;
    }

    .run-btn {
        grid-column: 1 / -1;
    }
}

@media (max-width: 640px) {
    .control-panel,
    .metric-grid,
    .analysis-grid,
    .score-card {
        grid-template-columns: 1fr;
        display: grid;
    }
}
</style>
