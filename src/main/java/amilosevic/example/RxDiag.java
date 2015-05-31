package amilosevic.example;

import javafx.scene.transform.Transform;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.*;
import java.util.Collection;
import java.util.Random;

/**
 * Rx Diag Java version.
 */
public class RxDiag extends JFrame {

    // window dimensions
    public static final int WIDTH = 1250;
    public static final int HEIGHT = 620;

    // window inital position
    private static final int X = 10;
    private static final int Y = 10;

    // title
    private static final String TITLE = "RxDiag";


    /**
     * Constructs a new frame that is initially invisible.
     * <p/>
     * This constructor sets the component's locale property to the value
     * returned by <code>JComponent.getDefaultLocale</code>.
     *
     * @throws java.awt.HeadlessException if GraphicsEnvironment.isHeadless()
     *                                    returns true.
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see java.awt.Component#setSize
     * @see java.awt.Component#setVisible
     * @see javax.swing.JComponent#getDefaultLocale
     */
    public RxDiag() throws HeadlessException {
        super();
        
        final Diag diag = new Diag();
        add(diag);

        setSize(WIDTH, HEIGHT);
        setLocation(X, Y);
        setResizable(false);
        setTitle(TITLE);

        setVisible(true);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    public static void main(String[] args) {
        //
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new RxDiag();
            }
        });

    }
}

class Diag extends JPanel implements ActionListener {

    private final int DELAY = 150;
    private final Timer timer;

    private final BasicStroke stroke = new BasicStroke(6.f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

    public Diag() {
        timer = new Timer(DELAY, this);
        timer.start();
    }

    /**
     * Invoked when an action occurs.
     *
     * @param e
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        doDrawing(g);
    }

    private void doDrawing(Graphics g) {

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(stroke);

        // backgound
        eventline(g2, 100);
        combinatorbox(g2, 310);
        eventline(g2, 500);

        // marbles
        marble(g2, 100, 100, Color.GREEN);
        square(g2, 400, 100, Color.BLUE);
        diamond(g2, 800, 100, Color.RED);
        pentagon(g2, 600, 100, Color.MAGENTA);
        triangle(g2, 250, 100, Color.CYAN);

        complete(g2, 900, 100);
        error(g2, 1100, 100);


    }

    private void combinatorbox(Graphics2D g2, int y) {
        final int cheight = 130;
        final int cwidth = RxDiag.WIDTH - 2*15;
        final Rectangle2D.Float shape = new Rectangle2D.Float(15, y - cheight / 2, cwidth, cheight);

        g2.setColor(Color.WHITE);
        g2.fill(shape);
        g2.setColor(Color.BLACK);
        g2.draw(shape);


    }

    private void eventline(Graphics2D g2, int y) {
        g2.drawLine(15, y, RxDiag.WIDTH - 15, y);
    }

    private Shape marble(Graphics2D g2, int x, int y, Color color){
        final int radius = 45;
        final Ellipse2D.Float shape = new Ellipse2D.Float(x - radius, y - radius, 2 * radius, 2 * radius);

        g2.setColor(color);
        g2.fill(shape);
        g2.setColor(Color.BLACK);
        g2.draw(shape);

        return shape;

    }

    private Shape square(Graphics2D g2, int x, int y, Color color) {
        final int d = 90;
        final Rectangle2D.Float shape = new Rectangle2D.Float(x - d/2, y - d/2, d, d);

        g2.setColor(color);
        g2.fill(shape);
        g2.setColor(Color.BLACK);
        g2.draw(shape);

        return shape;
    }

    private Shape diamond(Graphics2D g2, int x, int y, Color color) {
        final int d = 60;

        final AffineTransform at = new AffineTransform();
        at.rotate(Math.PI / 4, x, y);

        final AffineTransform prev = g2.getTransform();
        g2.setTransform(at);

        final Shape shape = new Rectangle2D.Float(x - d/2, y - d/2, d, d);

        g2.setColor(color);
        g2.fill(shape);
        g2.setColor(Color.BLACK);
        g2.draw(shape);

        g2.setTransform(prev);

        return shape;
    }

    private Shape pentagon(Graphics2D g2, int x, int y, Color color) {

        final int dm = 90, r = dm/2;

        final float a = Math.round(r * 0.8);
        final float b = Math.round(r * 0.59);
        final float c = Math.round(r * 0.2);
        final float d = Math.round(r * 0.98);


        GeneralPath path = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 5);

        path.moveTo(x, y - r);
        path.lineTo(x - d, y - c);
        path.lineTo(x - b, y + a);
        path.lineTo(x + b, y + a);
        path.lineTo(x + d, y - c);

        path.closePath();

        g2.setColor(color);
        g2.fill(path);
        g2.setColor(Color.BLACK);
        g2.draw(path);

        return path;
    }

    private Shape triangle(Graphics2D g2, int x, int y, Color color) {

        final int dm = 90, r = dm/2;

        final float s = r * 2 * 0.86f;

        final float a = Math.round(s * 0.86 - r);
        final float b = Math.round(s * 0.5);

        GeneralPath path = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 3);

        path.moveTo(x + b, y - a);
        path.lineTo(x, y + r);
        path.lineTo(x - b, y - a);
        path.closePath();

        g2.setColor(color);
        g2.fill(path);
        g2.setColor(Color.BLACK);
        g2.draw(path);

        return path;
    }

    private Shape complete(Graphics2D g2, int x, int y) {
        final int v = 40;

        final Shape shape = new Line2D.Float(x, y - v, x, y + v);

        g2.draw(shape);

        return shape;

    }

    private Shape error(Graphics2D g2, int x, int y) {
        final int v = 40;

        // todo: combine lines into shape

        final Shape line1 = new Line2D.Float(x - v, y - v, x + v, y + v);
        final Shape line2 = new Line2D.Float(x + v, y - v, x - v, y + v);


        g2.draw(line1);
        g2.draw(line2);

        return line1;

    }
}
