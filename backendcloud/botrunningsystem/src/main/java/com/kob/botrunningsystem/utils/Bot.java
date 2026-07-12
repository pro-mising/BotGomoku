package com.kob.botrunningsystem.utils;

public class Bot implements com.kob.botrunningsystem.utils.BotInterface {
    private static final int SIZE = 15;

    @Override
    public Integer nextMove(String input) {
        String[] parts = input.split("#");
        String board = parts[0];
        int myPiece = Integer.parseInt(parts[1]);
        int opponentPiece = myPiece == 1 ? 2 : 1;

        int[][] g = new int[SIZE][SIZE];
        for (int i = 0, k = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++, k++) {
                g[i][j] = board.charAt(k) - '0';
            }
        }

        Integer win = findBestMove(g, myPiece);
        if (win != null) return win;

        Integer block = findBestMove(g, opponentPiece);
        if (block != null) return block;

        int center = SIZE / 2;
        if (g[center][center] == 0) return center * SIZE + center;

        for (int radius = 1; radius < SIZE; radius++) {
            for (int i = center - radius; i <= center + radius; i++) {
                for (int j = center - radius; j <= center + radius; j++) {
                    if (i >= 0 && i < SIZE && j >= 0 && j < SIZE && g[i][j] == 0) {
                        return i * SIZE + j;
                    }
                }
            }
        }

        return 0;
    }

    private Integer findBestMove(int[][] g, int piece) {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (g[i][j] != 0) continue;
                g[i][j] = piece;
                boolean ok = hasFive(g, i, j, piece);
                g[i][j] = 0;
                if (ok) return i * SIZE + j;
            }
        }
        return null;
    }

    private boolean hasFive(int[][] g, int row, int col, int piece) {
        int[] dx = {1, 0, 1, 1};
        int[] dy = {0, 1, 1, -1};
        for (int d = 0; d < 4; d++) {
            int count = 1;
            for (int k = 1; ; k++) {
                int x = row + dx[d] * k, y = col + dy[d] * k;
                if (x < 0 || x >= SIZE || y < 0 || y >= SIZE || g[x][y] != piece) break;
                count++;
            }
            for (int k = 1; ; k++) {
                int x = row - dx[d] * k, y = col - dy[d] * k;
                if (x < 0 || x >= SIZE || y < 0 || y >= SIZE || g[x][y] != piece) break;
                count++;
            }
            if (count >= 5) return true;
        }
        return false;
    }
}
