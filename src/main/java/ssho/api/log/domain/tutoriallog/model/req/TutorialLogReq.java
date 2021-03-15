package ssho.api.log.domain.tutoriallog.model.req;

import lombok.Data;
import ssho.api.log.domain.tutoriallog.model.TutorialLog;

import java.util.List;

@Data
public class TutorialLogReq {
    private String startTime;
    private List<TutorialLog> tutorialList;
}
