package cars.student;

import cars.engine.Vector2;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static cars.engine.Vector2.*;

/**
 * Representa um caminho (circuito fechado ou aberto) composto por múltiplos segmentos
 * de reta conectados por waypoints e um raio de tolerância (largura da pista) para
 * o comportamento avançado de Path Following (Craig Reynolds).
 */
public final class Path {
    public record ClosestPointInfo(
        Vector2 normalPoint,
        Vector2 targetPoint,
        Vector2 segmentDirection,
        double distance,
        int segmentIndex
    ) {}

    private final List<Vector2> waypoints = new ArrayList<>();
    private final double radius;
    private final boolean looped;

    public Path(double radius, boolean looped) {
        this.radius = radius;
        this.looped = looped;
    }

    public Path addWaypoint(double x, double y) {
        waypoints.add(vec2(x, y));
        return this;
    }

    public List<Vector2> getWaypoints() {
        return Collections.unmodifiableList(waypoints);
    }

    public double getRadius() {
        return radius;
    }

    public boolean isLooped() {
        return looped;
    }

    /**
     * Encontra o ponto normal mais próximo na espinha dorsal do caminho para uma posição
     * futura projetada, além de calcular um ponto alvo (targetPoint) à frente ao longo do
     * caminho por uma distância lookAhead.
     */
    public ClosestPointInfo findClosestPoint(Vector2 queryPoint, double lookAheadDistance) {
        if (waypoints.size() < 2) {
            var pt = waypoints.isEmpty() ? vec2() : waypoints.get(0).clone();
            return new ClosestPointInfo(pt, pt, vec2(1, 0), 0.0, 0);
        }

        final int segCount = looped ? waypoints.size() : waypoints.size() - 1;
        double minDistSqr = Double.MAX_VALUE;
        Vector2 bestNormal = null;
        Vector2 bestDir = vec2(1, 0);
        int bestSegIndex = 0;
        double bestRemainingOnSeg = 0.0;

        for (int i = 0; i < segCount; i++) {
            final Vector2 a = waypoints.get(i);
            final Vector2 b = waypoints.get((i + 1) % waypoints.size());
            final Vector2 ab = subtract(b, a);
            final double abLenSqr = ab.sizeSqr();
            if (abLenSqr < 1e-9) continue;

            final Vector2 ap = subtract(queryPoint, a);
            double t = ap.dot(ab) / abLenSqr;
            t = Math.max(0.0, Math.min(1.0, t));

            final Vector2 normalPoint = add(a, multiply(ab, t));
            final double distSqr = distanceSqr(queryPoint, normalPoint);

            if (distSqr < minDistSqr) {
                minDistSqr = distSqr;
                bestNormal = normalPoint;
                final double segLen = Math.sqrt(abLenSqr);
                bestDir = divide(ab, segLen);
                bestSegIndex = i;
                bestRemainingOnSeg = (1.0 - t) * segLen;
            }
        }

        if (bestNormal == null) {
            bestNormal = waypoints.get(0).clone();
        }

        // Avança ao longo do caminho a partir do ponto normal pela distância lookAheadDistance,
        // fazendo a curva suavemente para o próximo segmento quando necessário.
        Vector2 targetPoint;
        if (lookAheadDistance <= bestRemainingOnSeg || (!looped && bestSegIndex == segCount - 1)) {
            targetPoint = add(bestNormal, multiply(bestDir, lookAheadDistance));
        } else {
            double leftover = lookAheadDistance - bestRemainingOnSeg;
            int nextSeg = (bestSegIndex + 1) % waypoints.size();
            Vector2 segStart = waypoints.get(nextSeg);
            Vector2 segEnd = waypoints.get((nextSeg + 1) % waypoints.size());
            Vector2 nextAb = subtract(segEnd, segStart);
            double nextLen = nextAb.size();
            while (nextLen > 1e-6 && leftover > nextLen && looped) {
                leftover -= nextLen;
                nextSeg = (nextSeg + 1) % waypoints.size();
                segStart = waypoints.get(nextSeg);
                segEnd = waypoints.get((nextSeg + 1) % waypoints.size());
                nextAb = subtract(segEnd, segStart);
                nextLen = nextAb.size();
            }
            Vector2 nextDir = nextLen > 1e-6 ? divide(nextAb, nextLen) : bestDir;
            targetPoint = add(segStart, multiply(nextDir, leftover));
        }

        return new ClosestPointInfo(
            bestNormal,
            targetPoint,
            bestDir,
            Math.sqrt(minDistSqr),
            bestSegIndex
        );
    }

    public void draw(Graphics2D g, boolean debug) {
        if (waypoints.size() < 2) return;

        final var oldPaint = g.getPaint();
        final var oldStroke = g.getStroke();

        final var pathShape = new Path2D.Double();
        final var first = waypoints.get(0);
        pathShape.moveTo(first.x, first.y);
        for (int i = 1; i < waypoints.size(); i++) {
            final var pt = waypoints.get(i);
            pathShape.lineTo(pt.x, pt.y);
        }
        if (looped) {
            pathShape.closePath();
        }

        // Borda externa da pista (zebra/acostamento)
        g.setColor(new Color(175, 178, 184));
        g.setStroke(new BasicStroke(
            (float) (radius * 2.0 + 6.0),
            BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND
        ));
        g.draw(pathShape);

        // Leito de asfalto da pista (largura = 2 * radius)
        g.setColor(new Color(196, 200, 206));
        g.setStroke(new BasicStroke(
            (float) (radius * 2.0),
            BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND
        ));
        g.draw(pathShape);

        // Linha central tracejada (espinha dorsal do Path Following)
        g.setColor(debug ? new Color(70, 90, 120, 190) : new Color(235, 235, 235, 160));
        g.setStroke(new BasicStroke(
            2.0f,
            BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_ROUND,
            0,
            new float[]{10f, 10f},
            0
        ));
        g.draw(pathShape);

        // Em modo debug, desenha os waypoints numerados e setas de sentido da pista
        if (debug) {
            final int segCount = looped ? waypoints.size() : waypoints.size() - 1;
            g.setStroke(new BasicStroke(1.5f));
            for (int i = 0; i < segCount; i++) {
                final var a = waypoints.get(i);
                final var b = waypoints.get((i + 1) % waypoints.size());
                final var mid = multiply(add(a, b), 0.5);
                final var dir = normalize(subtract(b, a));
                final var tip = add(mid, multiply(dir, 8));
                final var left = add(mid, rotate(multiply(dir, -6), Math.toRadians(35)));
                final var right = add(mid, rotate(multiply(dir, -6), Math.toRadians(-35)));
                g.setColor(new Color(60, 80, 110, 180));
                g.draw(new Line2D.Double(left.x, left.y, tip.x, tip.y));
                g.draw(new Line2D.Double(right.x, right.y, tip.x, tip.y));
            }

            g.setFont(new Font("SansSerif", Font.BOLD, 10));
            for (int i = 0; i < waypoints.size(); i++) {
                final var wp = waypoints.get(i);
                g.setColor(new Color(45, 105, 180));
                g.fill(new Ellipse2D.Double(wp.x - 4.5, wp.y - 4.5, 9, 9));
                g.setColor(Color.WHITE);
                g.draw(new Ellipse2D.Double(wp.x - 4.5, wp.y - 4.5, 9, 9));
                g.setColor(new Color(30, 50, 85));
                g.drawString("W" + (i + 1), (float) wp.x + 6, (float) wp.y - 5);
            }
        }

        g.setPaint(oldPaint);
        g.setStroke(oldStroke);
    }
}
