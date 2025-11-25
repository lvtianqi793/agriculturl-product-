package com.ltqtest.springbootquickstart.repository;

import com.ltqtest.springbootquickstart.entity.BuyRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

/**
 * 求购需求Repository接口
 */
public interface BuyRequestRepository extends JpaRepository<BuyRequest, Integer> {
    
    /**
     * 根据关键字搜索求购需求
     * @param keyword 搜索关键字
     * @return 匹配的求购需求列表
     */
    @Query("SELECT b FROM BuyRequest b WHERE b.title LIKE %:keyword% OR b.content LIKE %:keyword%")
    List<BuyRequest> searchByKeyword(@Param("keyword") String keyword);
    
    /**
     * 按创建时间降序查询所有求购需求
     * @return 求购需求列表
     */
    List<BuyRequest> findAllByOrderByCreateTimeDesc();
    
    /**
     * 按创建时间升序查询所有求购需求
     * @return 求购需求列表
     */
    List<BuyRequest> findAllByOrderByCreateTimeAsc();
    /**
     * 删除求购需求
     * @param buyRequestId 求购ID
     */
    void deleteById(Integer buyRequestId);
    boolean existsById(Integer buyRequestId);
}