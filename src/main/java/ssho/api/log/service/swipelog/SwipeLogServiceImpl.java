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
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import ssho.api.log.domain.swipelog.model.SwipeLog;
import ssho.api.log.domain.swipelog.model.req.SwipeLogReq;
import ssho.api.log.domain.swipelog.model.res.UserSwipeLogRes;
import ssho.api.log.domain.tag.model.ExpTag;
import ssho.api.log.domain.user.model.User;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
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
    public List<UserSwipeLogRes> getSwipeLogsGroupedByUserId(final String index, final List<User> userList) throws IOException {

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

            searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

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
            userSwipeLogRes.setUserId(user.getId());
            userSwipeLogRes.setSwipeLogList(swipeLogList);
            userSwipeLogResList.add(userSwipeLogRes);
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
    public List<ExpTag> getExpTagListOrderBySwipeCount(String index, String userId, final int score) {
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

        List<ExpTag> tagList =
                webClient
                        .get().uri("/tag/real")
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<List<ExpTag>>() {
                        })
                        .block();

        tagList.stream().forEach(expTag -> log.info(expTag + ""));

        Map<ExpTag, Integer> tagCountMap = new HashMap<>();
        tagList.stream().forEach(tag -> tagCountMap.put(tag, 0));

        swipeLogList
                .stream()
                .forEach(swipeLog -> {
                    if (swipeLog.getExpTagList() != null) {
                        swipeLog.getExpTagList()
                                .stream()
                                .forEach(expTag -> tagCountMap.computeIfPresent(expTag, (ExpTag tag, Integer count) -> ++count));
                    }
                });

        List<ExpTag> orderedTagList = new ArrayList<>();

        tagCountMap.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEachOrdered(x -> orderedTagList.add(x.getKey()));

        return orderedTagList;
    }

    private int getPrevCardSetId(final String index, final String userId) throws JsonProcessingException {
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
