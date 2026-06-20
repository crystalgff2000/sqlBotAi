package com.sqlbot.controller;

import com.sqlbot.dto.ResponseResult;
import com.sqlbot.entity.DataAsset;
import com.sqlbot.service.DataAssetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/data-assets")
public class DataAssetController {

    @Autowired
    private DataAssetService dataAssetService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("assets", dataAssetService.findAll());
        model.addAttribute("title", "\u6570\u636e\u8d44\u4ea7\u7ba1\u7406");
        return "modules/data-assets";
    }

    @GetMapping("/api/all")
    @ResponseBody
    public ResponseResult<List<DataAsset>> getAll() {
        return ResponseResult.success(dataAssetService.findAll());
    }

    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseResult<DataAsset> getById(@PathVariable Long id) {
        DataAsset asset = dataAssetService.findById(id);
        if (asset == null) {
            return ResponseResult.error("\u6570\u636e\u8d44\u4ea7\u4e0d\u5b58\u5728");
        }
        return ResponseResult.success(asset);
    }

    @PostMapping("/api/save")
    @ResponseBody
    public ResponseResult<DataAsset> save(@RequestBody DataAsset asset) {
        return ResponseResult.success("\u4fdd\u5b58\u6210\u529f", dataAssetService.save(asset));
    }

    @DeleteMapping("/api/{id}")
    @ResponseBody
    public ResponseResult<String> delete(@PathVariable Long id) {
        dataAssetService.delete(id);
        return ResponseResult.success("\u5220\u9664\u6210\u529f", null);
    }
}
