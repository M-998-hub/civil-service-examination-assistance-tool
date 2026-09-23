package com.m998.civilservice.modules.match.rule;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ScoreResult {
    private int score;
    private String detail;

    public static ScoreResult of(int score, String detail) {
        return new ScoreResult(score, detail);
    }

    public static ScoreResult zero() {
        return new ScoreResult(0, null);
    }
}
