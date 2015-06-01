package amilosevic.example;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.SwingScheduler;
import rx.schedulers.Timestamped;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Rx Diag Java version.
 */
public class RxDiag extends JFrame {

    // window dimensions
    public static final int WIDTH = 1280;
    public static final int HEIGHT = 620;

    // window inital position
    private static final int X = 10;
    private static final int Y = 10;

    // title
    private static final String TITLE = "RxDiag";


    /**
     * Constructs a new frame that is initially invisible.
     */
    public RxDiag() throws HeadlessException {
        super();

        setSize(WIDTH, HEIGHT);
        setLocation(X, Y);
        setResizable(false);
        setTitle(TITLE);

        setVisible(true);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        // set up animation
        DiagEv[] in = new DiagEv[] {
            new DiagEv("marble", Color.RED, 1),
            new DiagEv("marble", Color.BLUE, 3),
            new DiagEv("marble", Color.GREEN, 4),
            new DiagEv("complete", null, 8)
        };

        // construct from array
        rx.Observable<DiagEv> inObs = rx.Observable.from(in);

        // project in time
        rx.Observable<DiagEv> inObs1 = inObs.flatMap(new Func1<DiagEv, Observable<DiagEv>>() {
            @Override
            public Observable<DiagEv> call(DiagEv diagEv) {
                return rx.Observable.just(diagEv).delay(diagEv.tick * 1000, TimeUnit.MILLISECONDS);
            }
        });

        // combinator

        rx.Observable<DiagEv> outObs = inObs1.delay(500, TimeUnit.MILLISECONDS);

        // construct panel
        final Diag diag = new Diag(inObs1, outObs, "Delay(500ms)");
        add(diag);
    }

    public static void main(String[] args) {
        //
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new RxDiag();
            }
        });
    }

    Observable<Long> seconds = Observable.interval(1, TimeUnit.SECONDS).take(15);
}

class DiagEv {
    public final String shape;
    public final Color color;
    public final int tick;

    public DiagEv(String shape, Color color, int tick) {
        this.shape = shape;
        this.color = color;
        this.tick = tick;
    }

}

class Framed<T> {
    public final T value;
    public final int frame;

    public Framed(T value, int frame) {
        this.value = value;
        this.frame = frame;
    }
}

class Diag extends JPanel implements ActionListener {

    private final List<Framed<DiagEv>> inputs = Collections.synchronizedList(new ArrayList<Framed<DiagEv>>());
    private final List<Framed<DiagEv>> outputs = Collections.synchronizedList(new ArrayList<Framed<DiagEv>>());

    private final String text;

    public Diag(Observable<DiagEv> inputObs, Observable<DiagEv> outputObs, String text) {

        this.text = text;

        final long reference = System.currentTimeMillis();

        inputObs.subscribeOn(SwingScheduler.getInstance())
                .timestamp().subscribe(new Action1<Timestamped<DiagEv>>() {
            @Override
            public void call(Timestamped<DiagEv> tde) {
                // update data
                synchronized (inputs) {
                    inputs.add(new Framed<DiagEv>(tde.getValue(), (int)(tde.getTimestampMillis() - reference)/100));
                }
                repaint();

            }
        });

        outputObs.subscribeOn(SwingScheduler.getInstance())
                .timestamp().subscribe(new Action1<Timestamped<DiagEv>>() {
            @Override
            public void call(Timestamped<DiagEv> tde) {
                // update data
                synchronized (outputs) {
                    outputs.add(new Framed<DiagEv>(tde.getValue(), (int)(tde.getTimestampMillis() - reference)/100));
                }
                repaint();
            }
        });
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
        System.out.println("x");
    }

    private final BasicStroke stroke = new BasicStroke(6.f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND);

    private void doDrawing(Graphics g) {

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(stroke);

        // backgound
        eventline(g2, 100);
        combinator(g2, 310, text);
        eventline(g2, 500);

        /*

        // marbles
        marble(g2, 100, 100, Color.GREEN);
        square(g2, 400, 100, Color.BLUE);
        diamond(g2, 800, 100, Color.RED);
        pentagon(g2, 600, 100, Color.MAGENTA);
        triangle(g2, 250, 100, Color.CYAN);

        // events
        complete(g2, 900, 100);
        error(g2, 1100, 100);

        */

        synchronized (inputs) {
            for (Framed<DiagEv> ev: inputs) {
                marble(g2, 50 + ev.frame * 10, 100, ev.value.color);
            }
        }

        synchronized (outputs) {
            for (Framed<DiagEv> ev: outputs) {
                marble(g2, 50 + ev.frame * 10, 500, ev.value.color);
            }
        }

    }

    private void combinator(Graphics2D g2, int y, String text) {
        final int cheight = 130;
        final int cwidth = RxDiag.WIDTH - 2*15;
        final Rectangle2D.Float shape = new Rectangle2D.Float(15, y - cheight / 2, cwidth, cheight);

        g2.setColor(Color.WHITE);
        g2.fill(shape);
        g2.setColor(Color.BLACK);
        g2.draw(shape);

        Font prev = g2.getFont();
        g2.setFont(new Font(prev.getName(), Font.BOLD, 48));
        g2.drawString(text, 440, y + 15); // @todo: place in center

        g2.setFont(prev);

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
