package ssho.api.log.api.tutoriallog;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ssho.api.log.domain.tutoriallog.model.TutorialLog;
import ssho.api.log.domain.tutoriallog.model.req.TutorialLogReq;
import ssho.api.log.service.tutoriallog.TutorialLogServiceImpl;
import ssho.api.log.util.auth.Auth;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

@Slf4j
@RequestMapping("/log/tutorial")
@RestController
public class TutorialLogController {
    private TutorialLogServiceImpl tutorialLogService;

    private final String TUTORIAL_INDEX = "tutorial-log";

    public TutorialLogController(final TutorialLogServiceImpl tutorialLogService) {
        this.tutorialLogService = tutorialLogService;
    }

    @Auth
    @PostMapping("")
    public void saveTutorialLog(@RequestBody TutorialLogReq tutorialReq, final HttpServletRequest httpServletRequest) throws IOException {

        final int userId = (int)httpServletRequest.getAttribute("userId");

        tutorialReq.getTutorialList().forEach(tutorialLog -> tutorialLog.setUserId(userId));

        tutorialLogService.saveTutorialLog(tutorialReq, TUTORIAL_INDEX, String.valueOf(userId));
    }

    @GetMapping("")
    public List<TutorialLog> getTutorialLog(@RequestParam("userId") int userId) throws IOException {
        return tutorialLogService.getTutorialLog(TUTORIAL_INDEX, String.valueOf(userId));
    }

    @GetMapping("/yn")
    public boolean getTutorialYn(@RequestParam("userId") int userId) throws IOException {
        return tutorialLogService.getTutorialSeqByUserId(TUTORIAL_INDEX, String.valueOf(userId)) >= 2;
    }
}
