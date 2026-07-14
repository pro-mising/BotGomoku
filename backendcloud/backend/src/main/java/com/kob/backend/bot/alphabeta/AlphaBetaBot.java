package com.kob.backend.bot.alphabeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AlphaBetaBot {
    private static final int SIZE = 15;
    private static final int EMPTY = 0;
    private static final int BLACK = 1;
    private static final int WHITE = 2;
    private static final int WIN_SCORE = 10_000_000;
    private static final int INF = 1_000_000_000;
    private static final int MAX_DEPTH = 2;
    private static final long TIME_LIMIT_MS = 900;
    private static final int[][] DIRS = {{0, 1}, {1, 0}, {1, 1}, {1, -1}};

    private long deadline;
    private int rootPlayer;

    public int nextMove(int[][] board, int player) {
        this.rootPlayer = player;
        this.deadline = System.currentTimeMillis() + TIME_LIMIT_MS;

        if (isEmpty(board)) return encode(SIZE / 2, SIZE / 2);

        Integer winningMove = findImmediateMove(board, player);
        if (winningMove != null) return winningMove;

        Integer blockingMove = findImmediateMove(board, opponent(player));
        if (blockingMove != null) return blockingMove;

        List<Move> candidates = generateCandidates(board, player);
        if (candidates.isEmpty()) return firstEmpty(board);

        int bestScore = -INF;
        Move bestMove = candidates.get(0);

        for (Move move : candidates) {
            if (timeout()) break;
            board[move.row][move.col] = player;
            int score = alphaBeta(board, MAX_DEPTH - 1, -INF, INF, opponent(player), false);
            board[move.row][move.col] = EMPTY;

            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }

        return encode(bestMove.row, bestMove.col);
    }

    private int alphaBeta(int[][] board, int depth, int alpha, int beta, int player, boolean maximizing) {
        if (timeout() || depth == 0) return evaluate(board, rootPlayer);

        List<Move> candidates = generateCandidates(board, player);
        if (candidates.isEmpty()) return evaluate(board, rootPlayer);

        if (maximizing) {
            int value = -INF;
            for (Move move : candidates) {
                board[move.row][move.col] = player;
                if (hasFive(board, move.row, move.col, player)) {
                    board[move.row][move.col] = EMPTY;
                    return WIN_SCORE;
                }
                value = Math.max(value, alphaBeta(board, depth - 1, alpha, beta, opponent(player), false));
                board[move.row][move.col] = EMPTY;
                alpha = Math.max(alpha, value);
                if (alpha >= beta || timeout()) break;
            }
            return value;
        } else {
            int value = INF;
            for (Move move : candidates) {
                board[move.row][move.col] = player;
                if (hasFive(board, move.row, move.col, player)) {
                    board[move.row][move.col] = EMPTY;
                    return -WIN_SCORE;
                }
                value = Math.min(value, alphaBeta(board, depth - 1, alpha, beta, opponent(player), true));
                board[move.row][move.col] = EMPTY;
                beta = Math.min(beta, value);
                if (alpha >= beta || timeout()) break;
            }
            return value;
        }
    }

    private Integer findImmediateMove(int[][] board, int player) {
        for (Move move : generateCandidates(board, player)) {
            board[move.row][move.col] = player;
            boolean win = hasFive(board, move.row, move.col, player);
            board[move.row][move.col] = EMPTY;
            if (win) return encode(move.row, move.col);
        }
        return null;
    }

    private List<Move> generateCandidates(int[][] board, int player) {
        boolean[][] seen = new boolean[SIZE][SIZE];
        List<Move> moves = new ArrayList<>();

        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (board[r][c] == EMPTY) continue;
                for (int dr = -2; dr <= 2; dr++) {
                    for (int dc = -2; dc <= 2; dc++) {
                        int nr = r + dr, nc = c + dc;
                        if (!inside(nr, nc) || board[nr][nc] != EMPTY || seen[nr][nc]) continue;
                        seen[nr][nc] = true;
                        moves.add(new Move(nr, nc, moveScore(board, nr, nc, player)));
                    }
                }
            }
        }

        moves.sort(Comparator.comparingInt((Move move) -> move.score).reversed());
        if (moves.size() > 16) return new ArrayList<>(moves.subList(0, 16));
        return moves;
    }

    private int moveScore(int[][] board, int row, int col, int player) {
        board[row][col] = player;
        int attack = evaluatePoint(board, row, col, player);
        board[row][col] = opponent(player);
        int defense = evaluatePoint(board, row, col, opponent(player));
        board[row][col] = EMPTY;
        return attack + (int) (defense * 1.12);
    }

    private int evaluate(int[][] board, int player) {
        int myScore = evaluatePlayer(board, player);
        int enemyScore = evaluatePlayer(board, opponent(player));
        return myScore - (int) (enemyScore * 1.08);
    }

    private int evaluatePlayer(int[][] board, int player) {
        int score = 0;
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (board[r][c] == player) score += evaluatePoint(board, r, c, player);
            }
        }
        return score;
    }

    private int evaluatePoint(int[][] board, int row, int col, int player) {
        int score = 0;
        for (int[] dir : DIRS) {
            int count = 1;
            int open = 0;

            int r = row + dir[0], c = col + dir[1];
            while (inside(r, c) && board[r][c] == player) {
                count++;
                r += dir[0];
                c += dir[1];
            }
            if (inside(r, c) && board[r][c] == EMPTY) open++;

            r = row - dir[0];
            c = col - dir[1];
            while (inside(r, c) && board[r][c] == player) {
                count++;
                r -= dir[0];
                c -= dir[1];
            }
            if (inside(r, c) && board[r][c] == EMPTY) open++;

            score += patternScore(count, open);
        }
        return score;
    }

    private int patternScore(int count, int open) {
        if (count >= 5) return WIN_SCORE;
        if (count == 4 && open == 2) return 1_000_000;
        if (count == 4 && open == 1) return 120_000;
        if (count == 3 && open == 2) return 30_000;
        if (count == 3 && open == 1) return 6_000;
        if (count == 2 && open == 2) return 1_200;
        if (count == 2 && open == 1) return 300;
        if (count == 1 && open == 2) return 30;
        return 0;
    }

    private boolean hasFive(int[][] board, int row, int col, int player) {
        for (int[] dir : DIRS) {
            int count = 1;
            int r = row + dir[0], c = col + dir[1];
            while (inside(r, c) && board[r][c] == player) {
                count++;
                r += dir[0];
                c += dir[1];
            }
            r = row - dir[0];
            c = col - dir[1];
            while (inside(r, c) && board[r][c] == player) {
                count++;
                r -= dir[0];
                c -= dir[1];
            }
            if (count >= 5) return true;
        }
        return false;
    }

    private boolean isEmpty(int[][] board) {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (board[r][c] != EMPTY) return false;
            }
        }
        return true;
    }

    private int firstEmpty(int[][] board) {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (board[r][c] == EMPTY) return encode(r, c);
            }
        }
        return 0;
    }

    private int opponent(int player) {
        return player == BLACK ? WHITE : BLACK;
    }

    private boolean inside(int row, int col) {
        return row >= 0 && row < SIZE && col >= 0 && col < SIZE;
    }

    private int encode(int row, int col) {
        return row * SIZE + col;
    }

    private boolean timeout() {
        return System.currentTimeMillis() >= deadline;
    }

    private static class Move {
        private final int row;
        private final int col;
        private final int score;

        private Move(int row, int col, int score) {
            this.row = row;
            this.col = col;
            this.score = score;
        }
    }
}
