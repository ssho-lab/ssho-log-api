package ssho.api.log.service.tutoriallog;

import ssho.api.log.domain.tutoriallog.model.TutorialLog;

import java.io.IOException;

public interface TutorialLogService {
    void saveTutorialLog(final TutorialLog tutorialLog, final String index) throws IOException;

    boolean checkTutorialDone(final String userId, final String index);
}
