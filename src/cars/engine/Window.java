package cars.engine;

import cars.student.Setup;
import cars.student.TestMap;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public final class Window extends JFrame implements Runnable {
    public static final int INITIAL_WIDTH = 1024;
    public static final int INITIAL_HEIGHT = 768;

    private final Setup setup = new Setup();
    private volatile List<Car> cars;
    private volatile Vector2 clickPos = null;
    private volatile Vector2 mousePos = null;
    private volatile boolean debugMode = true;
    private volatile int currentScenario = 0;

    private Window() {
        super("Steering Behaviors - (SBCarsEngine)");
        setSize(INITIAL_WIDTH, INITIAL_HEIGHT);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setIgnoreRepaint(true);
        setResizable(true);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                start();
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    clickPos = null;
                } else {
                    clickPos = new Vector2(
                        e.getX() - getWidth() / 2.0,
                        e.getY() - getHeight() / 2.0
                    );
                }
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mousePos = new Vector2(
                    e.getX() - getWidth() / 2.0,
                    e.getY() - getHeight() / 2.0
                );
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                mousePos = new Vector2(
                    e.getX() - getWidth() / 2.0,
                    e.getY() - getHeight() / 2.0
                );
                if (SwingUtilities.isLeftMouseButton(e)) {
                    clickPos = mousePos.clone();
                }
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                final char ch = Character.toUpperCase(e.getKeyChar());
                if (ch == 'D') {
                    debugMode = !debugMode;
                } else if (ch == 'C') {
                    clickPos = null;
                } else if (ch == 'R') {
                    cars = setup.createCarsForScenario(currentScenario);
                } else if (ch >= '0' && ch <= '9') {
                    currentScenario = ch - '0';
                    cars = setup.createCarsForScenario(currentScenario);
                }
            }
        });
        requestFocus();
        this.cars = setup.createCars();
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> new Window().setVisible(true));
    }

    private void start() {
        createBufferStrategy(3);
        final var gameLoop = new Thread(this, "game-loop");
        gameLoop.setDaemon(true);
        gameLoop.start();
    }

    @Override
    public void run() {
        double prev = System.currentTimeMillis() - 1;
        try {
            final var strategy = getBufferStrategy();
            while (true) {
                final double now = System.currentTimeMillis();
                final double dt = Math.min(0.05, Math.max(0.001, (now - prev) / 1000.0));
                final var g2d = (Graphics2D) strategy.getDrawGraphics();

                draw(g2d);
                update(dt);

                g2d.dispose();
                Toolkit.getDefaultToolkit().sync(); // helps on some systems
                prev = now;
                strategy.show();

                Thread.sleep(1);
            }
        } catch (InterruptedException e) {
            System.err.println("Interrupted");
        }
        System.exit(0);
    }

    private void update(final double secs) {
        final var activeCars = this.cars;
        activeCars.forEach(car -> car.update(
            new World(
                secs, car, activeCars,
                mousePos, clickPos,
                getWidth(), getHeight()
            )
        ));
    }

    private void draw(Graphics2D g2d) {
        // Quality hints
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Clear background
        g2d.setBackground(new Color(228, 232, 224));
        g2d.clearRect(0, 0, getWidth(), getHeight());

        // Center the world origin
        g2d.translate(getWidth() / 2.0, getHeight() / 2.0);

        // Desenha pista e obstáculos conforme o cenário ativo
        final boolean showTrack = (currentScenario == 0 || currentScenario == 6 || currentScenario == 7);
        final boolean showObstacles = (currentScenario == 0 || currentScenario == 5 || currentScenario >= 7);
        TestMap.draw(g2d, debugMode, showTrack, showObstacles);

        // Click marker (alvo de Arrive / Pit-Stop)
        if (clickPos != null) {
            g2d.setColor(new Color(30, 160, 75, 190));
            g2d.fillOval((int) clickPos.x - 6, (int) clickPos.y - 6, 12, 12);
            g2d.setColor(Color.WHITE);
            g2d.drawOval((int) clickPos.x - 6, (int) clickPos.y - 6, 12, 12);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 10));
            g2d.setColor(new Color(20, 100, 45));
            g2d.drawString("Alvo (Clique)", (float) clickPos.x + 9, (float) clickPos.y + 4);
        }

        // Draw cars
        final var activeCars = this.cars;
        activeCars.forEach(car -> car.draw(g2d, debugMode));

        // HUD / Painel informativo no topo e rodapé (em coordenadas da janela)
        drawHud(g2d);
    }

    private void drawHud(Graphics2D g2d) {
        final float left = -getWidth() / 2.0f + 25.0f;
        final float top = -getHeight() / 2.0f + 25.0f;

        // Caixa superior com título do cenário ativo e atalhos
        g2d.setColor(new Color(20, 24, 32, 200));
        g2d.fillRoundRect((int) left, (int) top, 760, 62, 10, 10);

        g2d.setFont(new Font("SansSerif", Font.BOLD, 13));
        g2d.setColor(new Color(255, 215, 70));
        g2d.drawString(Setup.getScenarioTitle(currentScenario), left + 12, top + 20);

        g2d.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g2d.setColor(new Color(225, 230, 240));
        g2d.drawString(
            "Teclas Simples: [1] Seek  [2] Flee  [3] Arrive  [4] Wander   |   Avançados: [5] Obstacle Avoidance  [6] Path Following",
            left + 12, top + 38
        );
        g2d.drawString(
            "Combinações: [7] Combo 1 (Racer)  [8] Combo 2 (Explorador Evasivo)  [9] Combo 3 (Tático)  [0] Todos",
            left + 12, top + 54
        );

        // Rodapé com instruções de mouse e debug
        final float bottomY = getHeight() / 2.0f - 18.0f;
        g2d.setColor(new Color(30, 35, 45, 190));
        g2d.fillRoundRect((int) left, (int) (bottomY - 18), 760, 24, 8, 8);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 11));
        g2d.setColor(Color.WHITE);
        g2d.drawString(
            "[D] Debug: %s   |   [Clique Esq] Definir Alvo/Pit-Stop   |   [Clique Dir / C] Limpar Alvo   |   [R] Reiniciar Cenário"
                .formatted(debugMode ? "ON (Vetores: Azul=Velocidade, Laranja=Steering)" : "OFF"),
            left + 12, bottomY - 2
        );
    }
}
