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
                    <div class="analysis-pending" v-if="analysis_report && analysis_report.status === 'analyzing'">
                        DeepSeek 正在分析测评数据，刷新或切换页面不会中断，请稍等。
                    </div>
                    <div class="analysis-grid" v-else-if="hasDeepSeekAnalysis">
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
                    <div class="analysis-pending failed" v-else-if="analysis_report && analysis_report.status === 'failed'">
                        {{ analysis_report.findings || "DeepSeek 分析失败，请稍后重试。" }}
                    </div>
                    <p v-else>{{ report.ai_report }}</p>
                    <div class="formula">综合评分 = 胜率 40% + 防守能力 20% + 进攻能力 20% + 稳定性 10% + 效率 10%</div>
                    <div class="optimized-code" v-if="optimized_code">
                        <div class="optimized-code-head">
                            <h4>DeepSeek 优化后的 Bot 代码</h4>
                            <button class="btn btn-outline-secondary btn-sm" @click="copyOptimizedCode">
                                {{ copy_message || "复制纯代码" }}
                            </button>
                        </div>
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

        </div>
    </ContentField>
</template>

<script>
import ContentField from "@/components/ContentField.vue";
import { computed, onBeforeUnmount, ref } from "vue";
import { useStore } from "vuex";
import $ from "jquery";
import router from "@/router";

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
        const deepseek_running = ref(false);
        const analysis_report = ref(null);
        const optimized_code = ref("");
        const copy_message = ref("");
        let analysis_timer = null;

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
                        stopAnalysisPolling();
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
                        if (resp.analysis && resp.analysis.status === "analyzing") {
                            startAnalysisPolling();
                        } else {
                            deepseek_running.value = false;
                            stopAnalysisPolling();
                        }
                    } else {
                        error_message.value = resp.error_message;
                        deepseek_running.value = false;
                    }
                },
                error() {
                    error_message.value = "DeepSeek 分析请求失败";
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
                    if (resp.analysis && resp.analysis.status === "analyzing") {
                        deepseek_running.value = true;
                        startAnalysisPolling();
                    } else {
                        deepseek_running.value = false;
                        stopAnalysisPolling();
                    }
                }
            });
        };

        const startAnalysisPolling = () => {
            if (analysis_timer) return;
            analysis_timer = setInterval(() => load_latest_report(true), 2000);
        };

        const stopAnalysisPolling = () => {
            if (!analysis_timer) return;
            clearInterval(analysis_timer);
            analysis_timer = null;
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
            const blackMoves = [];
            const whiteMoves = [];
            game.moves.forEach(move => {
                if (move.piece === 1) blackMoves.push(move.action);
                else whiteMoves.push(move.action);
            });

            const userIsBlack = game.user_side === "黑方";
            const userId = Number(store.state.user.id);
            const systemId = 0;

            store.commit("updateIsRecord", true);
            store.commit("updateOpponent", {
                username: game.opponent_name || "系统Bot",
                photo: store.state.user.photo,
            });
            store.commit("updateGame", {
                a_id: userIsBlack ? userId : systemId,
                a_sx: 0,
                a_sy: 0,
                b_id: userIsBlack ? systemId : userId,
                b_sx: 0,
                b_sy: 0,
                current_player: userIsBlack ? userId : systemId,
            });
            store.commit("updateSteps", {
                a_steps: blackMoves.join(","),
                b_steps: whiteMoves.join(","),
            });
            store.commit("updateRecordLoser", game.result === "平局" ? "all" : "");
            router.push({
                name: "record_content",
                params: {
                    recordId: `bot-${report.value.report_id}-${game.opponent_name}`,
                }
            });
        };

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

        const copyOptimizedCode = async () => {
            try {
                await navigator.clipboard.writeText(optimized_code.value || "");
                copy_message.value = "已复制";
            } catch (e) {
                copy_message.value = "复制失败";
            }
            setTimeout(() => copy_message.value = "", 1500);
        };

        onBeforeUnmount(stopAnalysisPolling);

        refresh_bots();

        return {
            bots,
            selected_bot_id,
            mode,
            report,
            running,
            error_message,
            deepseek_running,
            analysis_report,
            hasDeepSeekAnalysis,
            optimized_code,
            copy_message,
            highlightedCode,
            copyOptimizedCode,
            run_evaluation,
            run_deepseek_analysis,
            load_latest_report,
            reasonText,
            openReplay,
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

.analysis-pending {
    padding: 14px 16px;
    border: 1px solid rgba(191, 120, 29, 0.2);
    border-radius: 8px;
    background: #fff8e8;
    color: #92400e;
    font-weight: 800;
}

.analysis-pending.failed {
    border-color: rgba(220, 53, 69, 0.22);
    background: #fff1f2;
    color: #b42334;
}

.formula {
    margin-top: 10px;
    color: #92400e;
    font-weight: 800;
}

.optimized-code {
    margin-top: 14px;
}

.optimized-code-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 8px;
}

.optimized-code h4 {
    margin: 0;
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
