package com.adamcalculator.dynamicpack.pack;

import java.util.function.LongConsumer;

public class FileDownloadConsumer implements LongConsumer {
    private long max = 0;
    private long latest = 0;

    @Override
    public void accept(long value) {
        this.latest = value;
        if (value > max) {
            max = value;
        }
        onUpdate(this);
    }

    public float getPercentage() {
        if (max <= latest) {
            return -1;
        }

        return (float) max * 100f / (float) latest;
    }

    public void onUpdate(FileDownloadConsumer it) {
        // override it
    }
}