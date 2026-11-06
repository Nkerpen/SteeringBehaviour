package cars.student;

import cars.engine.Car;
import cars.engine.Settings;
import cars.engine.Vector2;
import cars.engine.World;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Consumer;

import static cars.engine.Vector2.*;

/**
 * Agente autônomo que implementa os comportamentos individuais (Seek, Flee, Arrive,
 * Wander, Obstacle Avoidance, Path Following) e as combinações ponderadas de
 * Steering Behaviors solicitadas no trabalho prático.
 */
public class StudentCar extends Car {

    public enum BehaviorMode {
        SEEK(
            "Seek",
            "Simples: Persegue a posição do cursor do mouse em velocidade máxima"
        ),
        FLEE(
            "Flee",
            "Simples: Foge do cursor do mouse ao entrar no raio de pânico (240px)"
        ),
        ARRIVE(
            "Arrive",
            "Simples: Vai até o clique (ou mouse) desacelerando no raio de chegada (220px)"
        ),
        WANDER(
            "Wander",
            "Simples: Explora o mapa usando o círculo projetado de Reynolds"
        ),
        OBSTACLE_AVOIDANCE(
            "Obstacle Avoidance",
            "Avançado: Desvia de obstáculos estáticos e carros vizinhos usando sensor frontal"
        ),
        PATH_FOLLOWING(
            "Path Following",
            "Avançado: Segue continuamente o circuito fechado de waypoints"
        ),
        COMBO_RACER(
            "Combo 1: Path + Avoid",
            "Combinação 1: Path Following (1.0) + Obstacle Avoidance (2.4) [+ Arrive no Clique (1.3)]"
        ),
        COMBO_EXPLORER_EVADER(
            "Combo 2: Wander + Avoid + Flee",
            "Combinação 2: Wander (0.8) + Obstacle Avoidance (2.5) + Flee do Mouse (1.8)"
        ),
        COMBO_TACTICAL_ARRIVE(
            "Combo 3: Arrive + Avoid",
            "Combinação 3: Arrive ao Clique/Mouse (1.1) + Obstacle Avoidance (2.3)"
        );

        private final String shortName;
        private final String description;

        BehaviorMode(String shortName, String description) {
            this.shortName = shortName;
            this.description = description;
        }

        public String getShortName() {
            return shortName;
        }

        public String getDescription() {
            return description;
        }
    }

    // Parâmetros de calibração dos comportamentos
    public static final double FLEE_PANIC_DISTANCE = 240.0;
    public static final double ARRIVE_SLOWING_RADIUS = 220.0;
    public static final double ARRIVE_STOP_RADIUS = 8.0;
    public static final double WANDER_DISTANCE = 92.0;
    public static final double WANDER_RADIUS = 48.0;
    public static final double WANDER_JITTER = 4.5;
    public static final double AVOID_MIN_LOOKAHEAD = 65.0;
    public static final double AVOID_MAX_LOOKAHEAD = 165.0;
    public static final double PATH_PREDICT_TIME = 0.42;
    public static final double PATH_LOOKAHEAD_DIST = 62.0;

    // Pesos calibrados para as Combinações de Comportamentos
    // Combinação 1 (Corredor Autônomo): Path Following + Obstacle Avoidance (+ Arrive opcional no clique)
    public static final double WEIGHT_RACER_PATH = 1.0;
    public static final double WEIGHT_RACER_AVOID = 2.4;
    public static final double WEIGHT_RACER_PITSTOP = 1.3;

    // Combinação 2 (Explorador Evasivo): Wander + Obstacle Avoidance + Flee do Mouse
    public static final double WEIGHT_EXPLORER_WANDER = 0.8;
    public static final double WEIGHT_EXPLORER_FLEE = 1.8;
    public static final double WEIGHT_EXPLORER_AVOID = 2.5;

    // Combinação 3 (Navegador Tático): Arrive + Obstacle Avoidance
    public static final double WEIGHT_TACTICAL_ARRIVE = 1.1;
    public static final double WEIGHT_TACTICAL_AVOID = 2.3;

    private final String agentName;
    private BehaviorMode mode;
    private double wanderAngle = 0.0;

    // Estado de debug visual (atualizado a cada frame)
    private Vector2 debugWanderCenter;
    private Vector2 debugWanderTarget;
    private double debugWanderRadius;
    private double debugLookAhead;
    private Vector2 debugThreatObstacle;
    private Vector2 debugFuturePos;
    private Vector2 debugNormalPoint;
    private Vector2 debugPathTarget;
    private Vector2 debugActiveTarget;

    /**
     * Construtor padrão exigido pelo template original.
     */
    public StudentCar() {
        this("Agente 1", BehaviorMode.COMBO_RACER, settings ->
            settings
                .color(new Color(30, 115, 230))
                .position(-350, -230)
                .orientationDegrees(0)
                .maxSpeed(320)
                .maxForce(380)
        );
    }

    public StudentCar(String agentName, BehaviorMode mode, Consumer<Settings> customSettings) {
        super(customSettings);
        this.agentName = agentName;
        this.mode = mode;
    }

    public String getAgentName() {
        return agentName;
    }

    public BehaviorMode getMode() {
        return mode;
    }

    public void setMode(BehaviorMode mode) {
        this.mode = mode;
    }

    public double getWanderAngle() {
        return wanderAngle;
    }

    public void setWanderAngle(double wanderAngle) {
        this.wanderAngle = wanderAngle;
    }

    public void setDebugWander(Vector2 center, Vector2 target, double radius) {
        this.debugWanderCenter = center;
        this.debugWanderTarget = target;
        this.debugWanderRadius = radius;
    }

    public void setDebugObstacleAvoidance(double lookAhead, Vector2 threatObstacle) {
        this.debugLookAhead = lookAhead;
        this.debugThreatObstacle = threatObstacle;
    }

    public void setDebugPathFollowing(Vector2 futurePos, Vector2 normalPoint, Vector2 pathTarget) {
        this.debugFuturePos = futurePos;
        this.debugNormalPoint = normalPoint;
        this.debugPathTarget = pathTarget;
    }

    private void clearDebugState() {
        debugWanderCenter = null;
        debugWanderTarget = null;
        debugLookAhead = 0;
        debugThreatObstacle = null;
        debugFuturePos = null;
        debugNormalPoint = null;
        debugPathTarget = null;
        debugActiveTarget = null;
    }

    @Override
    public Vector2 calculateSteering(final World world) {
        clearDebugState();

        final Vector2 mouse = world.getMousePos();
        final Vector2 click = world.getClickPos();
        final Vector2 interactiveTarget = click != null ? click : (mouse != null ? mouse : vec2(0, 0));
        final Vector2 pointerTarget = mouse != null ? mouse : (click != null ? click : vec2(0, 0));

        return switch (mode) {
            case SEEK -> {
                debugActiveTarget = pointerTarget;
                yield SteeringBehaviors.seek(this, pointerTarget);
            }

            case FLEE -> {
                debugActiveTarget = pointerTarget;
                Vector2 fleeForce = SteeringBehaviors.flee(this, pointerTarget, FLEE_PANIC_DISTANCE);
                Vector2 wallForce = SteeringBehaviors.softWallContainment(
                    this, world.getWidth(), world.getHeight(), 55.0
                );
                // Quando fora da zona de pânico, aplica leve amortecimento para estabilizar o veículo
                if (fleeForce.isZero()) {
                    yield add(multiply(getVelocity(), -1.5), wallForce);
                }
                yield add(fleeForce, multiply(wallForce, 1.2));
            }

            case ARRIVE -> {
                debugActiveTarget = interactiveTarget;
                yield SteeringBehaviors.arrive(
                    this, interactiveTarget, ARRIVE_SLOWING_RADIUS, ARRIVE_STOP_RADIUS
                );
            }

            case WANDER -> {
                Vector2 wanderForce = SteeringBehaviors.wander(
                    this, WANDER_DISTANCE, WANDER_RADIUS, WANDER_JITTER, world.getSecs()
                );
                Vector2 wallForce = SteeringBehaviors.softWallContainment(
                    this, world.getWidth(), world.getHeight(), 50.0
                );
                yield add(wanderForce, multiply(wallForce, 0.8));
            }

            case OBSTACLE_AVOIDANCE -> {
                // Move-se em direção ao alvo (mouse/clique) desviando prioritariamente dos obstáculos
                debugActiveTarget = pointerTarget;
                Vector2 seekForce = SteeringBehaviors.seek(this, pointerTarget);
                Vector2 avoidForce = SteeringBehaviors.obstacleAvoidance(
                    this, world.getObstacles(), world.getNeighbors(),
                    AVOID_MIN_LOOKAHEAD, AVOID_MAX_LOOKAHEAD
                );
                yield add(multiply(seekForce, 0.9), multiply(avoidForce, 2.4));
            }

            case PATH_FOLLOWING -> SteeringBehaviors.pathFollowing(
                this, world.getPath(), PATH_PREDICT_TIME, PATH_LOOKAHEAD_DIST
            );

            // =================================================================
            // COMBINAÇÃO 1: Corredor Autônomo de Pista
            // Path Following (peso 1.0) + Obstacle Avoidance (peso 2.4)
            // [+ Arrive ao Pit-Stop quando há um clique ativo no mapa (peso 1.3)]
            // =================================================================
            case COMBO_RACER -> {
                Vector2 pathForce = SteeringBehaviors.pathFollowing(
                    this, world.getPath(), PATH_PREDICT_TIME, PATH_LOOKAHEAD_DIST
                );
                Vector2 avoidForce = SteeringBehaviors.obstacleAvoidance(
                    this, world.getObstacles(), world.getNeighbors(),
                    AVOID_MIN_LOOKAHEAD, AVOID_MAX_LOOKAHEAD
                );

                // Se um obstáculo está sendo evitado ativamente, reduz momentaneamente a tração
                // do Path Following para permitir que o desvio lateral ocorra com total fluidez
                double pathDynamicWeight = avoidForce.isZero() ? WEIGHT_RACER_PATH : WEIGHT_RACER_PATH * 0.45;

                Vector2 combined = add(
                    multiply(pathForce, pathDynamicWeight),
                    multiply(avoidForce, WEIGHT_RACER_AVOID)
                );

                // Se o usuário clicou em um ponto do mapa, o carro visita esse ponto (Pit-Stop)
                if (click != null && Vector2.distance(getPosition(), click) < 180.0) {
                    debugActiveTarget = click;
                    Vector2 pitStopArrive = SteeringBehaviors.arrive(
                        this, click, 140.0, ARRIVE_STOP_RADIUS
                    );
                    combined.add(multiply(pitStopArrive, WEIGHT_RACER_PITSTOP));
                }

                yield combined;
            }

            // =================================================================
            // COMBINAÇÃO 2: Explorador Evasivo
            // Wander (peso 0.8) + Obstacle Avoidance (peso 2.5) + Flee do Mouse (peso 1.8)
            // =================================================================
            case COMBO_EXPLORER_EVADER -> {
                Vector2 wanderForce = SteeringBehaviors.wander(
                    this, WANDER_DISTANCE, WANDER_RADIUS, WANDER_JITTER, world.getSecs()
                );
                Vector2 avoidForce = SteeringBehaviors.obstacleAvoidance(
                    this, world.getObstacles(), world.getNeighbors(),
                    AVOID_MIN_LOOKAHEAD, AVOID_MAX_LOOKAHEAD
                );
                Vector2 fleeForce = mouse != null
                    ? SteeringBehaviors.flee(this, mouse, FLEE_PANIC_DISTANCE)
                    : vec2();
                Vector2 wallForce = SteeringBehaviors.softWallContainment(
                    this, world.getWidth(), world.getHeight(), 55.0
                );

                if (mouse != null) {
                    debugActiveTarget = mouse;
                }

                // Se o agente está fugindo ou desviando de um obstáculo, atenua o Wander
                double wanderDynamicWeight = (avoidForce.isZero() && fleeForce.isZero())
                    ? WEIGHT_EXPLORER_WANDER
                    : WEIGHT_EXPLORER_WANDER * 0.35;

                yield add(
                    multiply(wanderForce, wanderDynamicWeight),
                    multiply(fleeForce, WEIGHT_EXPLORER_FLEE),
                    multiply(avoidForce, WEIGHT_EXPLORER_AVOID),
                    multiply(wallForce, 1.0)
                );
            }

            // =================================================================
            // COMBINAÇÃO 3: Navegador Tático com Chegada Suave
            // Arrive ao Clique/Mouse (peso 1.1) + Obstacle Avoidance (peso 2.3)
            // =================================================================
            case COMBO_TACTICAL_ARRIVE -> {
                debugActiveTarget = interactiveTarget;
                Vector2 arriveForce = SteeringBehaviors.arrive(
                    this, interactiveTarget, ARRIVE_SLOWING_RADIUS, ARRIVE_STOP_RADIUS
                );
                Vector2 avoidForce = SteeringBehaviors.obstacleAvoidance(
                    this, world.getObstacles(), world.getNeighbors(),
                    AVOID_MIN_LOOKAHEAD, AVOID_MAX_LOOKAHEAD
                );

                double arriveDynamicWeight = avoidForce.isZero()
                    ? WEIGHT_TACTICAL_ARRIVE
                    : WEIGHT_TACTICAL_ARRIVE * 0.55;

                yield add(
                    multiply(arriveForce, arriveDynamicWeight),
                    multiply(avoidForce, WEIGHT_TACTICAL_AVOID)
                );
            }
        };
    }

    @Override
    protected void drawExtraDebug(Graphics2D g, boolean debug) {
        final var oldPaint = g.getPaint();
        final var oldStroke = g.getStroke();
        final var oldFont = g.getFont();
        final var pos = getPosition();

        if (debug) {
            // 1) Caixa/sensor de detecção do Obstacle Avoidance
            if (debugLookAhead > 0) {
                final var g2 = (Graphics2D) g.create();
                g2.translate(pos.x, pos.y);
                g2.rotate(getDirection().getAngle());

                final double boxW = SteeringBehaviors.CAR_BOUNDING_RADIUS * 2.0;
                final boolean hasThreat = debugThreatObstacle != null;
                g2.setColor(hasThreat ? new Color(255, 60, 60, 45) : new Color(60, 180, 90, 28));
                g2.fill(new RoundRectangle2D.Double(0, -boxW / 2.0, debugLookAhead, boxW, 8, 8));

                g2.setColor(hasThreat ? new Color(220, 30, 30, 170) : new Color(50, 150, 75, 120));
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Double(0, -boxW / 2.0, debugLookAhead, boxW, 8, 8));
                g2.dispose();

                if (debugThreatObstacle != null) {
                    g.setColor(new Color(230, 30, 30, 190));
                    g.setStroke(new BasicStroke(2.0f));
                    g.draw(new Line2D.Double(pos.x, pos.y, debugThreatObstacle.x, debugThreatObstacle.y));
                }
            }

            // 2) Círculo projetado e ponto alvo do Wander
            if (debugWanderCenter != null && debugWanderTarget != null) {
                g.setColor(new Color(140, 60, 200, 140));
                g.setStroke(new BasicStroke(1.3f));
                g.draw(new Line2D.Double(pos.x, pos.y, debugWanderCenter.x, debugWanderCenter.y));
                g.draw(new Ellipse2D.Double(
                    debugWanderCenter.x - debugWanderRadius,
                    debugWanderCenter.y - debugWanderRadius,
                    debugWanderRadius * 2.0,
                    debugWanderRadius * 2.0
                ));
                g.setColor(new Color(210, 40, 140));
                g.fill(new Ellipse2D.Double(debugWanderTarget.x - 4, debugWanderTarget.y - 4, 8, 8));
            }

            // 3) Projeção futura, ponto normal e ponto alvo do Path Following
            if (debugFuturePos != null && debugNormalPoint != null && debugPathTarget != null) {
                g.setColor(new Color(30, 130, 210, 160));
                g.setStroke(new BasicStroke(1.2f));
                g.draw(new Line2D.Double(pos.x, pos.y, debugFuturePos.x, debugFuturePos.y));
                g.draw(new Line2D.Double(debugFuturePos.x, debugFuturePos.y, debugNormalPoint.x, debugNormalPoint.y));

                g.setColor(new Color(255, 195, 0));
                g.fill(new Ellipse2D.Double(debugFuturePos.x - 3.5, debugFuturePos.y - 3.5, 7, 7));

                g.setColor(new Color(30, 180, 80));
                g.fill(new Ellipse2D.Double(debugPathTarget.x - 4.5, debugPathTarget.y - 4.5, 9, 9));
            }

            // 4) Raios visuais de Arrive (desaceleração) e Flee (pânico)
            if (debugActiveTarget != null) {
                if (mode == BehaviorMode.ARRIVE || mode == BehaviorMode.COMBO_TACTICAL_ARRIVE) {
                    g.setColor(new Color(30, 150, 75, 95));
                    g.setStroke(new BasicStroke(
                        1.4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                        0, new float[]{6f, 6f}, 0
                    ));
                    g.draw(new Ellipse2D.Double(
                        debugActiveTarget.x - ARRIVE_SLOWING_RADIUS,
                        debugActiveTarget.y - ARRIVE_SLOWING_RADIUS,
                        ARRIVE_SLOWING_RADIUS * 2.0,
                        ARRIVE_SLOWING_RADIUS * 2.0
                    ));
                } else if (mode == BehaviorMode.FLEE || mode == BehaviorMode.COMBO_EXPLORER_EVADER) {
                    g.setColor(new Color(220, 95, 20, 95));
                    g.setStroke(new BasicStroke(
                        1.4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                        0, new float[]{6f, 6f}, 0
                    ));
                    g.draw(new Ellipse2D.Double(
                        debugActiveTarget.x - FLEE_PANIC_DISTANCE,
                        debugActiveTarget.y - FLEE_PANIC_DISTANCE,
                        FLEE_PANIC_DISTANCE * 2.0,
                        FLEE_PANIC_DISTANCE * 2.0
                    ));
                }
            }
        }

        // Etiqueta identificadora acima do veículo (com clamp para não cortar nas bordas)
        final String label = agentName + " [" + mode.getShortName() + "]";
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        final FontMetrics fm = g.getFontMetrics();
        final int textW = fm.stringWidth(label);
        final int boxW = textW + 10;
        final int badgeX = Math.max(-500, Math.min(500 - boxW, (int) pos.x - boxW / 2));
        final int badgeY = Math.max(-370, Math.min(350, (int) pos.y - 36));

        g.setColor(new Color(25, 28, 35, 185));
        g.fillRoundRect(badgeX, badgeY, boxW, 17, 6, 6);
        g.setColor(getColor().brighter());
        g.drawString(label, badgeX + 5, badgeY + 13);

        g.setPaint(oldPaint);
        g.setStroke(oldStroke);
        g.setFont(oldFont);
    }
}
