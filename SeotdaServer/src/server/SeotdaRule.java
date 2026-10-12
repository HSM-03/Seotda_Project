package server;

public class SeotdaRule {

    public static HandRank evaluate(Card c1, Card c2) {
        int n1 = c1.getNumber();
        int n2 = c2.getNumber();
        boolean g1 = c1.isGwang();
        boolean g2 = c2.isGwang();

        int hi = Math.max(n1, n2);
        int lo = Math.min(n1, n2);

        // 1️⃣ 광땡
        if (g1 && g2) {
            if ((n1 == 3 && n2 == 8) || (n1 == 8 && n2 == 3)) {
                return new HandRank(100, "38광땡");
            } else if ((n1 == 1 && n2 == 3) || (n1 == 3 && n2 == 1)) {
                return new HandRank(99, "13광땡");
            } else if ((n1 == 1 && n2 == 8) || (n1 == 8 && n2 == 1)) {
                return new HandRank(98, "18광땡");
            }
            // (이 외의 광·광 조합은 보통 사용 안 하니까 필요하면 나중에 추가)
        }

        // 2️⃣ 땡 (숫자 같은 경우)
        if (n1 == n2) {
            // 10땡이 제일 쎄게, 1땡이 제일 약하게
            int base = 80;  // 땡은 81~90 범위 사용
            int score = base + n1; // 10땡 = 90, 9땡 = 89, ..., 1땡 = 81

            String name;
            if (n1 == 10) name = "장땡";
            else if (n1 == 1) name = "1땡";
            else name = n1 + "땡";

            return new HandRank(score, name);
        }

        // 3️⃣ 특수 족보 (순서대로 강함)
        // 카드 순서는 상관 없이 (lo, hi) 기준으로 비교
        if (lo == 1 && hi == 2) {
            return new HandRank(70, "알리");
        } else if (lo == 1 && hi == 4) {
            return new HandRank(69, "독사");
        } else if (lo == 1 && hi == 9) {
            return new HandRank(68, "구삥");
        } else if (lo == 1 && hi == 10) {
            return new HandRank(67, "장삥");
        } else if (lo == 4 && hi == 10) {
            return new HandRank(66, "장사");
        } else if (lo == 4 && hi == 6) {
            return new HandRank(65, "세륙");
        }

        // 4️⃣ 끗 (합 % 10)
        int kkut = (n1 + n2) % 10;
        String name;
        int score;

        if (kkut == 0) {
            name = "망통";     // 0끗
            score = 10;       // 제일 약하게
        } else {
            name = kkut + "끗";
            score = 10 + kkut;  // 1끗=11 ~ 9끗=19
        }
        
        
        // ====================== 특수 족보 (이름 + 기본점수) ======================
        // 47 암행어사: 기본은 1끗 (4+7=11), 광땡 있을 때만 진짜 힘 발휘
        if (isAmhaeng(c1, c2)) {
            return new HandRank(11, "47암행어사"); // 기본 1끗
        }

        // 37 땡잡이: 기본은 망통 (3+7=10), 땡 있을 때만 힘 발휘
        if (isDdengJabi(c1, c2)) {
            return new HandRank(10, "37땡잡이"); // 기본 망통
        }

        // 멍텅구리 구사 / 구사: 기본은 망통으로 처리
        if (isMungGuriGusa(c1, c2)) {
            return new HandRank(10, "멍텅구리 구사");
        }
        
        if (isGusa(c1, c2)) {
            return new HandRank(10, "구사");
        }

        return new HandRank(score, name);
    }
    
    
    // 두 사람 손패 비교하는 헬퍼 메서드 (나중에 서버에서 쓰기 좋게)
    public static int compareHands(Card a1, Card a2, Card b1, Card b2) {
        HandRank hrA = evaluate(a1, a2);
        HandRank hrB = evaluate(b1, b2);
        
     // 0) 구사 / 멍텅구리 구사 → 재경기 (여기서는 무승부 처리)
        boolean aGusa = isGusa(a1, a2) || isMungGuriGusa(a1, a2);
        boolean bGusa = isGusa(b1, b2) || isMungGuriGusa(b1, b2);
        if (aGusa || bGusa) {
            return 0; // 무승부 → 서버에서 DRAW 처리
        }
        
     // 1) 47 암행어사 처리
        boolean aAm = isAmhaeng(a1, a2);
        boolean bAm = isAmhaeng(b1, b2);
        boolean aIs13or18 = is13or18Gwangddeng(a1, a2);
        boolean bIs13or18 = is13or18Gwangddeng(b1, b2);
        
     // 2) 37 땡잡이 처리
        boolean aDj = isDdengJabi(a1, a2);
        boolean bDj = isDdengJabi(b1, b2);
        boolean aIs1to9Ddeng = is1to9Ddeng(a1, a2);
        boolean bIs1to9Ddeng = is1to9Ddeng(b1, b2);

        if (aDj && bIs1to9Ddeng) return 1;   // A 승
        if (bDj && aIs1to9Ddeng) return -1;  // B 승

        if (aAm && bIs13or18) return 1;   // A 승
        if (bAm && aIs13or18) return -1;  // B 승

        return Integer.compare(hrA.getScore(), hrB.getScore());
        // > 0 이면 B보다 A가 강함
        // < 0 이면 B가 더 강함
        // == 0 이면 동급 (동점 처리 필요)
    }
    
 // ====================== 헬퍼 메서드들 ======================

    // 47 암행어사: 4_1 + 7_1 조합일 때만 인정
    private static boolean isAmhaeng(Card c1, Card c2) {

        // 4_1 조건 (4 일반, variant == 1)
        boolean has4_1 =
                (c1.getNumber() == 4 && !c1.isGwang() && c1.getVariant() == 1) ||
                (c2.getNumber() == 4 && !c2.isGwang() && c2.getVariant() == 1);

        // 7_1 조건 (7 일반, variant == 1)
        boolean has7_1 =
                (c1.getNumber() == 7 && !c1.isGwang() && c1.getVariant() == 1) ||
                (c2.getNumber() == 7 && !c2.isGwang() && c2.getVariant() == 1);

        // 둘 다 만족해야 47암행어사
        return has4_1 && has7_1;
    }
    
    // 37 땡잡이: 3월 광 + 7_1 조합일 때만 인정
    private static boolean isDdengJabi(Card c1, Card c2) {

        // 3광 조건
        boolean has3Gwang =
                (c1.getNumber() == 3 && c1.isGwang()) ||
                (c2.getNumber() == 3 && c2.isGwang());

        // 7_1 조건 (7 일반, variant == 1)
        boolean has7_1 =
                (c1.getNumber() == 7 && !c1.isGwang() && c1.getVariant() == 1) ||
                (c2.getNumber() == 7 && !c2.isGwang() && c2.getVariant() == 1);

        // 두 조건 모두 만족해야 37땡잡이
        return has3Gwang && has7_1;
    }

    
    private static boolean isGusa(Card c1, Card c2) {
        int n1 = c1.getNumber();
        int n2 = c2.getNumber();

        // 숫자가 4와 9 조합인지
        boolean is49 = (n1 == 4 && n2 == 9) || (n1 == 9 && n2 == 4);

        // 둘 다 일반패일 때만 구사
        return is49 && !c1.isGwang() && !c2.isGwang();
    }

    
    private static boolean isMungGuriGusa(Card c1, Card c2) {
        int n1 = c1.getNumber();
        int n2 = c2.getNumber();

        boolean is4_1 =
                (n1 == 4 && c1.getVariant() == 1 && !c1.isGwang()) ||
                (n2 == 4 && c2.getVariant() == 1 && !c2.isGwang());

        boolean is9_1 =
                (n1 == 9 && c1.getVariant() == 1 && !c1.isGwang()) ||
                (n2 == 9 && c2.getVariant() == 1 && !c2.isGwang());

        return is4_1 && is9_1;   // 둘 다 만족할 때만 멍텅구리 구사
    }

    // 13광땡 / 18광땡 인지 체크 (암행어사용)
    private static boolean is13or18Gwangddeng(Card c1, Card c2) {
        int n1 = c1.getNumber();
        int n2 = c2.getNumber();
        boolean g1 = c1.isGwang();
        boolean g2 = c2.isGwang();

        if (!(g1 && g2)) return false;

        int hi = Math.max(n1, n2);
        int lo = Math.min(n1, n2);
        return (lo == 1 && (hi == 3 || hi == 8));
    }

    // 1땡~9땡인지 체크 (땡잡이용, 장땡/광땡 제외)
    private static boolean is1to9Ddeng(Card c1, Card c2) {
        int n1 = c1.getNumber();
        int n2 = c2.getNumber();
        if (n1 != n2) return false;
        return (n1 >= 1 && n1 <= 9);
    }
}

