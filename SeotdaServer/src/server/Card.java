package server;

public class Card {
    private int number;      // 1 ~ 10
    private boolean isGwang; // 광패인지 여부
    private int variant;     // 같은 숫자 카드 중 몇 번째(1 또는 2)
    
    // 기본 생성자: variant = 1 로 가정
    public Card(int number, boolean isGwang) {
        this(number, isGwang, 1);
    }

    
    public Card(int number, boolean isGwang, int variant) {
        this.number = number;
        this.isGwang = isGwang;
        this.variant = variant;
    }


    public int getNumber() {
        return number;
    }

    public boolean isGwang() {
        return isGwang;
    }
    
    public int getVariant() {
        return variant;
    }

    @Override
    public String toString() {
        if (isGwang) return number + "광";

        // 1,3,8 일반패는 "1", "3", "8" 그대로 보내기
        if (number == 1 || number == 3 || number == 8) {
            return String.valueOf(number);
        }

        // 나머지는 variant 붙여야 함
        return number + "_" + variant;
    }
}
