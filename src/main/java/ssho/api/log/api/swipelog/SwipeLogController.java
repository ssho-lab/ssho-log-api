package ssho.api.log.api.swipelog;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ssho.api.log.domain.swipelog.model.SwipeLog;
import ssho.api.log.domain.swipelog.model.req.SwipeLogReq;
import ssho.api.log.domain.swipelog.model.res.UserSwipeLogRes;
import ssho.api.log.domain.tag.model.ExpTag;
import ssho.api.log.domain.user.model.User;
import ssho.api.log.service.swipelog.SwipeLogServiceImpl;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 스와이프 로그 수집 컨트롤러
 */
@Slf4j
@RequestMapping("/log/swipe")
@RestController
public class SwipeLogController {

    private final SwipeLogServiceImpl swipeLogCollectorService;

    public SwipeLogController(final SwipeLogServiceImpl swipeLogCollectorService) {
        this.swipeLogCollectorService = swipeLogCollectorService;
    }

    /**
     * 스와이프 로그 수집
     *
     * @throws IOException
     */
    @PostMapping("")
    public void collectSwipeLog(@RequestBody SwipeLogReq swipeReq) throws IOException {
        swipeLogCollectorService.saveSwipeLogs(swipeReq, "activity-log-swipe");
    }

    /**
     * 스와이프 로그 전체 조회
     *
     * @return List<String>
     * @throws IOException
     */
    @GetMapping("")
    public List<SwipeLog> getAllSwipeLogs() throws IOException {
        return swipeLogCollectorService.getSwipeLogs("activity-log-swipe");
    }

    /**
     * 사용자별 스와이프 로그 전체 조회
     *
     * @param userId
     * @return
     * @throws IOException
     */
    @GetMapping("/user")
    public List<SwipeLog> getAllSwipeLogsByUserId(@RequestParam("userId") final String userId) throws IOException {
        return swipeLogCollectorService.getSwipeLogsByUserId("activity-log-swipe", userId);
    }

    /**
     * 사용자별 좋아요한 스와이프 로그 전체 조회
     *
     * @param userId
     * @return
     * @throws IOException
     */
    @GetMapping("/user/like")
    public List<SwipeLog> getAllLikedSwipeLogsByUserId(@RequestParam("userId") final String userId) throws IOException {

        return swipeLogCollectorService.getSwipeLogsByUserIdAndScore("activity-log-swipe", userId, 1);
    }

    /**
     * 사용자별 좋아요한 스와이프 로그 전체 조회
     *
     * @param userId
     * @return
     * @throws IOException
     */
    @GetMapping("/user/like/grouped")
    public Map<Integer, List<SwipeLog>> getAllGroupedLikedSwipeLogsByUserId(@RequestParam("userId") final String userId) throws IOException {
        return swipeLogCollectorService.getSwipeLogListGroupedByCardSetSeq("activity-log-swipe", userId, 1);
    }

    @PostMapping("/user/grouped")
    public List<UserSwipeLogRes> getUserSwipeLogResList(@RequestBody final List<User> userList) throws IOException {
        return swipeLogCollectorService.getSwipeLogsGroupedByUserId("activity-log-swipe", userList);
    }

    @GetMapping("/user/tag")
    public List<ExpTag> getExpTagListOrderedByTagCountByUserId(@RequestParam("userId") final String userId) {
        return swipeLogCollectorService.getExpTagListOrderBySwipeCount("activity-log-swipe", userId, 1);
    }
}

