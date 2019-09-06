package com.ambekar.healthtracker;

public class Step_Item {
    private String date;
    private String steps;
    private long timestamp;

    public Step_Item(String date, String steps, long timestamp) {
        this.date = date;
        this.steps = steps;
        this.timestamp = timestamp;
    }

    public String getDate() {
        return date;
    }

    public String getSteps() {
        return steps;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object obj) {
        Step_Item temp = (Step_Item)obj;
        return this.date.equals(temp.getDate());
    }
}
