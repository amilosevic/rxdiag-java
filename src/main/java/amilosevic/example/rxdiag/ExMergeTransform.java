package amilosevic.example.rxdiag;

import rx.Observable;

/**
 * Created by aleksandar on 6/7/15.
 */
public class ExMergeTransform extends RxDiag.DiagTransform {

    @Override
    public Observable<DiagEv> apply(Observable<DiagEv>... inputs) {
        return Observable.<DiagEv>merge(inputs);
    }

    @Override
    public String title() {
        return "Merge";
    }
}
