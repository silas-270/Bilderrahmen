package display;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferStrategy;
import util.Scale;

/**
 * Erstellt und verwaltet das Vollbild-JFrame.
 * Enthält das Canvas, auf dem der Renderer zeichnet.
 * Leitet Mausklicks und Mausbewegungen an registrierte Callbacks weiter.
 */
public class FullscreenWindow {

    /** Callback-Interface für Klicks mit Koordinaten. */
    @FunctionalInterface
    public interface ClickCallback {
        void onClick(int x, int y);
    }

    @FunctionalInterface
    public interface SwipeCallback {
        void onSwipe();
    }

    /** Callback-Interface für Mausbewegungen (Hover). */
    @FunctionalInterface
    public interface MoveCallback {
        void onMove(int x, int y);
    }

    // -------------------------------------------------------------------------

    private final JFrame frame;
    private final Canvas canvas;

    private ClickCallback onClickCallback;
    private MoveCallback  onMoveCallback;
    private SwipeCallback onSwipeLeft;
    private SwipeCallback onSwipeRight;

    private int pressX; // Startpunkt für Swipe-Erkennung

    // -------------------------------------------------------------------------

    public FullscreenWindow() {
        frame = new JFrame("Digitaler Bilderrahmen");
        canvas = new Canvas();

        canvas.setBackground(Color.BLACK);
        canvas.setIgnoreRepaint(true); // Wir zeichnen selbst über BufferStrategy

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setUndecorated(true);          // Kein Fensterrahmen
        frame.setBackground(Color.BLACK);
        frame.add(canvas);

        // Vollbild-Modus aktivieren
        GraphicsDevice gd = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getDefaultScreenDevice();

        if (gd.isFullScreenSupported()) {
            gd.setFullScreenWindow(frame);
        } else {
            // Fallback: maximiertes Fenster
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            frame.setVisible(true);
        }

        frame.toFront();
        frame.requestFocus();

        // Doppelpuffer einrichten (nötig für aktives Rendering)
        canvas.createBufferStrategy(2);

        // Mauseingabe: Klicks
        canvas.addMouseListener(new MouseAdapter() {
            private long pressTime;

            @Override
            public void mousePressed(MouseEvent e) {
                pressTime = System.currentTimeMillis();
                pressX = e.getX();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                long held = System.currentTimeMillis() - pressTime;
                int releaseX = e.getX();
                int diffX = releaseX - pressX;
                int threshold = Scale.get(100, getHeight()); // Pixel für Swipe-Erkennung

                // 1. Swipe prüfen
                if (held < 500 && Math.abs(diffX) > threshold) {
                    if (diffX < 0 && onSwipeLeft != null) {
                        onSwipeLeft.onSwipe();
                        return; // Kein normaler Klick wenn Swipe
                    } else if (diffX > 0 && onSwipeRight != null) {
                        onSwipeRight.onSwipe();
                        return; // Kein normaler Klick wenn Swipe
                    }
                }

                // 2. Normaler Klick
                if (held < 500 && onClickCallback != null) {
                    onClickCallback.onClick(e.getX(), e.getY());
                }
            }
        });

        // Mauseingabe: Bewegung (für Hover-Effekte)
        canvas.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (onMoveCallback != null) {
                    onMoveCallback.onMove(e.getX(), e.getY());
                }
            }
        });

        // Tastatureingabe: Esc zum Beenden
        canvas.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE) {
                    System.exit(0);
                }
            }
        });
    }

    // -------------------------------------------------------------------------
    // Public API

    /** Gibt die BufferStrategy des Canvas zurück (wird vom Renderer benötigt). */
    public BufferStrategy getBufferStrategy() {
        return canvas.getBufferStrategy();
    }

    /** Breite des Canvas in Pixeln. */
    public int getWidth() {
        return canvas.getWidth();
    }

    /** Höhe des Canvas in Pixeln. */
    public int getHeight() {
        return canvas.getHeight();
    }

    /**
     * Setzt den Callback für Mausklicks (mit Koordinaten).
     * Typischerweise: Menü öffnen/schließen + Button-Auswertung.
     */
    public void setOnClickCallback(ClickCallback callback) {
        this.onClickCallback = callback;
    }

    /**
     * Setzt den Callback für Mausbewegungen (Hover-Effekte).
     */
    public void setOnMoveCallback(MoveCallback callback) {
        this.onMoveCallback = callback;
    }

    public void setOnSwipeLeft(SwipeCallback callback) {
        this.onSwipeLeft = callback;
    }

    public void setOnSwipeRight(SwipeCallback callback) {
        this.onSwipeRight = callback;
    }

    /** Minimiert das Fenster in die Taskleiste. */
    public void minimize() {
        frame.setExtendedState(JFrame.ICONIFIED);
    }

    /** Beendet die Anwendung und verlässt den Vollbildmodus. */
    public void dispose() {
        GraphicsDevice gd = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getDefaultScreenDevice();
        gd.setFullScreenWindow(null);
        frame.dispose();
    }
}