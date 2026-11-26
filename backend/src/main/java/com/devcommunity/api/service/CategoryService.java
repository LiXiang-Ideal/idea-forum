package com.devcommunity.api.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.devcommunity.api.dao.CategoryMapper;
import com.devcommunity.api.entity.CategoryEntity;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CategoryService {
    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    public List<CategoryEntity> list() {
        return categoryMapper.selectList(new QueryWrapper<CategoryEntity>()
                .eq("status", "ACTIVE")
                .orderByAsc("sort_order", "name"));
    }
}
