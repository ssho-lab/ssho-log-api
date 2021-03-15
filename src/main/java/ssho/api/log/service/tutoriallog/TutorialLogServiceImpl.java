package ssho.api.log.service.tutoriallog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.stereotype.Service;
import ssho.api.log.domain.tutoriallog.model.TutorialLog;
import ssho.api.log.domain.tutoriallog.model.req.TutorialLogReq;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
    public void saveTutorialLog(final TutorialLogReq tutorialReq, final String index, final String userId) throws IOException {

        int tutorialSeq = getTutorialSeqByUserId(index, userId) + 1;

        if(tutorialSeq > 2){
            return;
        }

        for (TutorialLog tutorialLog : tutorialReq.getTutorialList()) {
            tutorialLog.setTutorialSeq(tutorialSeq);
        }

        final BulkRequest bulkRequest = new BulkRequest();

        // duration 계산 및 field set
        calculateAndSetDuration(tutorialReq);

        // swipe log 별로 IndexRequest 생성 후 bulkRequest에 추가
        for (TutorialLog t : tutorialReq.getTutorialList()) {

            // docId 생성
            final String docId = t.getUserId() + "-" + t.getItemId() + "-" + t.getSwipeTime();

            IndexRequest indexRequest =
                    new IndexRequest(index)
                            .id(docId)
                            .source(objectMapper.writeValueAsString(t), XContentType.JSON);

            bulkRequest.add(indexRequest);
        }

        // bulk 요청
        restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
    }

    @Override
    public List<TutorialLog> getTutorialLog(final String index, final String userId) throws IOException {

        List<TutorialLog> tutorialLogList = new ArrayList<>();

        if(!indexExists(index)){
            return tutorialLogList;
        }

        // ES에 요청 보내기
        SearchRequest searchRequest = new SearchRequest(index);

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.termQuery("userId", userId));
        sourceBuilder.sort("tutorialSeq", SortOrder.DESC);
        sourceBuilder.sort("cardSeq", SortOrder.ASC);

        // search query 최대 크기 set
        sourceBuilder.size(1000);

        searchRequest.source(sourceBuilder);

        // ES로 부터 데이터 받기
        SearchResponse searchResponse;

        searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

        SearchHit[] searchHits = searchResponse.getHits().getHits();

        tutorialLogList = Arrays.stream(searchHits).map(hit -> {
            try {
                return objectMapper.readValue(hit.getSourceAsString(), TutorialLog.class);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return null;
            }
        }).collect(Collectors.toList());

        return tutorialLogList;
    }

    @Override
    public int getTutorialSeqByUserId(final String index, final String userId) throws IOException {

        if(!indexExists(index)){
            return -1;
        }

        // ES에 요청 보내기
        SearchRequest searchRequest = new SearchRequest(index);

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.termQuery("userId", userId));
        sourceBuilder.sort("tutorialSeq", SortOrder.DESC);
        sourceBuilder.sort("cardSeq", SortOrder.ASC);

        // search query 최대 크기 set
        sourceBuilder.size(1000);

        searchRequest.source(sourceBuilder);

        // ES로 부터 데이터 받기
        SearchResponse searchResponse;

        searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

        SearchHit[] searchHits = searchResponse.getHits().getHits();

        List<TutorialLog> tutorialLogList = Arrays.stream(searchHits).map(hit -> {
            try {
                return objectMapper.readValue(hit.getSourceAsString(), TutorialLog.class);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return null;
            }
        }).collect(Collectors.toList());

        if(tutorialLogList.size() == 0){
            return 0;
        }

        return tutorialLogList.get(0).getTutorialSeq();
    }

    /**
     * SwipeTime 간의 차이를 활용한 duration 계산 및 set
     *
     * @param tutorialReq
     */
    private void calculateAndSetDuration(final TutorialLogReq tutorialReq) {

        // 이전 로그의 swipeTime
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        LocalDateTime prevTime = LocalDateTime.parse(tutorialReq.getStartTime(), formatter);

        List<TutorialLog> tutorialList = tutorialReq.getTutorialList();

        // duration 계산
        for (TutorialLog t : tutorialList) {
            Duration duration = Duration.between(prevTime, LocalDateTime.parse(t.getSwipeTime(), formatter));
            t.setDuration((int) duration.getSeconds());
            prevTime = LocalDateTime.parse(t.getSwipeTime(), formatter);
        }
    }

    private boolean indexExists(String index) throws IOException {
        GetIndexRequest request = new GetIndexRequest(index);
        return restHighLevelClient.indices().exists(request, RequestOptions.DEFAULT);
    }
}
