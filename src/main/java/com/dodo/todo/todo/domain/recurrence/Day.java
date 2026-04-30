package com.dodo.todo.todo.domain.recurrence;

import java.time.DayOfWeek;

public enum Day {

    MO, TU, WE, TH, FR, SA, SU;

    public DayOfWeek toDayOfWeek() {
        return switch (this) {
            case MO -> DayOfWeek.MONDAY;
            case TU -> DayOfWeek.TUESDAY;
            case WE -> DayOfWeek.WEDNESDAY;
            case TH -> DayOfWeek.THURSDAY;
            case FR -> DayOfWeek.FRIDAY;
            case SA -> DayOfWeek.SATURDAY;
            case SU -> DayOfWeek.SUNDAY;
        };
    }
}
