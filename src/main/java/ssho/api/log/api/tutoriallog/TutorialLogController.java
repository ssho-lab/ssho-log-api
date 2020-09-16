package ssho.api.log.api.tutoriallog;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ssho.api.log.domain.tutoriallog.model.TutorialLog;
import ssho.api.log.service.tutoriallog.TutorialLogServiceImpl;

import java.io.IOException;

@Slf4j
@RequestMapping("/log/tutorial")
@RestController
public class TutorialLogController {
    private TutorialLogServiceImpl tutorialLogService;

    public TutorialLogController(final TutorialLogServiceImpl tutorialLogService) {
        this.tutorialLogService = tutorialLogService;
    }

    @PostMapping("")
    public void saveTutorialLog(@RequestBody TutorialLog tutorialLog) throws IOException {
        tutorialLogService.saveTutorialLog(tutorialLog, "tutorial-log");
    }

    @GetMapping("")
    public boolean tutorialDone(@RequestParam("userId") final String userId) {
        return tutorialLogService.checkTutorialDone(userId, "tutorial-log");
    }
}
