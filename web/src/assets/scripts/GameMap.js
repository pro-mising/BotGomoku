import { AcGameObjects } from "./AcGameObject";

export class GameMap extends AcGameObjects {
    constructor(ctx, parent, store) {
        super();

        this.ctx = ctx;
        this.parent = parent;
        this.store = store;
        this.rows = 15;
        this.cols = 15;
        this.L = 0;
        this.padding = 0;
        this.boardSize = 0;
        this.pieces = this.cloneBoard();
        this.lastMove = null;
        this.hoverCell = null;
        this.pendingMove = false;
    }

    cloneBoard() {
        if (this.store.state.record.is_record) {
            return Array.from({ length: this.rows }, () => Array(this.cols).fill(0));
        }
        return Array.from({ length: this.rows }, () => Array(this.cols).fill(0));
    }

    start() {
        this.add_listening_events();
        if (this.store.state.record.is_record) {
            this.play_record();
        }
    }

    isMyTurn() {
        return Number(this.store.state.pk.current_player) === Number(this.store.state.user.id);
    }

    cellFromEvent(e) {
        const rect = this.ctx.canvas.getBoundingClientRect();
        const x = e.clientX - rect.left;
        const y = e.clientY - rect.top;
        const c = Math.round((x - this.padding) / this.L);
        const r = Math.round((y - this.padding) / this.L);
        if (r < 0 || r >= this.rows || c < 0 || c >= this.cols) return null;

        const cellX = this.padding + c * this.L;
        const cellY = this.padding + r * this.L;
        const distance = Math.hypot(x - cellX, y - cellY);
        if (distance > this.L * 0.42) return null;
        return { r, c };
    }

    add_listening_events() {
        if (this.store.state.record.is_record) return;

        this.ctx.canvas.addEventListener("mousemove", e => {
            const cell = this.cellFromEvent(e);
            if (!cell || !this.isMyTurn() || this.pendingMove || this.pieces[cell.r][cell.c] !== 0) {
                this.hoverCell = null;
                this.ctx.canvas.style.cursor = "default";
                return;
            }
            this.hoverCell = cell;
            this.ctx.canvas.style.cursor = "pointer";
        });

        this.ctx.canvas.addEventListener("mouseleave", () => {
            this.hoverCell = null;
            this.ctx.canvas.style.cursor = "default";
        });

        this.ctx.canvas.addEventListener("click", e => {
            if (!this.store.state.pk.socket || !this.isMyTurn() || this.pendingMove) return;

            const cell = this.cellFromEvent(e);
            if (!cell || this.pieces[cell.r][cell.c] !== 0) return;

            this.pendingMove = true;
            this.store.state.pk.socket.send(JSON.stringify({
                event: "move",
                direction: cell.r * this.cols + cell.c,
            }));
        });
    }

    updateTurnState() {
        if (!this.isMyTurn()) {
            this.pendingMove = false;
            this.hoverCell = null;
        }
    }

    parseSteps(steps) {
        if (!steps) return [];
        return steps.split(',').filter(s => s.length > 0).map(s => parseInt(s));
    }

    play_record() {
        const aSteps = this.parseSteps(this.store.state.record.a_steps);
        const bSteps = this.parseSteps(this.store.state.record.b_steps);
        const moves = [];
        for (let i = 0; i < Math.max(aSteps.length, bSteps.length); i++) {
            if (i < aSteps.length) moves.push({ action: aSteps[i], piece: 1 });
            if (i < bSteps.length) moves.push({ action: bSteps[i], piece: 2 });
        }

        let k = 0;
        const intervalId = setInterval(() => {
            if (k >= moves.length) {
                clearInterval(intervalId);
                return;
            }
            const move = moves[k++];
            this.place(move.action, move.piece);
        }, 450);
    }

    place(action, piece) {
        const r = parseInt(action / this.cols);
        const c = action % this.cols;
        if (r < 0 || r >= this.rows || c < 0 || c >= this.cols) return;
        this.pieces[r][c] = piece;
        this.lastMove = { r, c };
        this.hoverCell = null;
        this.pendingMove = false;
    }

    update_size() {
        const size = parseInt(Math.min(this.parent.clientWidth, this.parent.clientHeight));
        if (this.ctx.canvas.width !== size || this.ctx.canvas.height !== size) {
            this.ctx.canvas.width = size;
            this.ctx.canvas.height = size;
        }
        this.boardSize = size;
        this.padding = parseInt(size * 0.075);
        this.L = (size - this.padding * 2) / (this.rows - 1);
    }

    update() {
        this.update_size();
        this.updateTurnState();
        this.render();
    }

    render() {
        const ctx = this.ctx;
        const size = this.boardSize;
        ctx.clearRect(0, 0, size, size);

        this.renderBoardBase(size);
        this.renderCoordinates(size);
        this.renderGrid(size);
        this.renderStars();
        this.renderHover();
        this.renderPieces();
    }

    renderBoardBase(size) {
        const ctx = this.ctx;
        ctx.save();
        ctx.fillStyle = "#f7faf8";
        ctx.fillRect(0, 0, size, size);

        const inset = Math.max(12, size * 0.022);
        ctx.fillStyle = "rgba(34, 77, 69, 0.1)";
        ctx.fillRect(inset + 8, inset + 9, size - inset * 2, size - inset * 2);

        const boardGradient = ctx.createLinearGradient(inset, inset, size - inset, size - inset);
        boardGradient.addColorStop(0, "#f3d99e");
        boardGradient.addColorStop(0.44, "#dfb86e");
        boardGradient.addColorStop(1, "#c7964d");
        ctx.fillStyle = boardGradient;
        ctx.shadowColor = "rgba(37, 55, 51, 0.24)";
        ctx.shadowBlur = 20;
        ctx.shadowOffsetY = 8;
        ctx.fillRect(inset, inset, size - inset * 2, size - inset * 2);

        ctx.shadowColor = "transparent";
        ctx.strokeStyle = "rgba(87, 57, 25, 0.66)";
        ctx.lineWidth = Math.max(2, size * 0.005);
        ctx.strokeRect(inset + 6, inset + 6, size - (inset + 6) * 2, size - (inset + 6) * 2);
        ctx.restore();
    }

    renderCoordinates() {
        const ctx = this.ctx;
        const letters = "ABCDEFGHIJKLMNO";
        ctx.save();
        ctx.fillStyle = "rgba(57, 43, 24, 0.62)";
        ctx.font = `${Math.max(10, this.L * 0.22)}px Arial`;
        ctx.textAlign = "center";
        ctx.textBaseline = "middle";

        for (let i = 0; i < this.rows; i++) {
            const p = this.padding + i * this.L;
            ctx.fillText(letters[i], p, this.padding * 0.48);
            ctx.fillText(String(i + 1), this.padding * 0.48, p);
        }
        ctx.restore();
    }

    renderGrid(size) {
        const ctx = this.ctx;
        ctx.save();
        ctx.strokeStyle = "rgba(56, 38, 20, 0.58)";
        ctx.lineWidth = Math.max(1, this.L * 0.016);
        for (let i = 0; i < this.rows; i++) {
            const p = this.padding + i * this.L;
            ctx.beginPath();
            ctx.moveTo(this.padding, p);
            ctx.lineTo(size - this.padding, p);
            ctx.stroke();

            ctx.beginPath();
            ctx.moveTo(p, this.padding);
            ctx.lineTo(p, size - this.padding);
            ctx.stroke();
        }
        ctx.restore();
    }

    renderStars() {
        const points = [[3, 3], [3, 11], [7, 7], [11, 3], [11, 11]];
        this.ctx.fillStyle = "rgba(56, 38, 20, 0.8)";
        for (const [r, c] of points) {
            this.ctx.beginPath();
            this.ctx.arc(this.padding + c * this.L, this.padding + r * this.L, this.L * 0.072, 0, Math.PI * 2);
            this.ctx.fill();
        }
    }

    renderHover() {
        if (!this.hoverCell) return;
        const piece = Number(this.store.state.pk.current_player) === Number(this.store.state.pk.a_id) ? 1 : 2;
        const x = this.padding + this.hoverCell.c * this.L;
        const y = this.padding + this.hoverCell.r * this.L;
        this.ctx.save();
        this.ctx.globalAlpha = 0.38;
        this.drawPiece(x, y, piece, this.L * 0.34);
        this.ctx.restore();
    }

    renderPieces() {
        for (let r = 0; r < this.rows; r++) {
            for (let c = 0; c < this.cols; c++) {
                const piece = this.pieces[r][c];
                if (piece === 0) continue;
                this.drawPiece(this.padding + c * this.L, this.padding + r * this.L, piece, this.L * 0.35);
            }
        }

        if (this.lastMove) {
            const x = this.padding + this.lastMove.c * this.L;
            const y = this.padding + this.lastMove.r * this.L;
            this.ctx.strokeStyle = "#e45b3f";
            this.ctx.lineWidth = Math.max(2, this.L * 0.035);
            this.ctx.beginPath();
            this.ctx.arc(x, y, this.L * 0.18, 0, Math.PI * 2);
            this.ctx.stroke();
        }
    }

    drawPiece(x, y, piece, radius) {
        const ctx = this.ctx;
        ctx.save();
        ctx.shadowColor = "rgba(0, 0, 0, 0.24)";
        ctx.shadowBlur = radius * 0.28;
        ctx.shadowOffsetY = radius * 0.18;

        const gradient = ctx.createRadialGradient(
            x - radius * 0.35,
            y - radius * 0.35,
            radius * 0.1,
            x,
            y,
            radius,
        );

        if (piece === 1) {
            gradient.addColorStop(0, "#8f8f8f");
            gradient.addColorStop(0.22, "#343434");
            gradient.addColorStop(1, "#050505");
        } else {
            gradient.addColorStop(0, "#ffffff");
            gradient.addColorStop(0.6, "#f5f2eb");
            gradient.addColorStop(1, "#c9c1b4");
        }

        ctx.fillStyle = gradient;
        ctx.beginPath();
        ctx.arc(x, y, radius, 0, Math.PI * 2);
        ctx.fill();
        ctx.shadowColor = "transparent";
        ctx.strokeStyle = piece === 1 ? "rgba(0, 0, 0, 0.58)" : "rgba(90, 90, 90, 0.36)";
        ctx.lineWidth = Math.max(1, radius * 0.06);
        ctx.stroke();
        ctx.restore();
    }
}
