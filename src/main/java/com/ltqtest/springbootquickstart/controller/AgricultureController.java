
package com.ltqtest.springbootquickstart.controller;

import com.ltqtest.springbootquickstart.common.Result;
import com.ltqtest.springbootquickstart.entity.Product;
import com.ltqtest.springbootquickstart.entity.ProductComment;
import com.ltqtest.springbootquickstart.entity.Purchase;
import com.ltqtest.springbootquickstart.entity.ShoppingCart;
import com.ltqtest.springbootquickstart.entity.User;
import com.ltqtest.springbootquickstart.repository.ProductCommentRepository;
import com.ltqtest.springbootquickstart.repository.ProductRepository;
import com.ltqtest.springbootquickstart.repository.PurchaseRepository;
import com.ltqtest.springbootquickstart.repository.ShoppingCartRepository;
import com.ltqtest.springbootquickstart.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class AgricultureController {
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private ShoppingCartRepository shoppingCartRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PurchaseRepository purchaseRepository;
    
    @Autowired
    private ProductCommentRepository productCommentRepository;
    
    // 基础上传路径
    private final String uploadBasePath = "uploads/";
    // 图片上传子路径
    private final String productImgPath = "product_images/";
    
    /**
     * 将本地图片转化为可访问的URL
     * @param localImagePath 本地图片的完整路径
     * @param userId 用户ID，用于创建用户专属目录
     * @return 可通过HTTP访问的图片URL
     * @throws IOException 当文件操作失败时抛出异常
     */
    public String convertLocalImageToUrl(String localImagePath, Integer userId) throws IOException {
        // 验证参数
        if (localImagePath == null || localImagePath.isEmpty()) {
            throw new IOException("本地图片路径不能为空");
        }
        if (userId == null || userId <= 0) {
            throw new IOException("用户ID无效");
        }
        
        // 创建源文件对象
        File sourceFile = new File(localImagePath);
        
        // 验证源文件是否存在
        if (!sourceFile.exists() || !sourceFile.isFile()) {
            throw new IOException("本地图片文件不存在：" + localImagePath);
        }
        
        // 获取文件扩展名
        String originalFilename = sourceFile.getName();
        String extension = StringUtils.getFilenameExtension(originalFilename);
        
        // 确保扩展名不为空
        if (extension == null || extension.isEmpty()) {
            throw new IOException("无法识别图片文件类型");
        }
        
        // 生成唯一文件名，避免文件冲突
        String newFilename = "product_" + userId + "_" + UUID.randomUUID() + "." + extension;
        
        // 创建目标目录路径 - 基于用户ID的子目录
        String targetDirPath = uploadBasePath + productImgPath + userId + File.separator;
        File targetDir = new File(targetDirPath);
        
        // 如果目录不存在，创建目录
        if (!targetDir.exists()) {
            if (!targetDir.mkdirs()) {
                throw new IOException("无法创建目标目录：" + targetDirPath);
            }
        }
        
        // 创建目标文件路径
        String targetFilePath = targetDirPath + newFilename;
        Path targetPath = Paths.get(targetFilePath);
        
        // 复制文件
        Files.copy(Paths.get(localImagePath), targetPath);
        
        // 生成可访问的URL（这里假设服务器配置了静态资源映射）
        // 实际使用时，可能需要根据项目的静态资源配置进行调整
        String imageUrl = "/api/uploads/product_images/" + userId + "/" + newFilename;
        
        return imageUrl;
    }
    
    /**
     * 提供REST接口，将本地图片转化为URL
     * 接口路径: POST /api/convert-image
     * @param request 包含本地图片路径和用户ID的请求参数
     * @return 包含生成的图片URL的响应
     */
    @PostMapping("/convert-image")
    public Result<Map<String, String>> convertImageToUrl(@RequestBody Map<String, Object> request) {
        try {
            // 验证请求参数
            if (request == null) {
                return Result.error(400, "请求参数不能为空");
            }
            
            // 获取必要参数
            String localImagePath = request.get("localImagePath") == null ? null : request.get("localImagePath").toString();
            Integer userId;
            
            try {
                userId = request.get("userId") == null ? null : Integer.parseInt(request.get("userId").toString());
            } catch (NumberFormatException e) {
                return Result.error(400, "用户ID格式不正确");
            }
            
            // 调用核心方法转化图片
            String imageUrl = convertLocalImageToUrl(localImagePath, userId);
            
            // 构建响应数据
            Map<String, String> responseData = new HashMap<>();
            responseData.put("imageUrl", imageUrl);
            
            return Result.success(200, "图片转化成功", responseData);
        } catch (IOException e) {
            return Result.error(400, "图片转化失败：" + e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "服务器内部错误：" + e.getMessage());
        }
    }
    
    /**
     * 获取商品列表接口
     * 接口路径: GET /api/products
     * @param nums 请求的商品数量
     * @return 商品列表及相关信息
     */
    @GetMapping("/products")
    public Result<Map<String, Object>> getProducts(@RequestParam(required = false, defaultValue = "10") Integer nums) {
        try {
            // 验证参数
            if (nums == null || nums <= 0) {
                return Result.error(400, "参数错误：nums必须为正整数");
            }
            
            // 查询所有商品
            List<Product> products = productRepository.findAll();
            
            // 如果数据不足，返回所有可用数据
            int actualSize = Math.min(nums, products.size());
            List<Product> limitedProducts = new ArrayList<>();
            if (actualSize > 0) {
                limitedProducts = products.subList(0, actualSize);
            }
            
            // 构建响应数据
            List<Map<String, Object>> productList = new ArrayList<>();
            for (Product product : limitedProducts) {
                Map<String, Object> productMap = new HashMap<>();
                productMap.put("productId", product.getProductId());
                productMap.put("productName", product.getProductName());
                productMap.put("price", product.getPrice());
                productMap.put("producer", product.getProducer());
                productMap.put("salesVolume", product.getSalesVolume());
                productMap.put("productImg", product.getProductImg());
                productMap.put("surplus", product.getSurplus());
                productList.add(productMap);
            }
            
            // 构建响应数据
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("products", productList);
            
            return Result.success(200, "获取商品列表成功", responseData);
        } catch (Exception e) {
            return Result.error(500, "服务器内部错误：" + e.getMessage());
        }
    }
    
    /**
     * 获取商品详情接口
     * 接口路径: GET /api/products/{productId}
     * @param productId 商品ID
     * @return 商品详细信息
     */
    @GetMapping("/products/{productId}")
    public Result<Map<String, Object>> getProductDetail(@PathVariable Integer productId) {
        try {
            // 验证参数
            if (productId == null || productId <= 0) {
                return Result.error(400, "参数错误：商品ID无效");
            }
            
            // 根据商品ID查询商品
            Product product = productRepository.findByProductId(productId);
            
            if (product == null) {
                return Result.error(404, "商品不存在");
            }
            
            // 构建响应数据
            Map<String, Object> productMap = new HashMap<>();
            productMap.put("productId", product.getProductId());
            productMap.put("productName", product.getProductName());
            productMap.put("price", product.getPrice());
            productMap.put("producer", product.getProducer());
            productMap.put("salesVolume", product.getSalesVolume());
            productMap.put("productImg", product.getProductImg());
            productMap.put("surplus", product.getSurplus());
            
            return Result.success(200, "获取商品详情成功", productMap);
        } catch (Exception e) {
            return Result.error(500, "服务器内部错误：" + e.getMessage());
        }
    }
    
    /**
     * 添加商品接口
     * 接口路径: POST /api/products
     * @param request 商品信息
     * @return 添加结果
     */
    @PostMapping("/products")
    public Result<Map<String, Object>> addProduct(@RequestBody Map<String, Object> request) {
        try {
            // 参数校验
            if (request == null) {
                return Result.error(400, "请求参数不能为空");
            }
            
            // 验证必填参数
            if (!request.containsKey("productName") || request.get("productName") == null) {
                return Result.error(400, "参数错误：商品名称不能为空");
            }
            if (!request.containsKey("price") || request.get("price") == null) {
                return Result.error(400, "参数错误：商品价格不能为空");
            }
            if (!request.containsKey("userId") || request.get("userId") == null) {
                return Result.error(400, "参数错误：农户ID不能为空");
            }
            if (!request.containsKey("totalVolumn") || request.get("totalVolumn") == null) {
                return Result.error(400, "参数错误：总数量不能为空");
            }
            
            // 解析参数
            String productName;
            double price;
            String producer;
            String productImg;
            Integer salesVolume; 
            Integer userId; // 农户ID
            Integer totalVolumn; // 总销售量
            
            try {
                productName = request.get("productName").toString();
                price = Double.parseDouble(request.get("price").toString());
                producer = request.get("producer").toString();
                productImg = request.get("productImg").toString();
                salesVolume = Integer.parseInt(request.get("salesVolume").toString());
                userId = Integer.parseInt(request.get("userId").toString());
                totalVolumn = Integer.parseInt(request.get("totalVolumn").toString());
            } catch (NumberFormatException e) {
                return Result.error(400, "参数错误：数值类型格式不正确");
            }
            
            // 验证农户ID是否存在
            Optional<User> userOptional = userRepository.findByUserId(userId);
            if (!userOptional.isPresent()) {
                return Result.error(404, "农户不存在");
            }
            
            // 创建商品对象
            Product product = new Product();
            product.setProductName(productName);
            product.setPrice(price);
            product.setProducer(producer);
            product.setSalesVolume(0); // 设置销售量
            product.setSurplus(totalVolumn); // 初始库存为总销售量减去销售量
            // salesVolume默认为0，不需要手动设置
            product.setProductImg(productImg);
            product.setTotalVolumn(totalVolumn); // 设置总销售量
            product.setUserId(userId); // 设置农户ID
            
            // 保存到数据库
            Product savedProduct = productRepository.save(product);
            
            // 构建响应数据
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("productId", savedProduct.getProductId());
    
            return Result.success(200, "商品添加成功", responseData);
            
        } catch (Exception e) {
            return Result.error(500, "服务器内部错误：" + e.getMessage());
        }
    }
    
    /**
     * 获取农户商品列表接口
     * 接口路径: GET /api/products/farmer
     * @param userId 农户用户ID
     * @return 该农户发布的所有商品信息
     */
    @GetMapping("/products/farmer")
    public Result<Map<String, Object>> getFarmerProducts(@RequestParam Integer userId) {
        try {
            // 验证参数
            if (userId == null || userId <= 0) {
                return Result.error(400, "参数错误：用户ID无效");
            }
            
            // 查询该农户发布的所有商品
            List<Product> products = productRepository.findByUserId(userId);
            
            // 构建响应数据
            List<Map<String, Object>> productList = new ArrayList<>();
            for (Product product : products) {
                Map<String, Object> productMap = new HashMap<>();
                productMap.put("productId", product.getProductId());
                productMap.put("productName", product.getProductName());
                productMap.put("price", product.getPrice());
                productMap.put("producer", product.getProducer());
                productMap.put("salesVolume", product.getSalesVolume());
                productMap.put("productImg", product.getProductImg());
                productMap.put("surplus", product.getSurplus());
                productList.add(productMap);
            }
            
            // 构建响应数据
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("data", productList);
            
            return Result.success(200, "获取农户商品列表成功", responseData);
        } catch (Exception e) {
            return Result.error(500, "服务器内部错误：" + e.getMessage());
        }
    }

    /**
     * 购物车接口 - 将商品加入购物车
     * 接口路径: POST /api/shop
     * @param request 购物车信息
     * @return 添加结果
     */
    @PostMapping("/shop")
    public Result<Map<String, Object>> addToShoppingCart(@RequestBody Map<String, Object> request) {
        try {
            // 参数校验
            if (request == null) {
                return Result.error(400, "请求参数不能为空");
            }
            
            // 验证必填参数
            if (!request.containsKey("productId") || request.get("productId") == null) {
                return Result.error(400, "参数错误：商品ID不能为空");
            }
            if (!request.containsKey("userId") || request.get("userId") == null) {
                return Result.error(400, "参数错误：用户ID不能为空");
            }
            if (!request.containsKey("amount") || request.get("amount") == null) {
                return Result.error(400, "参数错误：购买数量不能为空");
            }
            if (!request.containsKey("getAddress") || request.get("getAddress") == null) {
                return Result.error(400, "参数错误：收货地址不能为空");
            }
            
            // 解析参数
            Integer productId;
            Integer userId;
            Integer amount;
            String getAddress;
            
            try {
                productId = Integer.parseInt(request.get("productId").toString());
                userId = Integer.parseInt(request.get("userId").toString());
                amount = Integer.parseInt(request.get("amount").toString());
                
                getAddress = request.get("getAddress").toString();
            } catch (NumberFormatException e) {
                return Result.error(400, "参数错误：数值类型格式不正确");
            }
            
            // 验证商品是否存在
            Product product = productRepository.findByProductId(productId);
            if (product == null) {
                return Result.error(404, "商品不存在");
            }
            
            // 验证用户是否存在
            Optional<User> userOptional = userRepository.findByUserId(userId);
            if (!userOptional.isPresent()) {
                return Result.error(404, "用户不存在");
            }
            
            // 检查库存是否足够
            if (product.getSurplus() < amount) {
                return Result.error(400, "商品库存不足，当前库存：" + product.getSurplus());
            }
            
            // 创建购物车对象
            ShoppingCart shoppingCart = new ShoppingCart();
            shoppingCart.setProductId(productId);
            shoppingCart.setUserId(userId);
            shoppingCart.setAmount(amount);
            shoppingCart.setTotalPrice( amount * product.getPrice());
            shoppingCart.setGetAddress(getAddress);
            
            // 保存到数据库
            shoppingCartRepository.save(shoppingCart);
            
            // 更新商品库存
            product.setSurplus(product.getSurplus() - amount);
            product.setSalesVolume(product.getSalesVolume() + amount);
            productRepository.save(product);
            
            // 构建响应数据
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("message", "添加到购物车成功");
            
            return Result.success(200, "添加成功", responseData);
            
        } catch (Exception e) {
            return Result.error(500, "服务器内部错误：" + e.getMessage());
        }
    }
    
    /**
     * 购买商品接口
     * 接口路径: POST /api/purchase
     * @param request 购买信息
     * @return 购买结果
     */
    @PostMapping("/purchase")
    public Result<Map<String, Object>> purchaseProduct(@RequestBody Map<String, Object> request) {
        try {
            // 参数校验
            if (request == null) {
                return Result.error(400, "请求参数不能为空");
            }
            
            // 验证必填参数
            if (!request.containsKey("productId") || request.get("productId") == null) {
                return Result.error(400, "参数错误：商品ID不能为空");
            }
            if (!request.containsKey("userId") || request.get("userId") == null) {
                return Result.error(400, "参数错误：用户ID不能为空");
            }
            if (!request.containsKey("amount") || request.get("amount") == null) {
                return Result.error(400, "参数错误：购买数量不能为空");
            }
            if (!request.containsKey("getAddress") || request.get("getAddress") == null) {
                return Result.error(400, "参数错误：收货地址不能为空");
            }
            
            // 解析参数
            Integer productId;
            Integer userId;
            Integer amount;
            String getAddress;
            
            try {
                productId = Integer.parseInt(request.get("productId").toString());
                userId = Integer.parseInt(request.get("userId").toString());
                amount = Integer.parseInt(request.get("amount").toString());
                getAddress = request.get("getAddress").toString();
            } catch (NumberFormatException e) {
                return Result.error(400, "参数错误：数值类型格式不正确");
            }
            
            // 验证商品是否存在
            Product product = productRepository.findByProductId(productId);
            if (product == null) {
                return Result.error(404, "商品不存在");
            }
            
            // 验证用户是否存在
            Optional<User> userOptional = userRepository.findByUserId(userId);
            if (!userOptional.isPresent()) {
                return Result.error(404, "用户不存在");
            }
            
            // 检查库存是否足够
            if (product.getSurplus() < amount) {
                return Result.error(400, "商品库存不足，当前库存：" + product.getSurplus());
            }
            
            // 创建购买记录
            Purchase purchase = new Purchase();
            purchase.setProductId(productId);
            purchase.setUserId(userId);
            purchase.setAmount(amount);
            purchase.setGetAddress(getAddress);
            // 计算总价，注意类型转换
            purchase.setTotalPrice((double) amount * product.getPrice());
            
            // 保存购买记录
            Purchase savedPurchase = purchaseRepository.save(purchase);
            
            // 更新商品库存
            product.setSurplus(product.getSurplus() - amount);
            product.setSalesVolume(product.getSalesVolume() + amount);
            productRepository.save(product);
            
            // 构建响应数据
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("purchaseId", savedPurchase.getPurchaseId());
            
            return Result.success(200, "购买成功", responseData);
            
        } catch (Exception e) {
            return Result.error(500, "服务器内部错误：" + e.getMessage());
        }
    }




 /**
     * 查看已售出的商品接口（农户端）
     * 接口路径: GET /api/soldout
     * @param userId 农户ID
     * @return 已售出商品列表
     */
    @GetMapping("/soldout")
    public Result<List<Map<String, Object>>> getSoldOutProducts(@RequestParam Integer userId) {
        try {
            // 验证参数
            if (userId == null || userId <= 0) {
                return Result.error(400, "参数错误：农户ID无效");
            }
            
            // 验证农户是否存在
            Optional<User> userOptional = userRepository.findByUserId(userId);
            if (!userOptional.isPresent()) {
                return Result.error(404, "农户不存在");
            }
            
            // 获取该农户的所有商品
            List<Product> farmerProducts = productRepository.findByUserId(userId);
            
            // 构建商品ID集合
            List<Integer> productIds = new ArrayList<>();
            for (Product product : farmerProducts) {
                productIds.add(product.getProductId());
            }
            
            // 构建响应数据列表
            List<Map<String, Object>> soldoutProducts = new ArrayList<>();
            
            // 如果该农户有商品，则查询这些商品的购买记录
            if (!productIds.isEmpty()) {
                // 查询所有购买记录
                List<Purchase> allPurchases = purchaseRepository.findAll();
                
                // 筛选出属于该农户商品的购买记录
                for (Purchase purchase : allPurchases) {
                    if (productIds.contains(purchase.getProductId())) {
                        // 查找对应的商品信息
                        Product product = productRepository.findByProductId(purchase.getProductId());
                        if (product != null) {
                            Map<String, Object> productMap = new HashMap<>();
                            productMap.put("productId", product.getProductId());
                            productMap.put("productName", product.getProductName());
                            productMap.put("productImg", product.getProductImg());
                            productMap.put("amout", purchase.getAmount());
                            productMap.put("money", product.getPrice());
                            productMap.put("totalPrice", purchase.getTotalPrice());
                            productMap.put("sendAddress", purchase.getGetAddress());
                            productMap.put("createTime", purchase.getCreateTime());
                            
                            soldoutProducts.add(productMap);
                        }
                    }
                }
            }
            
            // 直接返回商品列表数组，符合接口要求的格式
            return Result.success(200, "成功", soldoutProducts);
            
        } catch (Exception e) {
            return Result.error(500, "服务器内部错误：" + e.getMessage());
        }
    }


    /**
     * 展示购物车接口（买家端）
     * 接口路径: GET /api/purchaseshow
     * @param userId 用户ID
     * @return 购买记录信息列表
     */
    @GetMapping("/purchaseshow")
    public Result<List<Map<String, Object>>> showPurchase(@RequestParam Integer userId) {
        try {
            // 验证参数
            if (userId == null) {
                return Result.error(400, "用户ID不能为空");
            }
            
            // 查询用户的购买记录信息（只查询未结算的购买记录项）
            List<Purchase> purchaseItems = purchaseRepository.findByUserId(userId.intValue());
            
            // 构建响应数据
            List<Map<String, Object>> purchaseList = new ArrayList<>();
            for (Purchase purchaseItem : purchaseItems) {
                // 查询对应的商品信息
                Product product = productRepository.findByProductId(purchaseItem.getProductId());
                if (product != null) {
                    Map<String, Object> purchaseMap = new HashMap<>();
                    purchaseMap.put("productName", product.getProductName());
                    purchaseMap.put("producer", product.getProducer());
                    purchaseMap.put("productImg", product.getProductImg());
                    purchaseMap.put("amount", purchaseItem.getAmount());
                    purchaseMap.put("price", product.getPrice());
                    purchaseMap.put("totalPrice", product.getPrice() * purchaseItem.getAmount());
                    purchaseMap.put("sendAddress", purchaseItem.getGetAddress());
                    purchaseMap.put("createTime", purchaseItem.getCreateTime());
                    purchaseList.add(purchaseMap);
                }
            }
            
            return Result.success(200, "成功", purchaseList);
        } catch (Exception e) {
            return Result.error(500, "服务器内部错误：" + e.getMessage());
        }
    }

    /**
     * 展示购买记录接口（买家端）
     * 接口路径: GET /api/shopshow
     * @param userId 用户ID
     * @return 购物车信息列表
     */
    @GetMapping("/shopshow")
    public Result<List<Map<String, Object>>> showShoppingCart(@RequestParam Integer userId) {
        try {
            // 验证参数
            if (userId == null) {
                return Result.error(400, "用户ID不能为空");
            }
            
            // 查询用户的购物车信息（只查询未结算的购物车项）
            List<ShoppingCart> cartItems = shoppingCartRepository.findByUserIdAndStatus(userId.intValue(), 1);
            
            // 构建响应数据
            List<Map<String, Object>> cartList = new ArrayList<>();
            for (ShoppingCart cartItem : cartItems) {
                // 查询对应的商品信息
                Product product = productRepository.findByProductId(cartItem.getProductId());
                if (product != null) {
                    Map<String, Object> cartMap = new HashMap<>();
                    cartMap.put("productName", product.getProductName());
                    cartMap.put("producer", product.getProducer());
                    cartMap.put("productImg", product.getProductImg());
                    cartMap.put("amount", cartItem.getAmount());
                    cartMap.put("price", product.getPrice());
                    cartMap.put("totalPrice", product.getPrice() * cartItem.getAmount());
                    cartList.add(cartMap);
                }
            }
            
            return Result.success(200, "成功", cartList);
        } catch (Exception e) {
            return Result.error(500, "服务器内部错误：" + e.getMessage());
        }
    }

}