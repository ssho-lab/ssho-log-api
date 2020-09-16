package ssho.api.log.domain.swipelog.model.res;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ssho.api.log.domain.swipelog.model.SwipeLog;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSwipeLogRes {
    private String userId;
    private List<SwipeLog> swipeLogList;
}
