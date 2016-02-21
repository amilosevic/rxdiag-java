package amilosevic.example.rxdiag;

import rx.Observable;

import java.util.concurrent.TimeUnit;

/**
 * Created by aleksandar on 6/7/15.
 */
public class ExDelayTransform extends RxDiag.DiagTransform {
    @Override
    public Observable<DiagEv> apply(Observable<DiagEv>... inputs) {
        return inputs[0].delay(500, TimeUnit.MILLISECONDS);
    }

    @Override
    public String title() {
        return "Delay(500ms)";
    }
}
