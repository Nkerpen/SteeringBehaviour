package cars.student;

import java.awt.*;
import java.util.List;

/**
 * Mapa de testes contendo o circuito fechado para Path Following e os obstáculos
 * estáticos posicionados estrategicamente para testar Obstacle Avoidance isolado
 * e combinado com Path Following, Wander, Arrive e Flee.
 */
public final class TestMap {
    private static final Path CIRCUIT_PATH = createCircuitPath();
    private static final List<Obstacle> OBSTACLES = createDefaultObstacles();

    private TestMap() {}

    private static Path createCircuitPath() {
        // Pista com raio de tolerância de 38px (largura total de 76px)
        return new Path(38.0, true)
            .addWaypoint(-350, -230)
            .addWaypoint(   0, -265)
            .addWaypoint( 350, -225)
            .addWaypoint( 410,    0)
            .addWaypoint( 340,  230)
            .addWaypoint(  30,  260)
            .addWaypoint(-340,  225)
            .addWaypoint(-410,    0);
    }

    private static List<Obstacle> createDefaultObstacles() {
        return List.of(
            // Obstáculos posicionados na borda/trajeto da pista (forçam desvio durante Path Following)
            new Obstacle( 175, -242, 22),
            new Obstacle( 382,  105, 22),
            new Obstacle(-175,  240, 22),
            new Obstacle(-395, -110, 21),

            // Obstáculos posicionados na região central da arena (testam Wander, Seek, Arrive e Flee)
            new Obstacle(-145,  -65, 30),
            new Obstacle( 150,  -55, 28),
            new Obstacle(   5,   85, 32)
        );
    }

    public static Path getCircuitPath() {
        return CIRCUIT_PATH;
    }

    public static List<Obstacle> getObstacles() {
        return OBSTACLES;
    }

    public static void draw(Graphics2D g, boolean debug, boolean showTrack, boolean showObstacles) {
        if (showTrack) {
            CIRCUIT_PATH.draw(g, debug);
        }
        if (showObstacles) {
            for (var obs : OBSTACLES) {
                obs.draw(g, debug);
            }
        }
    }
}
