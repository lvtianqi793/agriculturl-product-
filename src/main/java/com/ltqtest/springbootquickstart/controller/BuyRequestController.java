package com.ltqtest.springbootquickstart.controller;

import com.ltqtest.springbootquickstart.common.Result;
import com.ltqtest.springbootquickstart.entity.BuyRequest;
import com.ltqtest.springbootquickstart.repository.BuyRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.logging.Logger;

/**
 * 求购需求控制器
 */
@RestController
@RequestMapping("/api/buyRequest")
public class BuyRequestController {
    
    private static final Logger logger = Logger.getLogger(BuyRequestController.class.getName());
    
    @Autowired
    private BuyRequestRepository buyRequestRepository;
    
    /**
     * 发布求购需求接口
     * 接口路径: POST /api/buyRequest/publish
     * @param requestBody 请求参数
     * @return Result 包含发布结果的响应
     */
    @PostMapping("/publish")
    public Result<Map<String, Object>> publishBuyRequest(@RequestBody Map<String, Object> requestBody) {
        try {
            // 参数验证
            if (requestBody == null) {
                return Result.error(400, "请求参数不能为空");
            }
            
            String title = (requestBody.get("title")).toString();
            String content = (requestBody.get("content")).toString();
            String contact = (requestBody.get("contact")).toString();
            
            if (title == null || title.trim().isEmpty()) {
                title = "暂无标题"; // 使用默认标题
            }
            
            if (content == null || content.trim().isEmpty()) {
                return Result.error(400, "求购说明内容不能为空");
            }
            
            // 创建求购需求对象
            BuyRequest buyRequest = new BuyRequest();
            buyRequest.setTitle(title);
            buyRequest.setContent(content);
            buyRequest.setContact(contact); // contact可以为null
            buyRequest.setCreateTime(new Date());
            
            // 保存到数据库
            BuyRequest saved = buyRequestRepository.save(buyRequest);
            
            // 构建响应数据
            Map<String, Object> data = new HashMap<>();
            data.put("buyRequestId", saved.getBuyRequestId());
            data.put("title", saved.getTitle());
            data.put("content", saved.getContent());
            data.put("contact", saved.getContact());
            data.put("createTime", saved.getCreateTime());
            
            return Result.success(200, "发布成功", data);
        } catch (Exception e) {
            logger.severe("发布求购需求失败: " + e.getMessage());
            e.printStackTrace();
            return Result.error(500, "服务器内部错误，请稍后再试");
        }
    }
    
    /**
     * 删除求购需求接口
     * 接口路径: DELETE /api/buyRequest/delete
     * @param buyRequestId 求购ID
     * @return Result 删除结果
     */
    @DeleteMapping("/delete")
    public Result<?> deleteBuyRequest(@RequestParam("id") Integer buyRequestId) {
        try {
            // 参数验证
            if (buyRequestId == null || buyRequestId <= 0) {
                return Result.error(400, "参数错误：求购ID不能为空且必须大于0");
            }
            
            // 检查求购需求是否存在
            if (!buyRequestRepository.existsById(buyRequestId)) {
                return Result.error(404, "求购需求不存在");
            }
            
            // 删除求购需求
            buyRequestRepository.deleteById(buyRequestId);
            
            return Result.success(200, "删除成功");
        } catch (Exception e) {
            logger.severe("删除求购需求失败: " + e.getMessage());
            e.printStackTrace();
            return Result.error(500, "服务器内部错误，请稍后再试");
        }
    }
    
    /**
     * 检索求购需求接口
     * 接口路径: GET /api/buyRequest/search
     * @param keyword 搜索关键字
     * @param sort 排序方式 (time_desc/time_asc)
     * @return Result 包含搜索结果的响应
     */
    @GetMapping("/search")
    public Result<List<Map<String, Object>>> searchBuyRequests(
        @RequestBody Map<String, String> requestBody) {
        try {
            List<BuyRequest> buyRequests;
            
            // 根据是否有关键字决定搜索方式
            if (requestBody.containsKey("keyword") && !requestBody.get("keyword").trim().isEmpty()) {
                String keyword = requestBody.get("keyword").trim();
                buyRequests = buyRequestRepository.searchByKeyword(keyword);
                // 根据排序参数进行排序
                if ("time_asc".equals(requestBody.get("sort"))) {
                    buyRequests.sort(Comparator.comparing(BuyRequest::getCreateTime));
                } else {
                    buyRequests.sort(Comparator.comparing(BuyRequest::getCreateTime).reversed());
                }
            } else {
                // 没有关键字时，使用排序方法查询所有
                if ("time_asc".equals(requestBody.get("sort"))) {
                    buyRequests = buyRequestRepository.findAllByOrderByCreateTimeAsc();
                } else {
                    buyRequests = buyRequestRepository.findAllByOrderByCreateTimeDesc();
                }
            }
            
            // 构建响应数据
            List<Map<String, Object>> dataList = new ArrayList<>();
            for (BuyRequest buyRequest : buyRequests) {
                Map<String, Object> data = new HashMap<>();
                data.put("buyRequestId", buyRequest.getBuyRequestId());
                data.put("title", buyRequest.getTitle());
                data.put("content", buyRequest.getContent());
                data.put("contact", buyRequest.getContact());
                data.put("createTime", buyRequest.getCreateTime());
                dataList.add(data);
            }
            
            return Result.success(200, "搜索成功", dataList);
        } catch (Exception e) {
            logger.severe("检索求购需求失败: " + e.getMessage());
            e.printStackTrace();
            return Result.error(500, "服务器内部错误，请稍后再试");
        }
    }
    
    /**
     * 获取求购需求列表接口
     * 接口路径: GET /api/buyRequest/list
     * @return Result 包含求购需求列表的响应
     */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> getBuyRequestList() {
        try {
            // 查询所有求购需求，按创建时间降序排列
            List<BuyRequest> buyRequests = buyRequestRepository.findAllByOrderByCreateTimeDesc();
            
            // 构建响应数据
            List<Map<String, Object>> dataList = new ArrayList<>();
            for (BuyRequest buyRequest : buyRequests) {
                Map<String, Object> data = new HashMap<>();
                data.put("buyRequestId", buyRequest.getBuyRequestId());
                data.put("title", buyRequest.getTitle());
                data.put("content", buyRequest.getContent());
                data.put("contact", buyRequest.getContact());
                data.put("createTime", buyRequest.getCreateTime());
                dataList.add(data);
            }
            
            return Result.success(200, "获取求购需求列表成功", dataList);
        } catch (Exception e) {
            logger.severe("获取求购需求列表失败: " + e.getMessage());
            e.printStackTrace();
            return Result.error(500, "服务器内部错误，请稍后再试");
        }
    }
}