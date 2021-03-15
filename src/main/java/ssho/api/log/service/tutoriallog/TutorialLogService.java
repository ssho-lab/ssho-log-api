package ssho.api.log.service.tutoriallog;

import ssho.api.log.domain.tutoriallog.model.TutorialLog;
import ssho.api.log.domain.tutoriallog.model.req.TutorialLogReq;

import java.io.IOException;
import java.util.List;

public interface TutorialLogService {
    void saveTutorialLog(final TutorialLogReq tutorialReq, final String index, final String userId) throws IOException;
    List<TutorialLog> getTutorialLog(final String index, final String userId) throws IOException;
    int getTutorialSeqByUserId(final String index, final String userId) throws IOException;
}
