package com.sqlbot.controller;

import com.sqlbot.dto.ResponseResult;
import com.sqlbot.entity.EntityItem;
import com.sqlbot.service.EntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/entities")
public class EntityController {
    
    @Autowired
    private EntityService entityService;
    
    @GetMapping
    public String list(Model model) {
        model.addAttribute("entities", entityService.findAll());
        model.addAttribute("title", "\u5b9e\u4f53\u7ba1\u7406");
        return "modules/entities";
    }
    
    @GetMapping("/api/all")
    @ResponseBody
    public ResponseResult<List<EntityItem>> getAll() {
        return ResponseResult.success(entityService.findAll());
    }
    
    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseResult<EntityItem> getById(@PathVariable Long id) {
        EntityItem entity = entityService.findById(id);
        if (entity == null) {
            return ResponseResult.error("\u5b9e\u4f53\u4e0d\u5b58\u5728");
        }
        return ResponseResult.success(entity);
    }
    
    @PostMapping("/api/save")
    @ResponseBody
    public ResponseResult<EntityItem> save(@RequestBody EntityItem entity) {
        return ResponseResult.success("\u4fdd\u5b58\u6210\u529f", entityService.save(entity));
    }
    
    @DeleteMapping("/api/{id}")
    @ResponseBody
    public ResponseResult<String> delete(@PathVariable Long id) {
        entityService.delete(id);
        return ResponseResult.success("\u5220\u9664\u6210\u529f", null);
    }
}
