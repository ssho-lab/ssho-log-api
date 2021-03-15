package ssho.api.log.domain.swipelog.req;

import lombok.Data;
import ssho.api.log.domain.swipelog.SwipeLog;

import java.util.List;

@Data
public class SwipeLogReq {
    private String startTime;
    private List<SwipeLog> swipeList;
}
