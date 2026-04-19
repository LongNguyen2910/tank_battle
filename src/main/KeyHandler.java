package main;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class KeyHandler implements KeyListener {
    private final boolean[] keys = new boolean[256];

    public boolean isPressed(int keyCode) {
        return keyCode >= 0 && keyCode < keys.length && keys[keyCode];
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if(code < keys.length) {
            keys[code] = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        if(code < keys.length) {
            keys[code] = false;
        }
    }
}
