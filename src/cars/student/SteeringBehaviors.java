package cars.student;

import cars.engine.Car;
import cars.engine.Vector2;

import java.util.List;
import java.util.random.RandomGenerator;

import static cars.engine.Vector2.*;

/**
 * Implementação modular dos Steering Behaviors de Craig Reynolds:
 * - Simples: Seek, Flee, Arrive e Wander
 * - Avançados: Obstacle Avoidance e Path Following
 * - Auxiliar: Wall Avoidance (contenção suave nas bordas da janela)
 */
public final class SteeringBehaviors {
    private static final RandomGenerator RND = RandomGenerator.getDefault();

    /** Raio envolvente aproximado do carro F1 para margem de segurança de colisão */
    public static final double CAR_BOUNDING_RADIUS = 24.0;

    private SteeringBehaviors() {}

    // =========================================================================
    // 1. SEEK (Comportamento Simples)
    // =========================================================================
    /**
     * Calcula a força de Seek: direciona o agente em velocidade máxima até o alvo.
     * Fórmula:
     *   desiredVelocity = normalize(target - position) * maxSpeed
     *   steering = desiredVelocity - velocity
     */
    public static Vector2 seek(Car agent, Vector2 target) {
        if (target == null) return vec2();

        final Vector2 toTarget = subtract(target, agent.getPosition());
        if (toTarget.isZero()) return vec2();

        final Vector2 desiredVelocity = normalize(toTarget).multiply(agent.getMaxSpeed());
        return subtract(desiredVelocity, agent.getVelocity());
    }

    // =========================================================================
    // 2. FLEE (Comportamento Simples)
    // =========================================================================
    /**
     * Calcula a força de Flee: afasta o agente do alvo em velocidade máxima
     * quando o alvo está dentro do raio de pânico (panicDistance).
     * Se panicDistance <= 0, foge independentemente da distância.
     */
    public static Vector2 flee(Car agent, Vector2 threat, double panicDistance) {
        if (threat == null) return vec2();

        final Vector2 fromThreat = subtract(agent.getPosition(), threat);
        final double dist = fromThreat.size();

        if (panicDistance > 0 && dist > panicDistance) {
            return vec2();
        }

        if (fromThreat.isZero()) {
            // Caso esteja exatamente sobre o ponto de ameaça, foge na direção atual
            return multiply(agent.getDirection(), agent.getMaxForce());
        }

        final Vector2 desiredVelocity = normalize(fromThreat).multiply(agent.getMaxSpeed());
        // Escala levemente pela proximidade para resposta imediata ao entrar na zona de pânico
        final double urgency = panicDistance > 0
            ? 1.0 + 0.6 * (1.0 - Math.min(1.0, dist / panicDistance))
            : 1.0;
        return multiply(subtract(desiredVelocity, agent.getVelocity()), urgency);
    }

    // =========================================================================
    // 3. ARRIVE (Comportamento Simples)
    // =========================================================================
    /**
     * Calcula a força de Arrive: move o agente em direção ao alvo em velocidade
     * máxima fora do slowingRadius e reduz gradualmente a velocidade desejada
     * de forma linear dentro do slowingRadius até parar suavemente no stopRadius.
     */
    public static Vector2 arrive(Car agent, Vector2 target, double slowingRadius, double stopRadius) {
        if (target == null) return vec2();

        final Vector2 toTarget = subtract(target, agent.getPosition());
        final double distance = toTarget.size();

        // Dentro do raio de parada: aplica frenagem amortecida oposta à velocidade atual
        if (distance <= stopRadius) {
            if (agent.getSpeed() < 3.0) {
                return multiply(agent.getVelocity(), -15.0);
            }
            return multiply(agent.getVelocity(), -6.0);
        }

        // Calcula a velocidade escalonada pela distância ao alvo
        final double targetSpeed;
        if (distance < slowingRadius) {
            targetSpeed = agent.getMaxSpeed() * (distance / slowingRadius);
        } else {
            targetSpeed = agent.getMaxSpeed();
        }

        final Vector2 desiredVelocity = normalize(toTarget).multiply(targetSpeed);
        final Vector2 steering = subtract(desiredVelocity, agent.getVelocity());

        // Dentro do raio de desaceleração, amplifica o ganho do amortecimento (damping)
        // para respeitar a distância física de frenagem (v^2 / 2*a_max) sem overshoot.
        if (distance < slowingRadius) {
            return multiply(steering, 2.2);
        }
        return steering;
    }

    // =========================================================================
    // 4. WANDER (Comportamento Simples)
    // =========================================================================
    /**
     * Calcula a força de Wander segundo o modelo de projeção circular de Craig Reynolds:
     * projeta um círculo à frente do veículo (wanderDistance) com raio wanderRadius,
     * adiciona um pequeno deslocamento angular aleatório (wanderJitter * dt) ao alvo
     * na circunferência e realiza Seek até esse ponto projetado.
     */
    public static Vector2 wander(StudentCar agent,
                                 double wanderDistance,
                                 double wanderRadius,
                                 double wanderJitter,
                                 double dt) {
        // Atualiza o ângulo de Wander com ruído suave [-1, +1] * jitter * dt
        final double deltaAngle = (RND.nextDouble() * 2.0 - 1.0) * wanderJitter * Math.max(dt, 0.016);
        final double newWanderAngle = agent.getWanderAngle() + deltaAngle;
        agent.setWanderAngle(newWanderAngle);

        // Centro do círculo projetado à frente do veículo
        final Vector2 heading = agent.getDirection();
        final Vector2 circleCenter = add(agent.getPosition(), multiply(heading, wanderDistance));

        // Ponto alvo na circunferência do círculo de Wander
        final double headingAngle = heading.getAngle();
        final Vector2 displacement = byAngleSize(headingAngle + newWanderAngle, wanderRadius);
        final Vector2 wanderTarget = add(circleCenter, displacement);

        // Armazena para visualização no modo Debug (tecla D)
        agent.setDebugWander(circleCenter, wanderTarget, wanderRadius);

        return seek(agent, wanderTarget);
    }

    // =========================================================================
    // 5. OBSTACLE AVOIDANCE (Comportamento Avançado)
    // =========================================================================
    /**
     * Calcula a força de Obstacle Avoidance utilizando caixa de detecção frontal
     * dinâmica proporcional à velocidade atual (modelo de Reynolds / Buckland).
     * Avalia tanto obstáculos estáticos do mapa quanto carros vizinhos,
     * transformando suas posições para o espaço local do veículo (heading / side)
     * e gerando força lateral + força de frenagem para o obstáculo mais ameaçador.
     */
    public static Vector2 obstacleAvoidance(StudentCar agent,
                                            List<Obstacle> staticObstacles,
                                            List<Car> neighborCars,
                                            double minLookAhead,
                                            double maxLookAhead) {
        final double speedRatio = agent.getMaxSpeed() > 0
            ? agent.getSpeed() / agent.getMaxSpeed()
            : 0.0;
        final double lookAhead = minLookAhead + speedRatio * (maxLookAhead - minLookAhead);

        final Vector2 pos = agent.getPosition();
        final Vector2 heading = agent.getDirection();
        final Vector2 side = perp(heading); // Vetor perpendicular (-y, x)

        double closestIntersectionX = Double.MAX_VALUE;
        Vector2 closestObsPos = null;
        double closestLocalX = 0.0;
        double closestLocalY = 0.0;
        double closestExpandedRadius = 0.0;

        // 1) Verifica obstáculos circulares estáticos do mapa
        if (staticObstacles != null) {
            for (var obs : staticObstacles) {
                final Vector2 toObs = subtract(obs.getPosition(), pos);
                final double localX = toObs.dot(heading);
                final double localY = toObs.dot(side);
                final double expandedRadius = obs.getRadius() + CAR_BOUNDING_RADIUS;

                // Descarta se estiver atrás ou além do alcance do sensor frontal
                if (localX < -obs.getRadius() * 0.4 || localX > lookAhead + obs.getRadius()) {
                    continue;
                }

                // Verifica interseção com o corredor lateral da caixa de detecção
                if (Math.abs(localY) < expandedRadius) {
                    final double sqrtPart = Math.sqrt(
                        Math.max(0.0, expandedRadius * expandedRadius - localY * localY)
                    );
                    double intersectionX = localX - sqrtPart;
                    if (intersectionX <= 0) {
                        intersectionX = Math.max(0.1, localX + sqrtPart);
                    }

                    if (intersectionX < closestIntersectionX) {
                        closestIntersectionX = intersectionX;
                        closestObsPos = obs.getPosition();
                        closestLocalX = localX;
                        closestLocalY = localY;
                        closestExpandedRadius = expandedRadius;
                    }
                }
            }
        }

        // 2) Verifica carros vizinhos como obstáculos dinâmicos
        if (neighborCars != null) {
            for (var other : neighborCars) {
                final Vector2 otherPos = other.getPosition();
                final Vector2 toOther = subtract(otherPos, pos);
                final double localX = toOther.dot(heading);
                final double localY = toOther.dot(side);
                final double expandedRadius = CAR_BOUNDING_RADIUS * 1.85;

                if (localX < -10.0 || localX > lookAhead * 0.85) {
                    continue;
                }

                if (Math.abs(localY) < expandedRadius) {
                    final double sqrtPart = Math.sqrt(
                        Math.max(0.0, expandedRadius * expandedRadius - localY * localY)
                    );
                    double intersectionX = localX - sqrtPart;
                    if (intersectionX <= 0) {
                        intersectionX = Math.max(0.1, localX + sqrtPart);
                    }

                    if (intersectionX < closestIntersectionX) {
                        closestIntersectionX = intersectionX;
                        closestObsPos = otherPos;
                        closestLocalX = localX;
                        closestLocalY = localY;
                        closestExpandedRadius = expandedRadius;
                    }
                }
            }
        }

        // Atualiza dados de debug visual do sensor
        agent.setDebugObstacleAvoidance(lookAhead, closestObsPos);

        if (closestObsPos == null) {
            return vec2();
        }

        // Calcula a intensidade com base na proximidade longitudinal (quanto mais perto, mais forte)
        final double proximityMultiplier = 1.0 + 2.2 * Math.max(
            0.0, (lookAhead - Math.max(0.0, closestLocalX)) / lookAhead
        );

        // Força lateral: empurra na direção perpendicular oposta ao centro do obstáculo
        final double lateralDir = (closestLocalY >= 0.0) ? -1.0 : 1.0;
        final double penetration = closestExpandedRadius - Math.abs(closestLocalY);
        final double lateralMagnitude = penetration * proximityMultiplier * 16.0;
        final Vector2 lateralForce = multiply(side, lateralDir * lateralMagnitude);

        // Força de frenagem longitudinal suave para facilitar a curva de desvio
        final double brakingWeight = 1.8;
        final double brakingMagnitude = (closestExpandedRadius - Math.abs(closestLocalY)) * brakingWeight;
        final Vector2 brakingForce = multiply(heading, -brakingMagnitude);

        Vector2 totalAvoidance = add(lateralForce, brakingForce);

        // Segurança extra: repulsão radial direta se o veículo estiver muito próximo da borda física
        final Vector2 awayFromCenter = subtract(pos, closestObsPos);
        final double centerDist = awayFromCenter.size();
        if (centerDist > 1e-5 && centerDist < closestExpandedRadius) {
            final double overlap = closestExpandedRadius - centerDist;
            totalAvoidance.add(multiply(normalize(awayFromCenter), overlap * 28.0));
        }

        return totalAvoidance;
    }

    // =========================================================================
    // 6. PATH FOLLOWING (Comportamento Avançado)
    // =========================================================================
    /**
     * Calcula a força de Path Following segundo o modelo de Craig Reynolds:
     * 1) Projeta a posição futura do veículo (futurePos = pos + vel * predictTime).
     * 2) Encontra a projeção normal mais próxima nos segmentos do Path.
     * 3) Determina um ponto alvo (targetPoint) mais à frente ao longo do segmento.
     * 4) Se a distância até a espinha dorsal exceder o raio da pista (ou se o carro
     *    precisar manter alinhamento/velocidade na pista), aplica Seek ao ponto alvo.
     */
    public static Vector2 pathFollowing(StudentCar agent,
                                        Path path,
                                        double predictTime,
                                        double lookAheadDistance) {
        if (path == null) return vec2();

        final Vector2 velocity = agent.getVelocity();
        final Vector2 futureOffset = velocity.size() > 25.0
            ? multiply(velocity, predictTime)
            : multiply(agent.getDirection(), 35.0);
        final Vector2 futurePos = add(agent.getPosition(), futureOffset);

        final Path.ClosestPointInfo info = path.findClosestPoint(futurePos, lookAheadDistance);

        // Armazena pontos no agente para renderização no modo Debug (tecla D)
        agent.setDebugPathFollowing(futurePos, info.normalPoint(), info.targetPoint());

        // Fora do raio da pista: aplica Seek completo para reconduzir o carro ao traçado
        if (info.distance() > path.getRadius()) {
            return seek(agent, info.targetPoint());
        }

        // Dentro do raio da pista: aplica Seek suave ao alvo à frente para manter
        // o carro progredindo no sentido correto da pista e contornando as curvas
        return multiply(seek(agent, info.targetPoint()), 0.65);
    }

    // =========================================================================
    // 7. WALL AVOIDANCE AUXILIAR (Contenção suave nas bordas da janela)
    // =========================================================================
    /**
     * Força suave de contenção para evitar que agentes em Flee ou Wander saiam
     * continuamente das bordas da tela durante os testes.
     */
    public static Vector2 softWallContainment(Car agent, double width, double height, double margin) {
        final double halfW = width / 2.0 - margin;
        final double halfH = height / 2.0 - margin;
        final Vector2 pos = agent.getPosition();
        final Vector2 force = vec2();

        if (pos.x < -halfW) {
            force.x = (-halfW - pos.x) * 8.0;
        } else if (pos.x > halfW) {
            force.x = (halfW - pos.x) * 8.0;
        }

        if (pos.y < -halfH) {
            force.y = (-halfH - pos.y) * 8.0;
        } else if (pos.y > halfH) {
            force.y = (halfH - pos.y) * 8.0;
        }

        return force;
    }
}
