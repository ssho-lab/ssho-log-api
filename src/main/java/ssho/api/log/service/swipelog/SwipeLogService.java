package ssho.api.log.service.swipelog;

import com.fasterxml.jackson.core.JsonProcessingException;
import ssho.api.log.domain.swipelog.model.SwipeLog;
import ssho.api.log.domain.swipelog.model.req.SwipeLogReq;
import ssho.api.log.domain.swipelog.model.res.UserSwipeLogRes;
import ssho.api.log.domain.tag.model.ExpTag;
import ssho.api.log.domain.user.model.User;

import java.io.IOException;
import java.util.List;

public interface SwipeLogService {

    /**
     * 스와이프 로그 Bulk Request
     *
     * @param swipeReq
     * @param index
     * @throws IOException
     */
    void saveSwipeLogs(final SwipeLogReq swipeReq, final String index) throws IOException;

    /**
     * 스와이프 로그 Search Request
     *
     * @param index
     * @return
     */
    List<SwipeLog> getSwipeLogs(final String index);

    List<UserSwipeLogRes> getSwipeLogsGroupedByUserId(final String index, final List<User> userList) throws IOException;

    /**
     * 사용자 별 스와이프 로그 Search Request
     *
     * @param index
     * @param userId
     * @return
     */
    List<SwipeLog> getSwipeLogsByUserId(final String index, final String userId) throws JsonProcessingException;

    List<ExpTag> getExpTagListOrderBySwipeCount(String index, String userId, final int score);
}
