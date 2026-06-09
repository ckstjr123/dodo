package com.dodo.todo.todo.dto;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dodo.todo.common.exception.BusinessException;
import com.dodo.todo.recurrencerule.Day;
import com.dodo.todo.recurrencerule.Frequency;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RecurrenceRuleRequestTest {

    @Test
    @DisplayName("일 단위 반복은 상세 반복 조건을 허용하지 않는다")
    void rejectDailyDetailValues() {
        RecurrenceRuleRequest request = new RecurrenceRuleRequest(
                Frequency.DAILY,
                1,
                new ByDayRequest(null, List.of(Day.MO)),
                null,
                null
        );

        assertThatThrownBy(request::toRecurrenceRule)
                .isInstanceOf(BusinessException.class)
                .hasMessage(RecurrenceRuleRequestError.DAILY_DETAIL_VALUES.message());
    }

    @Test
    @DisplayName("주 단위 반복은 offset을 허용하지 않는다")
    void rejectWeeklyOffset() {
        RecurrenceRuleRequest request = new RecurrenceRuleRequest(
                Frequency.WEEKLY,
                1,
                new ByDayRequest(1, List.of(Day.MO)),
                null,
                null
        );

        assertThatThrownBy(request::toRecurrenceRule)
                .isInstanceOf(BusinessException.class)
                .hasMessage(RecurrenceRuleRequestError.WEEKLY_OFFSET_NOT_ALLOWED.message());
    }

    @Test
    @DisplayName("월 byDay는 특정 주차의 특정 요일 하나만 허용한다")
    void rejectMonthlyByDayMultipleDays() {
        RecurrenceRuleRequest request = new RecurrenceRuleRequest(
                Frequency.MONTHLY,
                1,
                new ByDayRequest(2, List.of(Day.MO, Day.TU)),
                null,
                null
        );

        assertThatThrownBy(request::toRecurrenceRule)
                .isInstanceOf(BusinessException.class)
                .hasMessage(RecurrenceRuleRequestError.MONTHLY_BY_DAY_SINGLE_DAY_REQUIRED.message());
    }

    @Test
    @DisplayName("byDay와 byMonthDay를 함께 사용할 수 없다")
    void rejectByDayAndByMonthDayTogether() {
        RecurrenceRuleRequest request = new RecurrenceRuleRequest(
                Frequency.MONTHLY,
                1,
                new ByDayRequest(2, List.of(Day.MO)),
                15,
                null
        );

        assertThatThrownBy(request::toRecurrenceRule)
                .isInstanceOf(BusinessException.class)
                .hasMessage(RecurrenceRuleRequestError.BY_DAY_AND_BY_MONTH_DAY_TOGETHER.message());
    }
}
