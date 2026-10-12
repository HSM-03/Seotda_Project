package server;

public class HandRank {
    private int score;      // 비교용 숫자 (클수록 강함)
    private String name;    // "38광땡", "장땡", "알리", "9끗", "망통" 등

    public HandRank(int score, String name) {
        this.score = score;
        this.name = name;
    }

    public int getScore() {	
        return score;
    }

    public String getName() {
        return name;
    }
}
