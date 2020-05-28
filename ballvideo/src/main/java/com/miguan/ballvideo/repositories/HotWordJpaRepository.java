package com.miguan.ballvideo.repositories;

import com.miguan.ballvideo.entity.HotWord;
import com.miguan.ballvideo.redis.util.CacheConstant;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface HotWordJpaRepository extends JpaRepository<HotWord, Long> {

    HotWord findTopByContent(String content);

    @Cacheable(value = CacheConstant.FIND_HOT_WORD_INFO, unless = "#result == null")
    @Query(value = "select * from hot_word where state=1 order by base_weight desc,updated_at desc limit 10", nativeQuery = true)
    List<HotWord> findHotWordInfo();
}
