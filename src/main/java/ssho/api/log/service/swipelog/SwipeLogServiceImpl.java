package ssho.api.log.service.swipelog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import ssho.api.log.domain.swipelog.model.SwipeLog;
import ssho.api.log.domain.swipelog.model.req.SwipeLogReq;
import ssho.api.log.domain.swipelog.model.res.UserSwipeLogRes;
import ssho.api.log.domain.user.model.User;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 스와이프 로그 수집 Service
 */
@Service
@Slf4j
public class SwipeLogServiceImpl implements SwipeLogService {

    private final RestHighLevelClient restHighLevelClient;
    private final WebClient webClient;
    private ObjectMapper objectMapper;

    public SwipeLogServiceImpl(final RestHighLevelClient restHighLevelClient,
                               final WebClient.Builder webClientBuilder,
                               final ObjectMapper objectMapper) {
        this.restHighLevelClient = restHighLevelClient;
        this.webClient = webClientBuilder.baseUrl("http://13.125.68.140:8083").build();
        this.objectMapper = objectMapper;
    }

    @Override
    public void saveSwipeLogs(final SwipeLogReq swipeReq, final String index) throws IOException {

        final BulkRequest bulkRequest = new BulkRequest();

        // duration 계산 및 field set
        calculateAndSetDuration(swipeReq);

        // 이전 카트셋 seq 조회
        int prevCardSetSeq = getPrevCardSetId(index, swipeReq.getSwipeList().get(0).getUserId());

        int cardSetSeq = prevCardSetSeq + 1;

        // swipe log 별로 IndexRequest 생성 후 bulkRequest에 추가
        for (SwipeLog s : swipeReq.getSwipeList()) {

            s.setCardSetSeq(cardSetSeq);

            // docId 생성
            final String docId =
                    s.getUserId() + "-" + s.getItemId() + "-" + s.getSwipeTime();

            IndexRequest indexRequest =
                    new IndexRequest(index)
                            .id(docId)
                            .source(objectMapper.writeValueAsString(s), XContentType.JSON);

            bulkRequest.add(indexRequest);
        }

        // bulk 요청
        restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
    }

    @Override
    public List<SwipeLog> getSwipeLogs(final String index) {

        // ES에 요청 보내기
        SearchRequest searchRequest = new SearchRequest(index);

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        sourceBuilder.sort("userId", SortOrder.ASC);
        sourceBuilder.sort("cardSetSeq", SortOrder.ASC);
        sourceBuilder.sort("cardSeq", SortOrder.ASC);

        // search query 최대 크기 set
        sourceBuilder.size(1000);

        searchRequest.source(sourceBuilder);

        // ES로 부터 데이터 받기
        SearchResponse searchResponse;

        try {
            searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }

        SearchHit[] searchHits = searchResponse.getHits().getHits();

        List<SwipeLog> swipeLogList = Arrays.stream(searchHits).map(hit -> {
            try {
                return objectMapper.readValue(hit.getSourceAsString(), SwipeLog.class);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return null;
            }
        }).collect(Collectors.toList());

        return swipeLogList;
    }

    @Override
    public List<UserSwipeLogRes> getSwipeLogsGroupedByUserId(final String index, final List<User> userList) {

        List<UserSwipeLogRes> userSwipeLogResList = new ArrayList<>();

        for (User user : userList) {

            // ES에 요청 보내기
            SearchRequest searchRequest = new SearchRequest(index);

            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            sourceBuilder.query(QueryBuilders.termQuery("userId", user.getId()));

            // search query 최대 크기 set
            sourceBuilder.size(1000);

            searchRequest.source(sourceBuilder);

            // ES로 부터 데이터 받기
            SearchResponse searchResponse;

            try {
                searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
                if (searchResponse.getHits().getHits().length == 0) {
                    UserSwipeLogRes userSwipeLogRes = new UserSwipeLogRes();
                    userSwipeLogRes.setUserId(String.valueOf(user.getId()));
                    userSwipeLogResList.add(userSwipeLogRes);
                    continue;
                }

                SearchHit[] searchHits = searchResponse.getHits().getHits();

                List<SwipeLog> swipeLogList = Arrays.stream(searchHits).map(hit -> {
                    try {
                        return objectMapper.readValue(hit.getSourceAsString(), SwipeLog.class);
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                        return null;
                    }
                }).sorted((a, b) -> {
                    if (Integer.parseInt(a.getItemId()) >= Integer.parseInt(b.getItemId())) {
                        return 1;
                    }
                    return -1;
                }).collect(Collectors.toList());

                UserSwipeLogRes userSwipeLogRes = new UserSwipeLogRes();
                userSwipeLogRes.setUserId(String.valueOf(user.getId()));
                userSwipeLogRes.setSwipeLogList(swipeLogList);
                userSwipeLogResList.add(userSwipeLogRes);
            } catch (Exception e) {
                UserSwipeLogRes userSwipeLogRes = new UserSwipeLogRes();
                userSwipeLogRes.setUserId(String.valueOf(user.getId()));
                userSwipeLogResList.add(userSwipeLogRes);
            }
        }
        return userSwipeLogResList;
    }

    @Override
    public List<SwipeLog> getSwipeLogsByUserId(final String index, final String userId) {

        // ES에 요청 보내기
        SearchRequest searchRequest = new SearchRequest(index);

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.termQuery("userId", userId));
        sourceBuilder.sort("cardSetSeq", SortOrder.ASC);
        sourceBuilder.sort("cardSeq", SortOrder.ASC);

        // search query 최대 크기 set
        sourceBuilder.size(1000);

        searchRequest.source(sourceBuilder);

        // ES로 부터 데이터 받기
        SearchResponse searchResponse;

        try {
            searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        } catch (Exception e) {
            return new ArrayList<>();
        }

        SearchHit[] searchHits = searchResponse.getHits().getHits();

        List<SwipeLog> swipeLogList = Arrays.stream(searchHits).map(hit -> {
            try {
                return objectMapper.readValue(hit.getSourceAsString(), SwipeLog.class);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return null;
            }
        }).collect(Collectors.toList());

        return swipeLogList;
    }

    public List<SwipeLog> getSwipeLogsByUserIdAndScore(final String index, final String userId, final int score) {

        // ES에 요청 보내기
        SearchRequest searchRequest = new SearchRequest(index);

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        QueryBuilder queryBuilder = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("userId", userId))
                .must(QueryBuilders.termQuery("score", score));

        sourceBuilder.query(queryBuilder);
        sourceBuilder.sort("cardSetSeq", SortOrder.ASC);
        sourceBuilder.sort("cardSeq", SortOrder.ASC);

        // search query 최대 크기 set
        sourceBuilder.size(1000);

        searchRequest.source(sourceBuilder);

        // ES로 부터 데이터 받기
        SearchResponse searchResponse;

        try {
            searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        } catch (Exception e) {
            return new ArrayList<>();
        }

        SearchHit[] searchHits = searchResponse.getHits().getHits();

        List<SwipeLog> swipeLogList = Arrays.stream(searchHits).map(hit -> {
            try {
                return objectMapper.readValue(hit.getSourceAsString(), SwipeLog.class);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return null;
            }
        }).collect(Collectors.toList());

        return swipeLogList;
    }

    public Map<Integer, List<SwipeLog>> getSwipeLogListGroupedByCardSetSeq(final String index, final String userId, final int score) {

        List<SwipeLog> swipeLogList = getSwipeLogsByUserIdAndScore(index, userId, score);

        Map<Integer, List<SwipeLog>> swipeLogListMap =
                swipeLogList.stream().collect(Collectors.groupingBy(SwipeLog::getCardSetSeq));

        return swipeLogListMap;
    }

    @Override
    public void deleteAllSwipeLogs(String index) throws IOException {
        DeleteByQueryRequest deleteByQueryRequest = new DeleteByQueryRequest(index);
        deleteByQueryRequest.setQuery(new MatchAllQueryBuilder());
        restHighLevelClient.deleteByQuery(deleteByQueryRequest, RequestOptions.DEFAULT);
    }

    @Override
    public void deleteAllSwipeLogsByUserId(String index, int userId) throws IOException {
        DeleteByQueryRequest deleteByQueryRequest = new DeleteByQueryRequest(index);
        deleteByQueryRequest.setQuery(new TermQueryBuilder("userId", userId));
        restHighLevelClient.deleteByQuery(deleteByQueryRequest, RequestOptions.DEFAULT);
    }

    private int getPrevCardSetId(final String index, final int userId) throws JsonProcessingException {
        // ES에 요청 보내기
        SearchRequest searchRequest = new SearchRequest(index);

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.termQuery("userId", userId));
        sourceBuilder.sort("cardSetSeq", SortOrder.DESC);

        // search query 최대 크기 set
        sourceBuilder.size(1);

        searchRequest.source(sourceBuilder);

        // ES로 부터 데이터 받기
        SearchResponse searchResponse;

        try {
            searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        } catch (Exception e) {
            return -1;
        }

        SearchHit[] searchHits = searchResponse.getHits().getHits();

        if (searchHits.length == 0) return -1;

        SearchHit hit = searchHits[0];

        int prevCardDeckSeq = objectMapper.readValue(hit.getSourceAsString(), SwipeLog.class).getCardSetSeq();

        return prevCardDeckSeq;
    }

    /**
     * SwipeTime 간의 차이를 활용한 duration 계산 및 set
     *
     * @param swipeReq
     */
    private void calculateAndSetDuration(final SwipeLogReq swipeReq) {

        // 이전 로그의 swipeTime

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        LocalDateTime prevTime = LocalDateTime.parse(swipeReq.getStartTime(), formatter);

        List<SwipeLog> swipeList = swipeReq.getSwipeList();

        // duration 계산
        for (SwipeLog s : swipeList) {
            Duration duration = Duration.between(prevTime, LocalDateTime.parse(s.getSwipeTime(), formatter));
            s.setDuration((int) duration.getSeconds());
            prevTime = LocalDateTime.parse(s.getSwipeTime(), formatter);
        }
    }
}
