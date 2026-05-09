package org.example.timecoinweb.controller;

import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.example.pojo.Result;
import org.example.pojo.User;
import org.example.timecoinweb.config.BlockchainProperties;
import org.example.timecoinweb.service.RegisterService;
import org.example.timecoinweb.service.TimeCoinChainService;
import org.example.timecoinweb.service.UserService;
import org.example.utils.AliOSSUtils;
import org.example.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

//用于显示当前用户自己的信息

@RestController
@Slf4j
@RequestMapping("/info")
public class SelfController {
    @Autowired
    private RegisterService registerService;
    @Autowired
    private AliOSSUtils aliOSSUtils;
    @Autowired
    private UserService userService;
    @Autowired
    private TimeCoinChainService timeCoinChainService;
    @Autowired
    private BlockchainProperties blockchainProperties;

    /**
     * 老人发布活动在链上的扣费数量（供移动端确认提示；与 {@code blockchain.old-publish-activity-cost} 一致）。
     */
    @GetMapping("/publishActivityFee")
    public Result publishActivityFee() {
        BigInteger cost = blockchainProperties.getOldPublishActivityCost();
        if (cost == null) {
            cost = BigInteger.ZERO;
        }
        boolean deduct =
                blockchainProperties.isEnabled()
                        && timeCoinChainService.isChainReady()
                        && cost.signum() > 0;
        BigInteger vmax = blockchainProperties.getVolunteerRewardMax();
        boolean rewardCapped = vmax != null && vmax.signum() > 0;
        Map<String, Object> body = new HashMap<>();
        body.put("cost", cost.toString());
        body.put("deductEnabled", deduct);
        body.put("feeRecipientUserId", blockchainProperties.getFeeRecipientUserId());
        body.put("volunteerRewardMax", rewardCapped ? vmax.toString() : null);
        body.put("volunteerRewardMaxCapped", rewardCapped);
        return Result.success(body);
    }

    /**
     * 当前登录用户在链上的时间币余额（userId = 用户表主键字符串）。
     * 路径含 info，{@link org.example.timecoinweb.interceptor.LoginCheckInterceptor} 对所有已登录角色放行。
     */
    @GetMapping("/coinBalance")
    public Result coinBalance(@RequestHeader("token") String token) {
        Claims claims = JwtUtils.parseJWT(token);
        Integer id = (Integer) claims.get("id");
        String userId = String.valueOf(id);
        Map<String, Object> body = new HashMap<>();
        body.put("userId", userId);
        if (!timeCoinChainService.isChainReady()) {
            body.put("balance", "0");
            body.put("chainReady", Boolean.FALSE);
            body.put("reason", timeCoinChainService.getNotReadyReason());
            return Result.success(body);
        }
        try {
            BigInteger bal = timeCoinChainService.balanceOf(userId);
            body.put("balance", bal.toString());
            body.put("chainReady", Boolean.TRUE);
            return Result.success(body);
        } catch (Exception e) {
            log.warn("coinBalance 查询失败 userId={}", userId, e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 查询用户个人信息
     * @param token
     * @return
     */
    @GetMapping
    public Result selectSelf(@RequestHeader("token")String token){

        //解析token
        Claims claims=JwtUtils.parseJWT(token);

        //找到id
        Integer id=(Integer) claims.get("id");;

        //利用service获取全部信息
        User user=registerService.selectSelf(id);

        return Result.success(user);
    }


    /**
     * 上传到阿里云OSS，并生成url地址
     * 前端用update更新
     * @param image
     * @return
     * @throws IOException
     */
//    //设置头像,上传到阿里云OSS
//    @PostMapping
//    public Result setPhoto(MultipartFile image) throws IOException {
//        log.info("文件上传，文件名:{}",image.getOriginalFilename());
//
//        //调用阿里云OSS工具类进行文件上传
//        String url=aliOSSUtils.upload(image);
//        log.info("访问的url为:{}",url);
//
//        return Result.success(url);
//    }


    @PostMapping
    public  Result setPhoto(@RequestParam("image") MultipartFile image,@RequestHeader("token") String token) throws IOException{
        //解析token
        Claims claims=JwtUtils.parseJWT(token);
        //找到id
        Integer id=(Integer) claims.get("id");;

        log.info("文件上传：userId为{}，图片为{}",id,image);

        //获取原始文件名
        String originalFilename=image.getName();
        int index=originalFilename.lastIndexOf(".");
        String extname=originalFilename.substring(index);//获取后缀名
        log.info("获取的新文件名：{}",id+extname);

        //将文件存储在服务器的磁盘目录中D:\java\image
        image.transferTo(new File("D:\\java\\image\\"+id+extname));
        String local=id+extname;

        //存图片名到数据库
        userService.updateImage(local,id);

        return Result.success(local);
    }

}
