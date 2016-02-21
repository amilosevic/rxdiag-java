package amilosevic.example.rxdiag;

import rx.Observable;

import java.util.concurrent.TimeUnit;

/**
 * Created by aleksandar on 6/7/15.
 */
public class ExJustTransform extends RxDiag.DiagTransform {
    @Override
    public Observable<DiagEv> apply(Observable<DiagEv>... inputs) {
        return Observable.just(new DiagEv("marble", null, 0)).delay(4000, TimeUnit.MILLISECONDS);
    }

    @Override
    public String title() {
        return "Just";
    }
}
