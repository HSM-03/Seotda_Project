package server;

public class Player {
    private String name;
    private Card card1;
    private Card card2;

    public Player(String name) {
        this.name = name;
    }

    public void setCards(Card c1, Card c2) {
        this.card1 = c1;
        this.card2 = c2;
    }

    public Card getCard1() { return card1; }
    public Card getCard2() { return card2; }

    public String getName() {
        return name;
    }
}
