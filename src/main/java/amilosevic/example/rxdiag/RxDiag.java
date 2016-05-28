package amilosevic.example.rxdiag;

import rx.Observable;
import rx.functions.Action0;
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
import java.util.Arrays;
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

    public static final int EV_LINE_HEIGHT = 120;

    // window inital position
    private static final int X = 60;
    private static final int Y = 60;

    // title
    private static final String TITLE = "RxDiag";


    /**
     * Constructs a new frame that is initially invisible.
     */
    public RxDiag(DiagTransform transform, DiagEv[] ... ins) throws HeadlessException {
        super();

        setSize(WIDTH, HEIGHT + (ins.length - 1) * EV_LINE_HEIGHT);
        setLocation(X, Y);
        setResizable(false);
        setTitle(TITLE + " // " + transform.title());

        setVisible(true);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        // inputs
        List<Observable<DiagEv>> inObs = new ArrayList<Observable<DiagEv>>();

        for (DiagEv[] in: ins) {
            Observable<DiagEv> in1 = project(in);
            inObs.add(in1);
        }

        // apply combinator
        Observable<DiagEv> outObs = transform.apply(inObs.toArray(new Observable[ins.length]));

        // construct panel
        final Diag diag = new Diag(transform.title(), outObs, inObs);

        add(diag);
    }


    private Observable<DiagEv> project(DiagEv[] in) {
        // create observable from array
        Observable<DiagEv> inObs = Observable.from(in);

        // project in time
        return inObs.flatMap(new Func1<DiagEv, Observable<DiagEv>>() {
            @Override
            public Observable<DiagEv> call(DiagEv diagEv) {
                if (diagEv.shape.equals(DiagShape.COMPLETE)) {
                    return Observable.<DiagEv>empty()
                            .delay(diagEv.tick * 1000, TimeUnit.MILLISECONDS);
                } else if (diagEv.shape.equals(DiagShape.ERROR)) {
                    return Observable.concat(
                            Observable.<DiagEv>empty().delay(diagEv.tick * 1000, TimeUnit.MILLISECONDS),
                            Observable.<DiagEv>error(new Error("Throw!"))
                    );
                } else {
                    return Observable.just(diagEv)
                            .delay(diagEv.tick * 1000, TimeUnit.MILLISECONDS);
                }
            }
        });
    }

    public static abstract class DiagTransform {

        public abstract Observable<DiagEv> apply(Observable<DiagEv> ... inputs);

        public abstract String title();
    }

    public static void main(String[] args) {

        // @todo: externalize inputs definition, maybe move to DiagTransform
        final DiagEv[] in0 = new DiagEv[] {
                new DiagEv("marble", Color.RED, 2),
                new DiagEv("marble", Color.BLUE, 7),
                new DiagEv("marble", Color.GREEN, 4),
                new DiagEv("error", null, 8)
        };

        final DiagEv[] in1 = new DiagEv[] {
//                new DiagEv("diamond", Color.RED, 2),
                new DiagEv("diamond", Color.BLUE, 5),
                new DiagEv("diamond", Color.GREEN, 6),
                new DiagEv("complete", null, 9)
        };

        final DiagEv[] in2 = new DiagEv[] {
                new DiagEv("square", Color.CYAN, 1),
                new DiagEv("square", Color.YELLOW, 3),
                new DiagEv("square", Color.GRAY, 9),
                new DiagEv("complete", null, 10)
        };


        // @todo: load transformation by class name, passed by args
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new RxDiag(new ExMergeTransform(), in0, in1, in2);
            }
        });
    }
}

enum DiagShape {
    MARBLE,
    SQUARE,
    DIAMOND,
    TRIANGLE,
    PENTAGON,
    COMPLETE,
    ERROR;
}


class DiagEv {
    public final DiagShape shape;
    public final Color color;
    public final int tick;

    public DiagEv(String shape, Color color, int tick) {
        this.shape = DiagShape.valueOf(shape.toUpperCase());
        this.color = (color == null ? Color.GREEN : color);
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

    private final List<List<Framed<DiagEv>>> inputs = new ArrayList<List<Framed<DiagEv>>>();
    private final List<Framed<DiagEv>> outputs;

    private final String text;

    public Diag(final String text, Observable<DiagEv> outputObs, Observable<DiagEv> inObs) {
        this(text, outputObs, Arrays.asList(inObs));
    }

    public Diag(final String text, Observable<DiagEv> outputObs, List<Observable<DiagEv>> inputObs) {

        this.text = text;

        final long reference = System.currentTimeMillis();

        for (Observable<DiagEv> inObs: inputObs) {
            final List<Framed<DiagEv>> in = Collections.synchronizedList(new ArrayList<Framed<DiagEv>>());
            inputs.add(in);

            inObs.subscribeOn(SwingScheduler.getInstance())
                    .timestamp().subscribe(
                    new Action1<Timestamped<DiagEv>>() {
                        @Override
                        public void call(Timestamped<DiagEv> tde) {
                            // update data
                            synchronized (in) {
                                final int frame = frame(tde.getTimestampMillis(), reference);
                                in.add(new Framed<DiagEv>(tde.getValue(), frame));
                            }
                            repaint();

                        }

                    },
                    new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            System.out.println("in; error");
                            synchronized (in) {
                                final int frame = frame(System.currentTimeMillis(), reference);
                                in.add(new Framed<DiagEv>(new DiagEv("error", null, 0), frame));
                            }
                            repaint();
                        }
                    },
                    new Action0() {
                        @Override
                        public void call() {
                            System.out.println("in; complete");
                            synchronized (in) {
                                final int frame = frame(System.currentTimeMillis(), reference);
                                in.add(new Framed<DiagEv>(new DiagEv("complete", null, 0), frame));
                            }
                            repaint();
                        }

                    }

            );
        }

        outputs = Collections.synchronizedList(new ArrayList<Framed<DiagEv>>());

        outputObs/*.subscribeOn(SwingScheduler.getInstance())*/
                .timestamp().subscribe(
                new Action1<Timestamped<DiagEv>>() {
                    @Override
                    public void call(Timestamped<DiagEv> tde) {
                        // update data
                        synchronized (outputs) {
                            outputs.add(new Framed<DiagEv>(tde.getValue(), (int) (tde.getTimestampMillis() - reference) / 100));
                        }
                        repaint();
                    }
                },
                new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        System.out.println("out; error");
                        synchronized (outputs) {
                            final int frame = frame(System.currentTimeMillis(), reference);
                            outputs.add(new Framed<DiagEv>(new DiagEv("error", null, 0), frame));
                        }
                        repaint();
                    }
                },
                new Action0() {
                    @Override
                    public void call() {
                        System.out.println("out; complete");
                        synchronized (outputs) {
                            final int frame = frame(System.currentTimeMillis(), reference);
                            outputs.add(new Framed<DiagEv>(new DiagEv("complete", null, 0), frame));
                        }
                        repaint();
                    }

                }
        );

        Timer timer = new Timer(25, this);
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

    private final BasicStroke stroke = new BasicStroke(6.f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND);
    private final BasicStroke marker = new BasicStroke(2.f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND);
    private final BasicStroke dashed = new BasicStroke(2.f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[] {4.0f, 14.0f}, 0.0f);

    private void doDrawing(Graphics g) {

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(stroke);

        // backgound
        final int yin = 100; // in eventline y coordinate
        final int yc = 300;
        final int yout = 500; // out eventlne y coordinate

        // delta height for combinator & output line
        final int delta = (inputs.size() - 1) * RxDiag.EV_LINE_HEIGHT;


        // draw inputs
        int i = 0;
        for (final List<Framed<DiagEv>> in: inputs) {
            final int d = i++ * RxDiag.EV_LINE_HEIGHT;
            final int y = yin + d;

            final int ey1 = y + 50;
            final int ey2 = yc + delta - 70;

            eventline(g2, y);

            synchronized (in) {
                for (Framed<DiagEv> ev: in) {
                    int x = 50 + ev.frame * 10;
                    drawEv(g2, x, y, ev.value.shape, ev.value.color);
                    drawArrow(g2, x, ey1, x, ey2, Color.BLACK);

                }
            }
        }

        // draw combinator box
        combinator(g2, yc + delta, text);

        // draw outputs
        eventline(g2, yout + delta);

        // draw events
        final int ey1 = yc + delta + 75;
        final int ey2 = yout + delta - 50;
        synchronized (outputs) {
            for (Framed<DiagEv> ev: outputs) {
                int x = 50 + ev.frame * 10;
                drawEv(g2, x,  yout + delta, ev.value.shape, ev.value.color);
                drawArrow(g2, x, ey1, x, ey2, Color.BLACK);
            }
        }


    }



    private void drawEv(Graphics2D g2, int x, int y, DiagShape shape, Color color) {
        switch (shape) {
            case COMPLETE: complete(g2, x, y); break;
            case ERROR: error(g2, x, y); break;
            case MARBLE: marble(g2, x, y, color); break;
            case SQUARE: square(g2, x, y, color); break;
            case DIAMOND: diamond(g2, x, y, color); break;
            case PENTAGON: pentagon(g2, x, y, color); break;
            case TRIANGLE: triangle(g2, x, y, color); break;
            default: throw new IllegalArgumentException("Unhandled shape");
        }

    }



    private Shape drawArrow(Graphics2D g2, int x1, int y1, int x2, int y2, Color color) {

        final Shape shape = new Line2D.Float(x1, y1, x2, y2);

        final Stroke prevStroke = g2.getStroke();
        final AffineTransform prevTransform = g2.getTransform();

        g2.setColor(color);
        g2.setStroke(dashed);
        g2.draw(shape);


        int dx = x2 - x1;
        int dy = y2 - y1;


        //double theta = -Math.PI / 4;
        double theta = Math.atan2(dy, dx) - Math.PI / 2;

        final AffineTransform at = new AffineTransform();
        at.setToRotation(theta, x2, y2);

        GeneralPath path = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 3);

        path.moveTo(x2, y2);
        path.lineTo(x2 - 8, y2 - 20);
        path.lineTo(x2 + 8, y2 - 20);
        path.closePath();

        g2.setTransform(at);
        g2.setStroke(marker);
        g2.setColor(color);
        g2.fill(path);
        g2.draw(path);
        g2.setColor(Color.BLACK);


        g2.setStroke(prevStroke);
        g2.setTransform(prevTransform);
        return shape;

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

    private int frame(final long timestampMillis, final long reference) {
        return (int) (timestampMillis - reference) / 100;
    }
}
