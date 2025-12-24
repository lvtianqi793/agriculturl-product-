
package com.ltqtest.springbootquickstart.controller;

import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.ltqtest.springbootquickstart.common.Result;
import com.ltqtest.springbootquickstart.entity.Product;
import com.ltqtest.springbootquickstart.entity.Purchase;
import com.ltqtest.springbootquickstart.entity.ShoppingCart;
import com.ltqtest.springbootquickstart.entity.User;
import com.ltqtest.springbootquickstart.entity.UserAddress;
import com.ltqtest.springbootquickstart.repository.ProductRepository;
import com.ltqtest.springbootquickstart.repository.PurchaseRepository;
import com.ltqtest.springbootquickstart.repository.ShoppingCartRepository;
import com.ltqtest.springbootquickstart.repository.UserRepository;
import com.ltqtest.springbootquickstart.util.PayUtil;

import cn.hutool.json.JSONObject;
import cn.hutool.log.Log;
import jakarta.servlet.http.HttpServletRequest;

import com.ltqtest.springbootquickstart.repository.UserAddressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.method.P;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Logger;



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
    private UserAddressRepository userAddressRepository;

    @Autowired
    private PayUtil payUtil;
    
    // 日志记录器
    private static final Logger logger = Logger.getLogger(AgricultureController.class.getName());
    
    // 基础上传路径
    @Value("${file.upload.base-path}")
    private String uploadBasePath;
    // 图片上传子路径
    @Value("${file.upload.product-img-path}")
    private String productImgPath;
    // 访问基础URL
    @Value("${file.upload.access-base-url}")
    private String accessBaseUrl;
    // 商品图片接口URL前缀
    private final String productImageApiUrl = "/api/products/image/";
    
    /**
     * 验证图片类型是否有效
     * @param contentType 文件的Content-Type
     * @return 是否为有效的图片类型
     */
    private boolean isValidImageType(String contentType) {
        return contentType != null && (
            contentType.equals("image/jpeg") ||
            contentType.equals("image/png") ||
            contentType.equals("image/gif")
        );
    }
    
    /**
     * 根据Content-Type获取文件扩展名
     * @param contentType 文件的Content-Type
     * @return 文件扩展名
     */
    private String getExtensionByContentType(String contentType) {
        if (contentType == null) return "jpg";
        switch (contentType) {
            case "image/jpeg": return "jpg";
            case "image/png": return "png";
            case "image/gif": return "gif";
            default: return "jpg";
        }
    }
    
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
     * 商品图片上传接口
     * 接口路径: POST /api/products/upload-image
     * @param file 上传的图片文件
     * @param userId 用户ID
     * @return 包含生成的图片URL的响应
     */
    @PostMapping("/products/upload-image")
    public Result<Map<String, String>> uploadProductImage(@RequestParam("file") MultipartFile file, @RequestParam("userId") Integer userId) {
        try {
            // 参数验证
            if (file == null || file.isEmpty()) {
                return Result.error(400, "图片文件不能为空");
            }
            if (userId == null || userId <= 0) {
                return Result.error(400, "用户ID无效");
            }
            
            // 验证用户是否存在
            Optional<User> userOptional = userRepository.findByUserId(userId);
            if (!userOptional.isPresent()) {
                return Result.error(404, "用户不存在");
            }
            
            // 验证图片类型
            String contentType = file.getContentType();
            if (!isValidImageType(contentType)) {
                return Result.error(400, "无效的图片类型，只支持jpg、png、gif格式");
            }
            
            // 创建基于用户ID的目录
            String userDirPath = uploadBasePath + productImgPath + userId + File.separator;
            File userDir = new File(userDirPath);
            if (!userDir.exists()) {
                if (!userDir.mkdirs()) {
                    return Result.error(500, "无法创建上传目录");
                }
            }
            
            // 生成唯一文件名
            String extension = getExtensionByContentType(contentType);
            String filename = "product_" + userId + "_" + UUID.randomUUID() + "." + extension;
            String filePath = userDirPath + filename;
            
            // 保存文件
            file.transferTo(new File(filePath));
            
            // 生成可访问的URL
            String imageUrl = accessBaseUrl + userId + "/" + filename;
            
            // 返回结果
            Map<String, String> responseData = new HashMap<>();
            responseData.put("imageUrl", imageUrl);
            responseData.put("filename", filename);
            
            return Result.success(200, "图片上传成功", responseData);
        } catch (IOException e) {
            return Result.error(500, "文件保存失败：" + e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "上传图片失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取商品图片接口
     * 接口路径: GET /api/products/image/{userId}/{filename}
     * @param userId 用户ID
     * @param filename 文件名
     * @return 图片数据
     */
    @GetMapping("/products/image/{userId}/{filename}")
    public ResponseEntity<byte[]> getProductImage(@PathVariable("userId") Integer userId, @PathVariable("filename") String filename) {
        try {
            // 参数验证
            if (userId == null || userId <= 0) {
                return ResponseEntity.badRequest().body(null);
            }
            if (filename == null || filename.isEmpty()) {
                return ResponseEntity.badRequest().body(null);
            }
            
            // 构造文件路径
            String filePath = uploadBasePath + productImgPath + userId + File.separator + filename;
            File imageFile = new File(filePath);
            
            // 验证文件是否存在
            if (!imageFile.exists() || !imageFile.isFile()) {
                return ResponseEntity.notFound().build();
            }
            
            // 读取文件内容
            byte[] imageData = Files.readAllBytes(imageFile.toPath());
            
            // 根据文件扩展名确定Content-Type
            String extension = StringUtils.getFilenameExtension(filename).toLowerCase();
            MediaType mediaType;
            switch (extension) {
                case "jpg":
                case "jpeg":
                    mediaType = MediaType.IMAGE_JPEG;
                    break;
                case "png":
                    mediaType = MediaType.IMAGE_PNG;
                    break;
                case "gif":
                    mediaType = MediaType.IMAGE_GIF;
                    break;
                default:
                    mediaType = MediaType.APPLICATION_OCTET_STREAM;
            }
            
            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(mediaType);
            headers.setContentLength(imageData.length);
            
            return new ResponseEntity<>(imageData, headers, HttpStatus.OK);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
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
     * 获取商品列表接口(买家端)
     * 接口路径: GET /api/products/buyer
     * @param nums 请求的商品数量
     * @return 商品列表及相关信息
     */
    @GetMapping("/products/buyer")
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
                if (product.getStatus() == 1) {
                    Map<String, Object> productMap = new HashMap<>();
                    productMap.put("productId", product.getProductId());
                    productMap.put("productName", product.getProductName());
                    productMap.put("price", product.getPrice());
                    productMap.put("producer", product.getProducer());
                    productMap.put("salesVolume", product.getSalesVolume());
                    
                    // 处理商品图片URL，确保格式正确
                    String productImg = product.getProductImg();
                    productMap.put("productImg", productImg);
                    
                    // 如果有图片，添加直接访问接口的URL（用于直接获取图片数据）
                    if (productImg != null && !productImg.isEmpty()) {
                        // 尝试从现有URL中提取用户ID和文件名，构建直接访问接口的URL
                        try {
                            // 解析现有URL，提取用户ID和文件名
                            if (productImg.startsWith(accessBaseUrl)) {
                                String relativePath = productImg.substring(accessBaseUrl.length());
                                if (relativePath.contains("/")) {
                                    String[] parts = relativePath.split("/");
                                    if (parts.length >= 2) {
                                        String userId = parts[0];
                                        String filename = parts[1];
                                        String directImageUrl = productImageApiUrl + userId + "/" + filename;
                                        productMap.put("directImageUrl", directImageUrl);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // 如果解析失败，忽略异常，不添加directImageUrl
                        }
                    }
                    
                    productMap.put("surplus", product.getSurplus());
                    productList.add(productMap);
                    }
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
     * 获取商品详情接口(买家端)
     * 接口路径: GET /api/products/buyer/{productId}
     * @param productId 商品ID
     * @return 商品详细信息
     */
    @GetMapping("/products/buyer/{productId}")
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
            
            // 验证商品是否已上架
            if (product.getStatus() != 1) {
                return Result.error(400, "商品已下架，无法查看详情");
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
     * 添加商品接口（支持图片上传）
     * 接口路径: POST /api/products/farmer/newProduct
     * @param productName 商品名称
     * @param price 商品价格
     * @param producer 生产者
     * @param totalVolumn 总数量
     * @param userId 农户ID
     * @param file 可选的商品图片
     * @return 添加结果
     */
    @PostMapping("/products/farmer/newProduct")
    public Result<Map<String, Object>> addProduct(
            @RequestParam("productName") String productName,
            @RequestParam("price") double price,
            @RequestParam(value = "producer", required = false, defaultValue = "") String producer,
            @RequestParam("totalVolumn") int totalVolumn,
            @RequestParam("userId") int userId,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        try {
            // 参数校验
            if (productName == null || productName.trim().isEmpty()) {
                return Result.error(400, "参数错误：商品名称不能为空");
            }
            if (price <= 0) {
                return Result.error(400, "参数错误：商品价格必须大于0");
            }
            if (userId <= 0) {
                return Result.error(400, "参数错误：农户ID无效");
            }
            if (totalVolumn <= 0) {
                return Result.error(400, "参数错误：总数量必须大于0");
            }
            
            String productImg = "";
            // 处理图片上传（如果有）
            if (file != null && !file.isEmpty()) {
                // 验证图片类型
                String contentType = file.getContentType();
                System.out.println(contentType);
                if (!isValidImageType(contentType)) {
                    return Result.error(400, "无效的图片类型，只支持jpg、png、gif格式");
                }
                String originalFilename = file.getOriginalFilename();
                String extension=StringUtils.getFilenameExtension(originalFilename);

                 // 确保扩展名不为空
                if (extension == null || extension.isEmpty()) {
                    // 根据MIME类型推断扩展名
                    extension = getExtensionByContentType(contentType);
                    logger.info("用户ID为" + userId + "的文件无扩展名，根据MIME类型推断为: " + extension);
                }
                String newFilename="product_"+userId+"_"+UUID.randomUUID()+"."+extension;
                String uploadRootDir=uploadBasePath + productImgPath;
                String userDir=uploadRootDir+userId+"/";
                System.out.println(newFilename);
                System.out.println(uploadRootDir);
                System.out.println(userDir);
                // 创建基于用户ID的目录
                java.io.File dir=new java.io.File(userDir);
                // 安全地创建目录，避免路径遍历攻击
                if (!dir.getCanonicalPath().startsWith(new java.io.File(uploadRootDir).getCanonicalPath())) {
                    logger.severe("用户ID为" + userId + "尝试使用非法文件路径: " + userDir);
                    return Result.error(400, "无效的文件路径");
                }
                if (!dir.exists()) {
                    boolean created = dir.mkdirs();
                    if (!created) {
                        logger.severe("无法创建用户ID为" + userId + "的农产品存储目录: " + userDir);
                        return Result.error(500, "创建文件存储目录失败");
                    }
                    logger.info("为用户ID" + userId + "创建了农产品存储目录: " + userDir);
                }
                String filePath = userDir + newFilename;
                java.io.File dest=new java.io.File(filePath);
                // 保存文件
                try {
                    file.transferTo(dest.getAbsoluteFile());
                    logger.info("用户ID为" + userId + "的农产品图片文件保存成功: " + dest.getAbsolutePath());
                } catch (IOException e) {
                    logger.severe("用户ID为" + userId + "的农产品图片文件保存失败: " + dest.getAbsolutePath());
                    return Result.error(500, "文件保存失败：" + e.getMessage());
                }
                
                // 生成可访问的URL
                productImg = accessBaseUrl + productImgPath + userId + "/" + newFilename;
                System.out.println(productImg);
                
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
            product.setProductImg(productImg);
            product.setTotalVolumn(totalVolumn); // 设置总数量
            product.setUserId(userId); // 设置农户ID
            
            // 保存到数据库
            Product savedProduct = productRepository.save(product);
            
            // 构建响应数据
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("productId", savedProduct.getProductId());
            responseData.put("productImg", productImg);
    
            return Result.success(200, "商品添加成功", responseData);
            
        } catch (Exception e) {
            return Result.error(500, "文件保存失败：" + e.getMessage());
        } 
    }
    
    /**
     * 获取该农户商品列表接口（农户端）
     * 接口路径: GET /api/products/farmer/getMyProducts
     * @param userId 农户用户ID
     * @return 该农户发布的所有商品信息数组
     */
    @GetMapping("/products/farmer/getMyProducts")
    public Result<List<Map<String, Object>>> getFarmerProducts(@RequestParam Integer userId) {
        try {
            // 验证参数
            if (userId == null || userId <= 0) {
                return Result.error(400, "参数错误：用户ID无效");
            }
            
            // 检查用户角色类型是否为1（农户）
            Optional<User> userOptional = userRepository.findByUserId(userId);
            if (!userOptional.isPresent()) {
                return Result.error(404, "用户不存在");
            }
            
            User user = userOptional.get();
            // 假设User实体类中有getRoleType()方法获取用户角色类型
            if (user.getRoleType() != 1) {
                return Result.error(403, "您不是农户，不可看农户发布的商品");
            }
            
            // 查询该农户发布的所有商品
            List<Product> products = productRepository.findByUserId(userId);
            
            // 构建响应数据
            List<Map<String, Object>> productList = new ArrayList<>();
            for (Product product : products) {
                // 验证商品是否已上架
                if (product.getStatus() == 1) {
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
            }
            
            return Result.success(200, "获取农户商品列表成功", productList);
        } catch (Exception e) {
            return Result.error(500, "服务器内部错误：" + e.getMessage());
        }
    }

    /**
     * 购物车接口 - 将商品加入购物车
     * 接口路径: POST /api/products/buyer/shop
     * @param request 购物车信息
     * @return 添加结果
     */
    @PostMapping("products/buyer/shop")
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
            
            // 解析参数
            Integer productId;
            Integer userId;
            Integer amount;
            try {
                productId = Integer.parseInt(request.get("productId").toString());
                userId = Integer.parseInt(request.get("userId").toString());
                amount = Integer.parseInt(request.get("amount").toString());
            } catch (NumberFormatException e) {
                return Result.error(400, "参数错误：数值类型格式不正确");
            }
            
            // 验证商品是否存在
            Product product = productRepository.findByProductId(productId);
            if (product == null) {
                return Result.error(404, "商品不存在");
            }
            
            // 验证商品是否已上架
            if (product.getStatus() != 1) {
                return Result.error(400, "商品已下架，无法购买");
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
            // 保存到数据库
            shoppingCartRepository.save(shoppingCart);
        
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
     * 直接购买商品接口
     * 接口路径: POST /api/products/buyer/purchase
     * @param request 购买信息
     * @return 购买结果
     */
    @PostMapping("products/buyer/purchase")
    public Result<Map<String, Object>> purchaseProduct(@RequestBody Map<String, Object> request) throws AlipayApiException {
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
            
            // 验证商品是否已上架
            if (product.getStatus() != 1) {
                return Result.error(400, "商品已下架，无法购买");
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
            purchase.setStatus(2);
            // 计算总价，注意类型转换
            purchase.setTotalPrice((double) amount * product.getPrice());
            
            // 保存购买记录
            Purchase savedPurchase = purchaseRepository.save(purchase);
            
            // 构建响应数据
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("purchaseId", savedPurchase.getPurchaseId());
            
            responseData.put("alipay", payUtil.sendRequestToAlipay(purchase.getTotalPrice(), savedPurchase.getPurchaseId(), "购买商品支付"));

            return Result.success(200, "请支付", responseData);
            
        } catch (Exception e) {
            return Result.error(500, "服务器内部错误：" + e.getMessage());
        }
    }
    
    /**
     * 支付成功回调接口
     * @param request 支付宝异步回调请求
     * @return 支付结果
     */
    @PostMapping("products/buyer/purchaseReturn")
    public Result<Map<String, Object>> purchaseReturn(HttpServletRequest request) throws ParseException {
        try {
            Log.get().info("=>支付宝异步回调");
            Map<String, String> params = new HashMap<>();
            Map<String, String[]> requestParams = request.getParameterMap();
            for (String name : requestParams.keySet()) {
                params.put(name, request.getParameter(name));
                // System.out.println(name + " = " + request.getParameter(name));
            }
            String tradeStatus = params.get("trade_status");
            if (tradeStatus.equals("TRADE_SUCCESS")) {
                //支付成功，更新订单状态
                String tradeNo = params.get("out_trade_no");
                Purchase purchase = purchaseRepository.findByPurchaseId(Integer.parseInt(tradeNo));
                if (purchase != null) {
                    //更新购买记录状态为已支付
                    purchase.setStatus(3);
                    purchaseRepository.save(purchase);
                    //更新商品库存和销量
                    Product product = productRepository.findByProductId(purchase.getProductId());
                    if (product != null) {
                        product.setSurplus(product.getSurplus() - purchase.getAmount());
                        product.setSalesVolume(product.getSalesVolume() + purchase.getAmount());
                        productRepository.save(product);
                    }
                }
                return Result.success(200, "支付成功");
            } else {
                return Result.error(400, "支付失败");
            }
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
    @GetMapping("/products/farmer/soldout")
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
     * 展示购买记录（买家端）
     * 接口路径: GET /api/products/buyer/showPurchase
     * @param userId 用户ID
     * @return 购买记录信息列表
     */
    @GetMapping("/products/buyer/showPurchase")
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
                    purchaseMap.put("purchaseId", purchaseItem.getPurchaseId());
                    purchaseMap.put("productName", product.getProductName());
                    purchaseMap.put("producer", product.getProducer());
                    purchaseMap.put("productImg", product.getProductImg());
                    purchaseMap.put("amount", purchaseItem.getAmount());
                    purchaseMap.put("price", product.getPrice());
                    purchaseMap.put("totalPrice", product.getPrice() * purchaseItem.getAmount());
                    purchaseMap.put("sendAddress", purchaseItem.getGetAddress());
                    purchaseMap.put("createTime", purchaseItem.getCreateTime());
                    purchaseMap.put("status", purchaseItem.getStatus());
                    purchaseList.add(purchaseMap);
                }
            }
            
            return Result.success(200, "成功", purchaseList);
        } catch (Exception e) {
            return Result.error(500, "服务器内部错误：" + e.getMessage());
        }
    }

    /**
     * 展示购物车接口（买家端）
     * 接口路径: GET /api/products/buyer/showshop
     * @param userId 用户ID
     * @return 购物车信息列表
     */
    @GetMapping("/products/buyer/showshop")
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
                    cartMap.put("cartId", cartItem.getCartId());
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
    
    /**
     * 从购物车中购买商品接口
     * 接口路径: POST /api/products/buyer/buyshop
     * @param request 购买信息
     * @return 购买结果
     */
    @PostMapping("/products/buyer/buyshop")
    public Result<?> buyFromShoppingCart(@RequestBody Map<String, Object> request) {
        try {
            // 参数校验
            if (request == null) {
                return Result.error(400, "请求参数不能为空");
            }
            
            // 验证必填参数
            if(!request.containsKey("cartId") || request.get("cartId") == null) {
                return Result.error(400, "参数错误：购物车ID不能为空");
            }
            
            // 解析参数
            Integer cartId;
            
            try {
                cartId = Integer.parseInt(request.get("cartId").toString());
            } catch (NumberFormatException e) {
                return Result.error(400, "参数错误：数值类型格式不正确");
            }
            
            // 验证购物车是否存在
            Optional<ShoppingCart> cartOptional = shoppingCartRepository.findByCartId(cartId);
            if (!cartOptional.isPresent()) {
                return Result.error(404, "购物车不存在");
            }
            
            // 获取购物车中的商品ID
            Integer productId = cartOptional.get().getProductId();
            
            // 验证商品是否存在
            Product product = productRepository.findByProductId(productId);
            if (product == null) {
                return Result.error(404, "商品不存在");
            }
            
            // 验证商品是否已上架
            if (product.getStatus() != 1) {
                return Result.error(400, "商品已下架，无法购买");
            }
            Integer userId = cartOptional.get().getUserId();
            
            // 验证用户是否存在
            Optional<User> userOptional = userRepository.findByUserId(userId);
            if (!userOptional.isPresent()) {
                return Result.error(404, "用户不存在");
            }
            
            Optional<ShoppingCart> cartItemOptional = shoppingCartRepository.findByCartId(cartId);
            // 获取购物车中的商品
            ShoppingCart cartItem = cartItemOptional.get();
            Integer amount = cartItem.getAmount();
            
            // 检查库存是否足够
            if (product.getSurplus() < amount) {
                return Result.error(400, "商品库存不足，当前库存：" + product.getSurplus());
            }
            
            // 创建购买记录
            Purchase purchase = new Purchase();
            purchase.setProductId(productId);
            purchase.setUserId(userId);
            purchase.setAmount(amount);
            purchase.setGetAddress(cartItem.getGetAddress());
            // 计算总价
            purchase.setTotalPrice((double) amount * product.getPrice());
            purchase.setStatus(3);

            // 保存购买记录     ````
            purchaseRepository.save(purchase);
            
            // 更新商品库存
            product.setSurplus(product.getSurplus() - amount);
            product.setSalesVolume(product.getSalesVolume() + amount);
            productRepository.save(product);
            
            // 从购物车中删除该商品
            shoppingCartRepository.delete(cartItem);
            
            return Result.success(200, "购买成功");
            
        } catch (Exception e) {
            return Result.error(500, "服务器内部错误：" + e.getMessage());
        }
    }
    
    /**
     * 删除购物车内的某件商品接口（买家端）
     * 接口路径: DELETE /api/products/buyer/shop/delete
     * @param request 其中包含cartId
     * @return 删除结果
     */
    @DeleteMapping("/products/buyer/shop/delete")
    public Result<?> deleteShoppingCartItem(@RequestBody Map<String, Object> request) {
        try {
            // 参数校验
            if (request == null) {
                return Result.error(400, "请求参数不能为空");
            }
            
            // 验证必填参数
            if(!request.containsKey("cartId") || request.get("cartId") == null) {
                return Result.error(400, "参数错误：购物车ID不能为空");
            }
            
            // 解析参数
            Integer cartId;
            
            try {
                cartId = Integer.parseInt(request.get("cartId").toString());
            } catch (NumberFormatException e) {
                return Result.error(400, "参数错误：数值类型格式不正确");
            }
    
            // 查询购物车中是否有该商品
            Optional<ShoppingCart> cartItemOptional = shoppingCartRepository.findByCartId(cartId);
            
            if (!cartItemOptional.isPresent()) {
                return Result.error(404, "购物车中不存在该条记录");
            }
            
            // 删除购物车中的商品
            ShoppingCart cartItem = cartItemOptional.get();
            
            shoppingCartRepository.delete(cartItem);
            
            return Result.success(200, "删除成功");
            
        } catch (Exception e) {
            return Result.error(500, "服务器内部错误：" + e.getMessage());
        }
    }
    
    /**
     * 查看自己对应的所有买家已付款，待发货的商品接口（农户端）
     * 接口路径: GET /api/products/farmer/showAllPurchase
     * @param userId 农户ID
     * @return 已付款待发货商品列表
     */
    @GetMapping("/products/farmer/showAllPurchase")
    public Result<List<Map<String, Object>>> showAllPurchase(@RequestParam Integer userId) {
        try {
            // 参数验证
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
                // 验证商品是否已上架
                if (product.getStatus() != 1) {
                    continue;
                }
                productIds.add(product.getProductId());
            }
            
            // 构建响应数据列表
            List<Map<String, Object>> purchaseList = new ArrayList<>();
            
            // 如果该农户有商品，则查询这些商品的购买记录
            if (!productIds.isEmpty()) {
                // 查询所有购买记录
                List<Purchase> allPurchases = purchaseRepository.findAll();
                
                // 筛选出属于该农户商品且状态为3（已付款待发货）的购买记录
                for (Purchase purchase : allPurchases) {
                    if (productIds.contains(purchase.getProductId()) && purchase.getStatus() == 3) {
                        Map<String, Object> purchaseMap = new HashMap<>();
                        purchaseMap.put("purchaseId", purchase.getPurchaseId());
                        purchaseMap.put("productId", purchase.getProductId());
                        purchaseMap.put("amount", purchase.getAmount());
                        purchaseMap.put("totalPrice", purchase.getTotalPrice());
                        purchaseMap.put("getAddress", purchase.getGetAddress());
                        purchaseMap.put("createTime", purchase.getCreateTime());
                        
                        purchaseList.add(purchaseMap);
                    }
                }
            }
            
            return Result.success(200, "成功", purchaseList);
            
        } catch (Exception e) {
            return Result.error(500, "服务器内部错误：" + e.getMessage());
        }
    }
    
    /**
     * 农民发货接口
         * 接口路径: POST /api/products/farmer/sendProduct
     * @return 操作结果
     */
    @PostMapping("/products/farmer/sendProduct")
    public Result<Map<String, Object>> sendProduct(@RequestBody Map<String, Object> request) {
        try {
            // 参数验证
            if (request == null || !request.containsKey("purchase_id") || request.get("purchase_id") == null) {
                return Result.error(400, "参数错误：购买记录ID不能为空");
            }
            
            Integer purchase_id;
            try {
                // 尝试转换purchase_id为整数
                if (request.get("purchase_id") instanceof Number) {
                    purchase_id = ((Number) request.get("purchase_id")).intValue();
                } else {
                    purchase_id = Integer.parseInt(request.get("purchase_id").toString());
                }
            } catch (NumberFormatException e) {
                return Result.error(400, "参数错误：购买记录ID格式不正确");
            }
            
            System.out.println("purchase_id:" + purchase_id);
            // 参数有效性验证
            if (purchase_id <= 0) {
                return Result.error(400, "参数错误：购买记录ID无效");
            }
            
            // 查询购买记录是否存在
            Optional<Purchase> purchaseOptional = purchaseRepository.findById(purchase_id);
            if (!purchaseOptional.isPresent()) {
                return Result.error(404, "购买记录不存在");
            }
            
            // 获取购买记录
            Purchase purchase = purchaseOptional.get();
            
            // 验证购买记录状态是否为3（已付款待发货）
            if (purchase.getStatus() != 3) {
                return Result.error(400, "只能对已付款待发货的商品进行发货操作");
            }
            
            // 更新购买记录状态为4（已发货待签收）
            purchase.setStatus(4);
            purchaseRepository.save(purchase);
            
            return Result.success(200, "已成功发货");
            
        } catch (Exception e) {
            return Result.error(500, "服务器内部错误：" + e.getMessage());
        }
    }
    
    /**
     * 农户取消订单接口
     * 接口路径: POST /api/products/farmer/cancelPurchase
     * @param request 其中包括purchase_id 购买记录ID
     * @return 操作结果
     */
    @PostMapping("/products/farmer/cancelPurchase")
    public Result<Map<String, Object>> farmerCancelPurchase(@RequestBody Map<String, Object> request) {
        try {
            // 参数验证
            if (request == null || !request.containsKey("purchase_id") || request.get("purchase_id") == null) {
                return Result.error(400, "参数错误：购买记录ID不能为空");
            }
            
            Integer purchase_id;
            try {
                // 尝试转换purchase_id为整数
                if (request.get("purchase_id") instanceof Number) {
                    purchase_id = ((Number) request.get("purchase_id")).intValue();
                } else {
                    purchase_id = Integer.parseInt(request.get("purchase_id").toString());
                }
            } catch (NumberFormatException e) {
                return Result.error(400, "参数错误：购买记录ID格式不正确");
            }
            
            // 参数有效性验证
            if (purchase_id <= 0) {
                return Result.error(400, "参数错误：购买记录ID无效");
            }
            
            // 查询购买记录是否存在
            Optional<Purchase> purchaseOptional = purchaseRepository.findById(purchase_id);
            if (!purchaseOptional.isPresent()) {
                return Result.error(404, "购买记录不存在");
            }
            
            // 获取购买记录
            Purchase purchase = purchaseOptional.get();
            
            // 验证购买记录状态是否为3（已付款待发货）
            if (purchase.getStatus() != 3) {
                return Result.error(400, "只能对已付款待发货的订单进行取消操作");
            }
            
            // 更新购买记录状态为6（取消状态）
            purchase.setStatus(6);
            purchaseRepository.save(purchase);
            
            return Result.success(200, "已成功取消");
            
        } catch (Exception e) {
            return Result.error(500, "服务器内部错误：" + e.getMessage());
        }
    }
    
    /**
     * 展示某个状态的所有商品接口（农户端）
     * 接口路径: GET /api/products/farmer/showOneStatusAllProduct
     * @param userId 农户ID
     * @param status 购买记录状态
     * @return 指定状态的商品列表
     */
    @GetMapping("/products/farmer/showOneStatusAllProduct")
    public Result<List<Map<String, Object>>> showOneStatusAllProduct(@RequestParam Integer userId, @RequestParam Integer status) {
        try {
            // 参数验证
            if (userId == null || userId <= 0) {
                return Result.error(400, "参数错误：农户ID无效");
            }
            if (status == null || status < 0) {
                return Result.error(400, "参数错误：状态值无效");
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
                // 验证商品是否已上架
                if (product.getStatus() != 1) {
                    continue;
                }
                productIds.add(product.getProductId());
            }
            
            // 构建响应数据列表
            List<Map<String, Object>> productList = new ArrayList<>();
            
            // 如果该农户有商品，则查询这些商品的购买记录
            if (!productIds.isEmpty()) {
                // 查询所有购买记录
                List<Purchase> allPurchases = purchaseRepository.findAll();
                
                // 筛选出属于该农户商品且状态符合要求的购买记录
                for (Purchase purchase : allPurchases) {
                    if (productIds.contains(purchase.getProductId()) && purchase.getStatus() == status) {
                        Map<String, Object> productMap = new HashMap<>();
                        productMap.put("product_id", purchase.getProductId());
                        productMap.put("amount", purchase.getAmount());
                        productMap.put("totalPrice", purchase.getTotalPrice());
                        productMap.put("getAddress", purchase.getGetAddress());
                        productMap.put("createTime", purchase.getCreateTime());
                        
                        productList.add(productMap);
                    }
                }
            }
            
            return Result.success(200, "已成功商品", productList);
            
        } catch (Exception e) {
            return Result.error(500, "服务器内部错误：" + e.getMessage());
        }
    }

    /**
     * 买家收货接口
     * 接口路径: POST /api/products/buyer/receiveProduct
     * @param request 包含purchase_id 购买记录ID
     * @return 操作结果
     */
    @PostMapping("/products/buyer/receiveProduct")
    public Result<Map<String, Object>> receiveProduct(@RequestBody Map<String, Object> request) {
        try {
            // 参数验证
            if (request == null || !request.containsKey("purchase_id") || request.get("purchase_id") == null) {
                return Result.error(400, "参数错误：购买记录ID不能为空");
            }
            
            Integer purchase_id;
            try {
                // 尝试转换purchase_id为整数
                if (request.get("purchase_id") instanceof Number) {
                    purchase_id = ((Number) request.get("purchase_id")).intValue();
                } else {
                    purchase_id = Integer.parseInt(request.get("purchase_id").toString());
                }
            } catch (NumberFormatException e) {
                return Result.error(400, "参数错误：购买记录ID格式不正确");
            }
            
            // 参数有效性验证
            if (purchase_id <= 0) {
                return Result.error(400, "参数错误：购买记录ID无效");
            }
            
            // 查询购买记录是否存在
            Optional<Purchase> purchaseOptional = purchaseRepository.findById(purchase_id);
            if (!purchaseOptional.isPresent()) {
                return Result.error(404, "购买记录不存在");
            }
            
            // 获取购买记录
            Purchase purchase = purchaseOptional.get();
            
            // 验证购买记录状态是否为4（已发货待收货）
            if (purchase.getStatus() != 4) {
                return Result.error(400, "只能对已发货待收货的商品进行收货操作");
            }
            
            // 更新购买记录状态为5（已收货）
            purchase.setStatus(5);
            purchaseRepository.save(purchase);
            
            return Result.success(200, "收货成功");
            
        } catch (Exception e) {
            return Result.error(500, "服务器内部错误：" + e.getMessage());
        }
    }
    /**
     * 买家取消订单接口
     * 接口路径: POST /api/products/buyer/cancelPurchase
     * @param request 包含purchase_id 购买记录ID
     * @return 操作结果
     */
    @PostMapping("/products/buyer/cancelPurchase")
    public Result<Map<String, Object>> buyerCancelPurchase(@RequestBody Map<String, Object> request) {
        try {
            // 参数验证
            if (request == null || !request.containsKey("purchase_id") || request.get("purchase_id") == null) {
                return Result.error(400, "参数错误：购买记录ID不能为空");
            }
            
            Integer purchase_id;
            try {
                // 尝试转换purchase_id为整数
                if (request.get("purchase_id") instanceof Number) {
                    purchase_id = ((Number) request.get("purchase_id")).intValue();
                } else {
                    purchase_id = Integer.parseInt(request.get("purchase_id").toString());
                }
            } catch (NumberFormatException e) {
                return Result.error(400, "参数错误：购买记录ID格式不正确");
            }
            
            // 参数有效性验证
            if (purchase_id <= 0) {
                return Result.error(400, "参数错误：购买记录ID无效");
            }
            // 查询购买记录是否存在
            Optional<Purchase> purchaseOptional = purchaseRepository.findById(purchase_id);
            if (!purchaseOptional.isPresent()) {
                return Result.error(404, "购买记录不存在");
            }
            
            // 获取购买记录
            Purchase purchase = purchaseOptional.get();
            
            // 验证购买记录状态是否为3（已付款待发货）
            if (purchase.getStatus() != 3) {
                return Result.error(400, "只能对已付款待发货的订单进行取消操作");
            }
            
            // 更新购买记录状态为6（取消状态）
            purchase.setStatus(6);
            purchaseRepository.save(purchase);
            Product product = productRepository.findByProductId(purchase.getProductId());
            // 恢复商品库存
            product.setSurplus(product.getSurplus() + purchase.getAmount());
            product.setSalesVolume(product.getSalesVolume() - purchase.getAmount());
            productRepository.save(product);
            
            return Result.success(200, "已成功取消");
            
        } catch (Exception e) {
            return Result.error(500, "服务器内部错误：" + e.getMessage());
        }
    }
    /**
     * 买家退货订单接口
     * 接口路径: POST /api/products/buyer/returnProduct
     * @param request 包含purchase_id 购买记录ID
     * @return 操作结果
     */
    @PostMapping("/products/buyer/returnProduct")
    public Result<Map<String, Object>> buyerReturnProduct(@RequestBody Map<String, Object> request) {
        try {
            // 参数验证
            if (request == null || !request.containsKey("purchase_id") || request.get("purchase_id") == null) {
                return Result.error(400, "参数错误：购买记录ID不能为空");
            }
            
            // 尝试转换purchase_id为整数
            Integer purchase_id;
            try {
                if (request.get("purchase_id") instanceof Number) {
                    purchase_id = ((Number) request.get("purchase_id")).intValue();
                } else {
                    purchase_id = Integer.parseInt(request.get("purchase_id").toString());
                }
            } catch (NumberFormatException e) {
                return Result.error(400, "参数错误：购买记录ID格式不正确");
            }
            
            // 参数有效性验证
            if (purchase_id <= 0) {
                return Result.error(400, "参数错误：购买记录ID无效");
            }
            
            // 查询购买记录是否存在
            Optional<Purchase> purchaseOptional = purchaseRepository.findById(purchase_id);
            if (!purchaseOptional.isPresent()) {
                return Result.error(404, "购买记录不存在");
            }
            
            // 获取购买记录
            Purchase purchase = purchaseOptional.get();
            
            // 验证购买记录状态是否为4（已发货待收货）或5（已收货）
            if (purchase.getStatus() != 4&&purchase.getStatus()!=5) {
                return Result.error(400, "只能对已发货待收货或已收货的订单进行退货操作");
            }
            
            
            // 更新购买记录状态为7（退货状态）
            purchase.setStatus(7);
            purchaseRepository.save(purchase);
            Product product = productRepository.findByProductId(purchase.getProductId());
            // 恢复商品库存
            product.setSurplus(product.getSurplus() + purchase.getAmount());
            product.setSalesVolume(product.getSalesVolume() - purchase.getAmount());
            productRepository.save(product);
            
            return Result.success(200, "已成功退货");
            
        } catch (Exception e) {
            return Result.error(500, "服务器内部错误：" + e.getMessage());
        }
    }

    /**
     * 下架已发布的农产品接口
     * 接口路径: DELETE /api/products/farmer/deleteProduct
     * @param request  包含productId 农产品ID
     * @return 操作结果
     */
    @DeleteMapping("/products/farmer/deleteProduct")
    public Result<Map<String, Object>> deleteProduct(@RequestBody Map<String, Object> request) {
        try {
            // 参数验证
            if (request == null || !request.containsKey("productId") || request.get("productId") == null) {
                return Result.error(400, "参数错误：农产品ID不能为空");
            }
            // 尝试转换productId为整数
            Integer productId;
            try {
                if (request.get("productId") instanceof Number) {
                    productId = ((Number) request.get("productId")).intValue();
                } else {
                    productId = Integer.parseInt(request.get("productId").toString());
                }
            } catch (NumberFormatException e) {
                return Result.error(400, "参数错误：农产品ID格式不正确");
            }
            
            // 参数有效性验证
            if (productId <= 0) {
                return Result.error(400, "参数错误：农产品ID无效");
            }
            
            // 查询农产品是否存在
            Optional<Product> productOptional = productRepository.findById(productId);
            if (!productOptional.isPresent()) {
                return Result.error(404, "农产品不存在");
            }
            
            Product product = productOptional.get();
            
            // 验证农产品状态是否为1（已发布）
            if (product.getStatus() != 1) {
                return Result.error(400, "只能下架已发布的农产品");
            }
            
            // 查询该农产品的所有购买记录
            List<Purchase> allPurchases = purchaseRepository.findAll();
            List<Purchase> productPurchases = new ArrayList<>();
            
            for (Purchase purchase : allPurchases) {
                if (purchase.getProductId().equals(productId)) {
                    productPurchases.add(purchase);
                }
            }
            
            // 取消所有该农产品的未完成订单（将状态设置为6）
            for (Purchase purchase : productPurchases) {
                // 只有未完成的订单才需要取消(买家已付款的订单)
                if (purchase.getStatus() == 3 ) {
                    purchase.setStatus(6);
                    purchaseRepository.save(purchase);
                }
            }
            
            // 将农产品状态由1改为2（下架）
            product.setStatus(2);
            productRepository.save(product);
            
            return Result.success(200, "下架成功");
            
        } catch (Exception e) {
            return Result.error(500, "服务器内部错误：" + e.getMessage());
        }
    }

    /**
     * 查看已保存的地址接口
     * 接口路径: GET /api/products/buyer/getSavedAddress
     * @param userId 用户ID
     * @return 用户保存的地址列表
     */
    
    @GetMapping("/products/buyer/getSavedAddress")
    public Result<?> getSavedAddress(@RequestParam Integer userId) {
        try {
            // 验证参数
            if (userId == null) {
                return Result.error(400, "参数错误：用户ID不能为空");
            }
            // 参数有效性验证
            if (userId <= 0) {
                return Result.error(400, "参数错误：用户ID无效");
            }
            
            // 验证用户是否存在
            if (!userRepository.existsById(userId)) {
                return Result.error(404, "用户不存在");
            }
            
            // 查询用户的所有地址
            List<UserAddress> addresses = userAddressRepository.findByUserId(userId);
            
            // 构建响应数据
            List<Map<String, Object>> addressList = new ArrayList<>();
            for (UserAddress address : addresses) {
                Map<String, Object> addressMap = new HashMap<>();
                addressMap.put("addressId", address.getAddressId());
                addressMap.put("address_name", address.getAddressName());
                addressList.add(addressMap);
            }
            
            return Result.success(200, "查看地址成功", addressList);
        } catch (Exception e) {
            return Result.error(500, "服务器内部错误：" + e.getMessage());
        }
    }

}