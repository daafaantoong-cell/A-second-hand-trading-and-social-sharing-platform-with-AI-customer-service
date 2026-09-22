package com.hmdp.tool;

import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * 商家相关工具
 * 供 ShopConsultAgent 调用，提供商家详情与按类型/位置查询能力
 */
@Slf4j
@Component
public class ShopTools {

    @Resource
    private IShopService shopService;

    /**
     * 根据商家 ID 查询商家详情（名称、地址、营业时间、评分、均价等）
     */
    @Tool("根据商家ID查询商家详情，返回名称、地址、营业时间、评分、均价、销量等信息")
    public String queryShopById(Long id) {
        try {
            Result result = shopService.queryById(id);
            if (!result.getSuccess()) {
                return "查询失败：" + result.getErrorMsg();
            }
            Shop shop = (Shop) result.getData();
            if (shop == null) {
                return "未找到该商家";
            }
            return String.format(
                    "商家信息：名称=%s, 类型ID=%d, 商圈=%s, 地址=%s, 营业时间=%s, 评分=%.1f, 均价=%d元, 销量=%d, 评论数=%d",
                    shop.getName(), shop.getTypeId(), shop.getArea(), shop.getAddress(),
                    shop.getOpenHours(), shop.getScore() / 10.0, shop.getAvgPrice(),
                    shop.getSold(), shop.getComments()
            );
        } catch (Exception e) {
            log.error("queryShopById error, id={}", id, e);
            return "查询商家信息时出错：" + e.getMessage();
        }
    }

    /**
     * 按商家类型查询商家列表，可选按经纬度距离排序
     *
     * @param typeId  商家类型 ID（1美食 2ktv 3... 详见 tb_shop_type）
     * @param current 页码，从 1 开始
     * @param x       经度（可选，用于按距离排序）
     * @param y       纬度（可选，用于按距离排序）
     */
    @Tool("按商家类型查询商家列表，支持按经纬度距离排序；typeId为商家类型ID，current为页码，x/y为经纬度（可选）")
    public String queryShopsByType(Integer typeId, Integer current, Double x, Double y) {
        try {
            Result result = shopService.queryShopByType(typeId, current, x, y);
            if (!result.getSuccess()) {
                return "查询失败：" + result.getErrorMsg();
            }
            Object data = result.getData();
            if (data == null) {
                return "未找到符合条件的商家";
            }
            return "商家列表：" + JSONUtil.toJsonStr(data);
        } catch (Exception e) {
            log.error("queryShopsByType error, typeId={}", typeId, e);
            return "查询商家列表时出错：" + e.getMessage();
        }
    }
}
