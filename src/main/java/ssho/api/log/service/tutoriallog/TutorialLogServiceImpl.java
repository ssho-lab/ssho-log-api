package ssho.api.log.service.tutoriallog;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Service;
import ssho.api.log.domain.tutoriallog.model.TutorialLog;

import java.io.IOException;

@Slf4j
@Service
public class TutorialLogServiceImpl implements TutorialLogService {

    private RestHighLevelClient restHighLevelClient;
    private ObjectMapper objectMapper;

    public TutorialLogServiceImpl(final RestHighLevelClient restHighLevelClient, final ObjectMapper objectMapper) {
        this.restHighLevelClient = restHighLevelClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public void saveTutorialLog(final TutorialLog tutorialLog, final String index) throws IOException {

        IndexRequest indexRequest =
                new IndexRequest(index)
                        .source(objectMapper.writeValueAsString(tutorialLog), XContentType.JSON);

        restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
    }

    @Override
    public boolean checkTutorialDone(final String userId, final String index) {

        SearchRequest searchRequest = new SearchRequest(index);

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.termQuery("userId", userId));

        // search query 최대 크기 set
        sourceBuilder.size(1000);

        searchRequest.source(sourceBuilder);

        // ES로 부터 데이터 받기
        SearchResponse searchResponse;

        try {
            searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        SearchHit[] searchHits = searchResponse.getHits().getHits();

        return searchHits.length > 0;
    }
}
