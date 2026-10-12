package server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Deck {

    private List<Card> cards = new ArrayList<>();

    public Deck() {
    	// 섯다 20장 생성
        for (int num = 1; num <= 10; num++) {

            if (num == 1 || num == 3 || num == 8) {
                // 1, 3, 8은
                //   variant=1  → 광패 (1광, 3광, 8광)
                //   variant=2  → 일반패 (1.png, 3.png, 8.png)
                cards.add(new Card(num, true, 1));   // 광
                cards.add(new Card(num, false, 2));  // 일반
            } else {
                // 나머지 숫자는 일반패 두 장 (짝패)
                cards.add(new Card(num, false, 1));  // ex) 2_1, 4_1 ...
                cards.add(new Card(num, false, 2));  // ex) 2_2, 4_2 ...
            }
        }
    }

    public void shuffle() {
        Collections.shuffle(cards);
    }

    public Card draw() {
        return cards.remove(0);
    }
}
