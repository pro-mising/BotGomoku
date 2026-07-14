package com.kob.backend.bot.builtin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GomokuBotSupport {
    public static final int SIZE = 15;
    public static final int EMPTY = 0;
    public static final int BLACK = 1;
    public static final int WHITE = 2;
    public static final int[][] DIRS = {{0, 1}, {1, 0}, {1, 1}, {1, -1}};

    public static int opponent(int player) {
        return player == BLACK ? WHITE : BLACK;
    }

    public static int encode(int row, int col) {
        return row * SIZE + col;
    }

    public static boolean inside(int row, int col) {
        return row >= 0 && row < SIZE && col >= 0 && col < SIZE;
    }

    public static boolean isEmpty(int[][] board) {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (board[r][c] != EMPTY) return false;
            }
        }
        return true;
    }

    public static boolean hasFive(int[][] board, int row, int col, int player) {
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

    public static Integer findImmediateMove(int[][] board, int player) {
        for (Move move : generateCandidates(board, player, 40)) {
            board[move.row][move.col] = player;
            boolean win = hasFive(board, move.row, move.col, player);
            board[move.row][move.col] = EMPTY;
            if (win) return encode(move.row, move.col);
        }
        return null;
    }

    public static List<Move> generateCandidates(int[][] board, int player, int limit) {
        if (isEmpty(board)) {
            List<Move> moves = new ArrayList<>();
            moves.add(new Move(SIZE / 2, SIZE / 2, 0));
            return moves;
        }

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
        if (moves.size() > limit) return new ArrayList<>(moves.subList(0, limit));
        return moves;
    }

    public static int moveScore(int[][] board, int row, int col, int player) {
        board[row][col] = player;
        int attack = evaluatePoint(board, row, col, player);
        board[row][col] = opponent(player);
        int defense = evaluatePoint(board, row, col, opponent(player));
        board[row][col] = EMPTY;
        return attack + (int) (defense * 1.08);
    }

    public static int evaluatePoint(int[][] board, int row, int col, int player) {
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

    public static int patternScore(int count, int open) {
        if (count >= 5) return 10_000_000;
        if (count == 4 && open == 2) return 1_000_000;
        if (count == 4 && open == 1) return 120_000;
        if (count == 3 && open == 2) return 30_000;
        if (count == 3 && open == 1) return 6_000;
        if (count == 2 && open == 2) return 1_200;
        if (count == 2 && open == 1) return 300;
        if (count == 1 && open == 2) return 30;
        return 0;
    }

    public static int firstEmpty(int[][] board) {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (board[r][c] == EMPTY) return encode(r, c);
            }
        }
        return 0;
    }

    public static int[][] copy(int[][] board) {
        int[][] res = new int[SIZE][SIZE];
        for (int i = 0; i < SIZE; i++) {
            System.arraycopy(board[i], 0, res[i], 0, SIZE);
        }
        return res;
    }

    public static class Move {
        public final int row;
        public final int col;
        public final int score;

        public Move(int row, int col, int score) {
            this.row = row;
            this.col = col;
            this.score = score;
        }
    }
}
