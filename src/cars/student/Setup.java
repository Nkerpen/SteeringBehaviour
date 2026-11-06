package cars.student;

import cars.engine.Car;
import cars.student.StudentCar.BehaviorMode;

import java.awt.*;
import java.util.List;

/**
 * Configura os agentes autônomos e os cenários de demonstração do mapa de testes.
 * O usuário pode alternar entre os cenários em tempo real pelas teclas 0 a 9.
 */
public class Setup {

    /**
     * Retorne uma lista com todos os carros que serão desenhados no exercício.
     * Por padrão (Cenário 0), inicia o mapa de testes com múltiplos agentes
     * demonstrando as combinações de comportamentos simultaneamente.
     */
    public List<Car> createCars() {
        return createCarsForScenario(0);
    }

    public List<Car> createCarsForScenario(int scenario) {
        return switch (scenario) {
            // [1] Comportamento Simples: SEEK
            case 1 -> List.of(
                new StudentCar("Agente Seek", BehaviorMode.SEEK, s -> s
                    .color(new Color(25, 115, 230))
                    .position(-260, 0)
                    .orientationDegrees(0)
                    .maxSpeed(330)
                    .maxForce(380)
                )
            );

            // [2] Comportamento Simples: FLEE
            case 2 -> List.of(
                new StudentCar("Agente Flee", BehaviorMode.FLEE, s -> s
                    .color(new Color(235, 110, 25))
                    .position(0, 0)
                    .orientationDegrees(0)
                    .maxSpeed(340)
                    .maxForce(420)
                )
            );

            // [3] Comportamento Simples: ARRIVE
            case 3 -> List.of(
                new StudentCar("Agente Arrive", BehaviorMode.ARRIVE, s -> s
                    .color(new Color(30, 165, 80))
                    .position(-300, 150)
                    .orientationDegrees(-20)
                    .maxSpeed(320)
                    .maxForce(400)
                )
            );

            // [4] Comportamento Simples: WANDER
            case 4 -> List.of(
                new StudentCar("Agente Wander 1", BehaviorMode.WANDER, s -> s
                    .color(new Color(145, 55, 205))
                    .position(-180, -120)
                    .orientationDegrees(25)
                    .maxSpeed(240)
                    .maxForce(300)
                ),
                new StudentCar("Agente Wander 2", BehaviorMode.WANDER, s -> s
                    .color(new Color(200, 55, 145))
                    .position(180, 120)
                    .orientationDegrees(205)
                    .maxSpeed(240)
                    .maxForce(300)
                )
            );

            // [5] Comportamento Avançado: OBSTACLE AVOIDANCE
            case 5 -> List.of(
                new StudentCar("Agente Avoid", BehaviorMode.OBSTACLE_AVOIDANCE, s -> s
                    .color(new Color(220, 45, 55))
                    .position(-380, 0)
                    .orientationDegrees(0)
                    .maxSpeed(290)
                    .maxForce(450)
                )
            );

            // [6] Comportamento Avançado: PATH FOLLOWING
            case 6 -> List.of(
                new StudentCar("Piloto Path 1", BehaviorMode.PATH_FOLLOWING, s -> s
                    .color(new Color(235, 175, 15))
                    .position(-350, -230)
                    .orientationDegrees(0)
                    .maxSpeed(300)
                    .maxForce(380)
                ),
                new StudentCar("Piloto Path 2", BehaviorMode.PATH_FOLLOWING, s -> s
                    .color(new Color(30, 145, 210))
                    .position(340, 230)
                    .orientationDegrees(180)
                    .maxSpeed(270)
                    .maxForce(360)
                )
            );

            // [7] Combinação 1: PATH FOLLOWING (1.0) + OBSTACLE AVOIDANCE (2.4) [+ ARRIVE PIT-STOP]
            case 7 -> List.of(
                new StudentCar("Corredor 1", BehaviorMode.COMBO_RACER, s -> s
                    .color(new Color(25, 115, 230))
                    .position(-350, -230)
                    .orientationDegrees(0)
                    .maxSpeed(310)
                    .maxForce(440)
                ),
                new StudentCar("Corredor 2", BehaviorMode.COMBO_RACER, s -> s
                    .color(new Color(225, 50, 50))
                    .position(-120, -255)
                    .orientationDegrees(0)
                    .maxSpeed(265)
                    .maxForce(420)
                )
            );

            // [8] Combinação 2: WANDER (0.8) + OBSTACLE AVOIDANCE (2.5) + FLEE MOUSE (1.8)
            case 8 -> List.of(
                new StudentCar("Explorador 1", BehaviorMode.COMBO_EXPLORER_EVADER, s -> s
                    .color(new Color(145, 55, 205))
                    .position(-220, 30)
                    .orientationDegrees(15)
                    .maxSpeed(265)
                    .maxForce(440)
                ),
                new StudentCar("Explorador 2", BehaviorMode.COMBO_EXPLORER_EVADER, s -> s
                    .color(new Color(235, 120, 20))
                    .position(220, -40)
                    .orientationDegrees(165)
                    .maxSpeed(265)
                    .maxForce(440)
                )
            );

            // [9] Combinação 3: ARRIVE AO CLIQUE/MOUSE (1.1) + OBSTACLE AVOIDANCE (2.3)
            case 9 -> List.of(
                new StudentCar("Navegador Tático", BehaviorMode.COMBO_TACTICAL_ARRIVE, s -> s
                    .color(new Color(25, 165, 95))
                    .position(-350, 180)
                    .orientationDegrees(-25)
                    .maxSpeed(295)
                    .maxForce(440)
                )
            );

            // [0] Cenário Completo Padrão: Múltiplos agentes com combinações distintas
            default -> List.of(
                new StudentCar("Corredor A", BehaviorMode.COMBO_RACER, s -> s
                    .color(new Color(25, 115, 230))
                    .position(-350, -230)
                    .orientationDegrees(0)
                    .maxSpeed(305)
                    .maxForce(440)
                ),
                new StudentCar("Corredor B", BehaviorMode.COMBO_RACER, s -> s
                    .color(new Color(220, 50, 50))
                    .position(340, 230)
                    .orientationDegrees(180)
                    .maxSpeed(265)
                    .maxForce(420)
                ),
                new StudentCar("Explorador", BehaviorMode.COMBO_EXPLORER_EVADER, s -> s
                    .color(new Color(145, 55, 205))
                    .position(-50, -15)
                    .orientationDegrees(45)
                    .maxSpeed(255)
                    .maxForce(440)
                ),
                new StudentCar("Tático", BehaviorMode.COMBO_TACTICAL_ARRIVE, s -> s
                    .color(new Color(25, 165, 95))
                    .position(-250, 140)
                    .orientationDegrees(-30)
                    .maxSpeed(285)
                    .maxForce(440)
                )
            );
        };
    }

    public static String getScenarioTitle(int scenario) {
        return switch (scenario) {
            case 1 -> "Modo [1] — Comportamento Simples: SEEK (Persegue o cursor do mouse)";
            case 2 -> "Modo [2] — Comportamento Simples: FLEE (Foge do cursor do mouse dentro de 240px)";
            case 3 -> "Modo [3] — Comportamento Simples: ARRIVE (Desacelera até parar no clique ou mouse)";
            case 4 -> "Modo [4] — Comportamento Simples: WANDER (Exploração suave com círculo de Reynolds)";
            case 5 -> "Modo [5] — Comportamento Avançado: OBSTACLE AVOIDANCE (Desvio de obstáculos com sensor)";
            case 6 -> "Modo [6] — Comportamento Avançado: PATH FOLLOWING (Seguimento de pista fechada)";
            case 7 -> "Modo [7] — Combinação 1: Path Following (1.0) + Obstacle Avoidance (2.4) [+ Pit-Stop no clique]";
            case 8 -> "Modo [8] — Combinação 2: Wander (0.8) + Obstacle Avoidance (2.5) + Flee do Mouse (1.8)";
            case 9 -> "Modo [9] — Combinação 3: Arrive no Clique/Mouse (1.1) + Obstacle Avoidance (2.3)";
            default -> "Modo [0] — Mapa Completo: Todas as Combinações em Ação (Corredores + Explorador + Tático)";
        };
    }
}
