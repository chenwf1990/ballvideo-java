package com.miguan.ballvideo.repositories;

import com.miguan.ballvideo.entity.es.FirstVideoEsVo;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

/**
 * @Author shixh
 * @Date 2020/1/7
 **/
public interface VideoEsRepository extends ElasticsearchRepository<FirstVideoEsVo,Long> {

    List<FirstVideoEsVo> findByGatherIdOrderByTotalWeight(long gatherId);

    List<FirstVideoEsVo> findByGatherId(long gatherId);

    List<FirstVideoEsVo> findByCreateDateLessThanEqual(long seconds);

    int countByGatherId(Long gatherId);

    void deleteByGatherId(Long gatherId);
}
