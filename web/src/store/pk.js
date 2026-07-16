//存储和pk相关的全局变量

export default {
    state: {
        status: "matching", //matching表示匹配界面, playing表示对战界面
        socket: null,
        opponent_username: "",
        opponent_photo: "",
        gamemap: null,
        a_id: 0,
        a_sx: 0,
        a_sy: 0,
        b_id: 0,
        b_sx: 0,
        b_sy: 0,
        current_player: 0,
        gameObject: null,
        loser: "none", //all, A, B
        winner: "none",
        result_reason: "",
        turn_time_limit: 15,
        turn_deadline: 0,
    },
    getters: {
    },
    mutations: {
        updateSocket(state, socket) {
            state.socket = socket;
        },
        updateOpponent(state, opponent) {
            state.opponent_username = opponent.username;
            state.opponent_photo = opponent.photo;
        },
        updateStatus(state, status) {
            state.status = status;
        },
        updateGame(state, game) {
            state.gamemap = Array.from({ length: 15 }, () => Array(15).fill(0));
            state.a_id = Number(game.a_id);
            state.a_sx = game.a_sx;
            state.a_sy = game.a_sy;
            state.b_id = Number(game.b_id);
            state.b_sx = game.b_sx;
            state.b_sy = game.b_sy;
            state.current_player = Number(game.current_player || game.a_id);
            state.turn_deadline = Date.now() + state.turn_time_limit * 1000;
        },
        updateGameObject(state, gameObject) {
            state.gameObject = gameObject;
        },
        updateLoser(state, loser) {
            state.loser = loser;
        },
        updateResult(state, result) {
            state.loser = result.loser;
            state.winner = result.winner || (result.loser === "A" ? "B" : (result.loser === "B" ? "A" : "all"));
            state.result_reason = result.reason || "";
            state.turn_deadline = 0;
        },
        resetResult(state) {
            state.loser = "none";
            state.winner = "none";
            state.result_reason = "";
            state.turn_deadline = 0;
        },
        updateCurrentPlayer(state, userId) {
            state.current_player = Number(userId);
            state.turn_deadline = Date.now() + state.turn_time_limit * 1000;
        }
    },
    actions: {
    },
    modules: {
    }
}
