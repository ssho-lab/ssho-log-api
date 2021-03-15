package ssho.api.log.service.swipelog;

import com.fasterxml.jackson.core.JsonProcessingException;
import ssho.api.log.domain.swipelog.SwipeLog;
import ssho.api.log.domain.swipelog.req.SwipeLogReq;
import ssho.api.log.domain.swipelog.res.UserSwipeLogRes;
import ssho.api.log.domain.user.model.User;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface SwipeLogService {

    /**
     * 스와이프 로그 저장
     *
     * @param swipeReq
     * @param index
     * @throws IOException
     */
    void saveSwipeLogs(final SwipeLogReq swipeReq, final String index) throws IOException;

    /**
     * 스와이프 로그 전체 조회
     *
     * @param index
     * @return
     */
    List<SwipeLog> getSwipeLogs(final String index);

    /**
     * 사용자 별 스와이프 로그 조회
     *
     * @param index
     * @param userList
     * @return
     * @throws IOException
     */
    List<UserSwipeLogRes> getSwipeLogsGroupedByUserId(final String index, final List<User> userList) throws IOException;

    /**
     * 사용자 별 스와이프 로그 조회
     *
     * @param index
     * @param userId
     * @return
     */
    List<SwipeLog> getSwipeLogsByUserId(final String index, final String userId) throws JsonProcessingException;

    /**
     * 사용자 별 스와이프 로그 조회
     *
     * @param index
     * @param userId
     * @param score
     * @return
     */
    List<SwipeLog> getSwipeLogsByUserIdAndScore(final String index, final String userId, final int score);

    List<SwipeLog> getRecentSwipeLogsByUserIdAndScore(final String index, final String userId, final int score);

    Map<Integer, List<SwipeLog>> getSwipeLogListGroupedByCardSetSeq(final String index, final String userId, final int score);

    void deleteAllSwipeLogs(final String index) throws Exception;

    void deleteAllSwipeLogsByUserId(final String index, final int userId) throws IOException;
}
