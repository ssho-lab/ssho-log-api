package ssho.api.log.domain.swipelog.model.req;

import lombok.Data;
import ssho.api.log.domain.swipelog.model.SwipeLog;

import java.util.List;

@Data
public class SwipeLogReq {
    private String startTime;
    private List<SwipeLog> swipeList;
}
