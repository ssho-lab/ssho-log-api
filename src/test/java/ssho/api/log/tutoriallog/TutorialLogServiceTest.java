package ssho.api.log.tutoriallog;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ssho.api.log.domain.tutoriallog.model.TutorialLog;
import ssho.api.log.domain.tutoriallog.model.req.TutorialLogReq;
import ssho.api.log.service.tutoriallog.TutorialLogServiceImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@SpringBootTest
public class TutorialLogServiceTest {
    @Autowired
    TutorialLogServiceImpl tutorialLogService;

    @Test
    void getTutorialLogsByUserId() {
    }

    @Test
    void saveTutorialLog() throws IOException {

        TutorialLogReq tutorialLogReq = new TutorialLogReq();
        List<TutorialLog> tutorialLogList = new ArrayList<>();

        TutorialLog tutorialLog = TutorialLog.builder().duration(0).itemId("4").score(1).swipeTime("2021-01-19 17:03:20").userId(5).build();
        tutorialLogList.add(tutorialLog);

        tutorialLogReq.setStartTime("2021-01-19 17:03:20");
        tutorialLogReq.setTutorialList(tutorialLogList);
        tutorialLogService.saveTutorialLog(tutorialLogReq, "tutorial-log", "5");
    }
}
