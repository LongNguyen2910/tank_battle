package main;

import java.awt.*;
import java.awt.event.KeyEvent;

public class Config {
    // SCREEN SETTINGS
    public static final int ORIGINAL_TILE_SIZE = 32;
    public static final int SCALE = 2;
    public static final int TILE_SIZE = ORIGINAL_TILE_SIZE * SCALE;
    public static final int MAX_SCREEN_COL = 16;
    public static final int MAX_SCREEN_ROW = 12;
    public static final int SCREEN_WIDTH = TILE_SIZE * MAX_SCREEN_COL;
    public static final int SCREEN_HEIGHT = TILE_SIZE * MAX_SCREEN_ROW;

    // MENU SETTINGS
    public static final int MENU_WIDTH = 1000;
    public static final int MENU_HEIGHT = 700;

    // GAME SETTINGS
    public static final int FPS = 60;

    // SPAWN SETTINGS
    public static final int X_SPAWN_PLAYER_1 = 2 * TILE_SIZE + 10;
    public static final int Y_SPAWN_PLAYER_1 = SCREEN_HEIGHT - 2 * TILE_SIZE - 10;

    public static final int X_SPAWN_PLAYER_2 = SCREEN_WIDTH - 3 * TILE_SIZE;
    public static final int Y_SPAWN_PLAYER_2 = SCREEN_HEIGHT - 2 * TILE_SIZE - 10;

    public static final int X_SPAWN_PLAYER_3 = 2 * TILE_SIZE;
    public static final int Y_SPAWN_PLAYER_3 = 2 * TILE_SIZE;

    public static final int X_SPAWN_PLAYER_4 = SCREEN_WIDTH - 3 * TILE_SIZE;
    public static final int Y_SPAWN_PLAYER_4 = 2 * TILE_SIZE;

    // KEY SETTINGS
    public static int P1_UP = KeyEvent.VK_W;
    public static int P1_DOWN = KeyEvent.VK_S;
    public static int P1_LEFT = KeyEvent.VK_A;
    public static int P1_RIGHT = KeyEvent.VK_D;
    public static int P1_SHOOT = KeyEvent.VK_J;
    public static int P1_DASH = KeyEvent.VK_K;
    public static int P1_SKILL1 = KeyEvent.VK_U;
    public static int P1_SKILL2 = KeyEvent.VK_I;

    public static int P2_UP = KeyEvent.VK_UP;
    public static int P2_DOWN = KeyEvent.VK_DOWN;
    public static int P2_LEFT = KeyEvent.VK_LEFT;
    public static int P2_RIGHT = KeyEvent.VK_RIGHT;
    public static int P2_SHOOT = KeyEvent.VK_NUMPAD2;
    public static int P2_DASH = KeyEvent.VK_M;
    public static int P2_SKILL1 = KeyEvent.VK_NUMPAD4;
    public static int P2_SKILL2 = KeyEvent.VK_NUMPAD5;

    // GAMEPLAY SETTINGS
    public static int MAX_HEALTH = 100;
    public static int MAX_FUEL = 100; // This will be the base for all tanks
    public static String DIFFICULTY = "NORMAL";
    public static int MATCH_TIME = 180; // in seconds
}
