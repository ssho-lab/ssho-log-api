package ssho.api.log.domain.swipelog;

import lombok.Data;
import ssho.api.log.domain.tag.model.Tag;

import java.util.List;

@Data
public class SwipeLog implements Comparable<SwipeLog> {

    private int userId;         // 회원 고유 번호
    private String itemId;      // 상품 고유 번호
    private int score;          // 스와이프 score
    private String swipeTime;   // 스와이프 로그 생성 시각
    private int duration;       // 해당 상품 카드에서 머문 시간(sec)
    private List<Tag> tagList;
    private int cardSeq;
    private int userCardSetId;

    @Override
    public int compareTo(SwipeLog swipeLog) {
        if (this.cardSeq > swipeLog.getCardSeq()) return 1;
        else if (this.cardSeq < swipeLog.getCardSeq()) return -1;
        return 0;
    }
}
