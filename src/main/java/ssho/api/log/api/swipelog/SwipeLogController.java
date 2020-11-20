package ssho.api.log.api.swipelog;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ssho.api.log.domain.swipelog.model.SwipeLog;
import ssho.api.log.domain.swipelog.model.req.SwipeLogReq;
import ssho.api.log.domain.swipelog.model.res.UserSwipeLogRes;
import ssho.api.log.domain.user.model.User;
import ssho.api.log.service.swipelog.SwipeLogServiceImpl;
import ssho.api.log.util.auth.Auth;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 스와이프 로그 수집 컨트롤러
 */
@Slf4j
@RequestMapping("/log/swipe")
@RestController
public class SwipeLogController {

    private final SwipeLogServiceImpl swipeLogService;

    private final String SWIPE_INDEX = "activity-log-swipe";
    private final String SWIPE_TEST_INDEX = "activity-log-swipe-test-20201119";

    public SwipeLogController(final SwipeLogServiceImpl swipeLogService) {
        this.swipeLogService = swipeLogService;
    }

    /**
     * 스와이프 로그 수집
     *
     * @throws IOException
     */
    @Auth
    @PostMapping("")
    public void collectSwipeLog(@RequestBody SwipeLogReq swipeReq, final HttpServletRequest httpServletRequest) throws IOException {

        final int userId = (int)httpServletRequest.getAttribute("userId");

        swipeReq.getSwipeList()
                .stream()
                .forEach(swipeLog -> swipeLog.setUserId(userId));

        swipeLogService.saveSwipeLogs(swipeReq, SWIPE_TEST_INDEX);
    }

    /**
     * 스와이프 로그 전체 조회
     * @return
     */
    @GetMapping("")
    public List<SwipeLog> getAllSwipeLogs() {
        try{
            return swipeLogService.getSwipeLogs(SWIPE_TEST_INDEX);
        }
        catch (Exception e){
            return new ArrayList<>();
        }
    }

    /**
     * 사용자별 스와이프 로그 전체 조회
     *
     * @param userId
     * @return
     * @throws IOException
     */
    @GetMapping("/user")
    public List<SwipeLog> getAllSwipeLogsByUserId(@RequestParam("userId") final String userId) {
        try {
            return swipeLogService.getSwipeLogsByUserId(SWIPE_TEST_INDEX, userId);
        }
        catch (Exception e){
            return new ArrayList<>();
        }
    }

    /**
     * 사용자별 좋아요한 스와이프 로그 전체 조회
     *
     * @param userId
     * @return
     * @throws IOException
     */
    @GetMapping("/user/like")
    public List<SwipeLog> getAllLikedSwipeLogsByUserId(@RequestParam("userId") final String userId) {
        return swipeLogService.getSwipeLogsByUserIdAndScore(SWIPE_TEST_INDEX, userId, 1);
    }

    /**
     * 사용자별 좋아요한 스와이프 로그 전체 조회(그룹핑)
     *
     * @param userId
     * @return
     * @throws IOException
     */
    @GetMapping("/user/like/grouped")
    public Map<Integer, List<SwipeLog>> getAllGroupedLikedSwipeLogsByUserId(@RequestParam("userId") final String userId) throws IOException {
        return swipeLogService.getSwipeLogListGroupedByCardSetSeq(SWIPE_TEST_INDEX, userId, 1);
    }

    /**
     * 사용자별 스와이프 로그 전체 조회(그룹핑)
     * @param userList
     * @return
     * @throws IOException
     */
    @PostMapping("/user/grouped")
    public List<UserSwipeLogRes> getUserSwipeLogResList(@RequestBody final List<User> userList) throws IOException {
        return swipeLogService.getSwipeLogsGroupedByUserId(SWIPE_TEST_INDEX, userList);
    }

    @DeleteMapping("")
    public void deleteAllSwipeLogs() throws IOException {
        swipeLogService.deleteAllSwipeLogs(SWIPE_TEST_INDEX);
    }

    @DeleteMapping("/user")
    public void deleteUserSwipeLogs(@RequestParam("userId") final int userId) throws IOException {
        swipeLogService.deleteAllSwipeLogsByUserId(SWIPE_TEST_INDEX, userId);
    }
}

