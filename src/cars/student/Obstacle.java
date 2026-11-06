package cars.student;

import cars.engine.Vector2;

import java.awt.*;
import java.awt.geom.Ellipse2D;

/**
 * Representa um obstáculo circular fixo no mapa de testes para o comportamento
 * de Obstacle Avoidance.
 */
public final class Obstacle {
    private final Vector2 position;
    private final double radius;
    private final String label;

    public Obstacle(Vector2 position, double radius, String label) {
        this.position = position.clone();
        this.radius = radius;
        this.label = label;
    }

    public Obstacle(double x, double y, double radius) {
        this(new Vector2(x, y), radius, "");
    }

    public Vector2 getPosition() {
        return position.clone();
    }

    public double getRadius() {
        return radius;
    }

    public String getLabel() {
        return label;
    }

    public void draw(Graphics2D g, boolean debug) {
        final var oldPaint = g.getPaint();
        final var oldStroke = g.getStroke();

        // Sombra suave
        g.setColor(new Color(0, 0, 0, 45));
        g.fill(new Ellipse2D.Double(
            position.x - radius + 3,
            position.y - radius + 3,
            radius * 2,
            radius * 2
        ));

        // Corpo externo (estilo barreira de pneus / zebra vermelha e branca)
        g.setColor(new Color(200, 45, 45));
        g.fill(new Ellipse2D.Double(
            position.x - radius,
            position.y - radius,
            radius * 2,
            radius * 2
        ));

        // Anel intermediário branco
        final double inner1 = radius * 0.72;
        g.setColor(new Color(245, 245, 245));
        g.fill(new Ellipse2D.Double(
            position.x - inner1,
            position.y - inner1,
            inner1 * 2,
            inner1 * 2
        ));

        // Miolo escuro (pneu/concreto)
        final double inner2 = radius * 0.48;
        g.setColor(new Color(45, 48, 55));
        g.fill(new Ellipse2D.Double(
            position.x - inner2,
            position.y - inner2,
            inner2 * 2,
            inner2 * 2
        ));

        // Borda
        g.setColor(new Color(120, 20, 20));
        g.setStroke(new BasicStroke(2f));
        g.draw(new Ellipse2D.Double(
            position.x - radius,
            position.y - radius,
            radius * 2,
            radius * 2
        ));

        // Margem de segurança expandida no modo debug
        if (debug) {
            final double expanded = radius + SteeringBehaviors.CAR_BOUNDING_RADIUS;
            g.setColor(new Color(220, 50, 50, 75));
            g.setStroke(new BasicStroke(
                1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                0, new float[]{4f, 4f}, 0
            ));
            g.draw(new Ellipse2D.Double(
                position.x - expanded,
                position.y - expanded,
                expanded * 2,
                expanded * 2
            ));
        }

        g.setPaint(oldPaint);
        g.setStroke(oldStroke);
    }
}
